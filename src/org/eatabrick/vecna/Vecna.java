/* Copyright (c) 2012 Alan Berndt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package org.eatabrick.vecna;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Security;
import java.util.Iterator;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRingCollection;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPOnePassSignatureList;
import org.spongycastle.openpgp.PGPCompressedData;


public class Vecna extends ListActivity {
  private final static String TAG = "Vecna";

  private PasswordEntryAdapter adapter;
  private SharedPreferences settings;
  private String passphrase = "";

  private class ReadEntriesTask extends AsyncTask<String, Integer, Integer> {
    ProgressDialog progress;

    protected void onPreExecute() {
      adapter.clear();
      adapter.notifyDataSetChanged();
      adapter.setNotifyOnChange(false);

      progress = new ProgressDialog(Vecna.this);
      progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      progress.setMessage(getString(R.string.progress_initial));
      progress.setCancelable(false);
      progress.setMax(6);

      progress.show();
    }

    protected Integer doInBackground(String... string) {
      PGPSecretKeyRingCollection keyring = null;
      FileInputStream dataStream = null;

      publishProgress(R.string.progress_keyfile);
      try {
        File keyFile = new File(settings.getString("key_file", ""));
        FileInputStream keyStream = new FileInputStream(keyFile);
        keyring = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(keyStream));
      } catch (Exception e) {
        return R.string.error_key_file_not_found;
      }

      publishProgress(R.string.progress_passfile);
      try {
        File dataFile = new File(settings.getString("passwords", ""));
        dataStream = new FileInputStream(dataFile);
      } catch (Exception e) {
        return R.string.error_password_file_not_found;
      }

      try {
        PGPObjectFactory factory;

        publishProgress(R.string.progress_find_data);

        factory = new PGPObjectFactory(PGPUtil.getDecoderStream(dataStream));
        PGPEncryptedDataList dataList = null;

        Object o;
        while ((o = factory.nextObject()) != null) {
          if (o instanceof PGPEncryptedDataList) {
            dataList = (PGPEncryptedDataList) o;
            break;
          }
        }

        if (dataList == null) return R.string.error_no_data_in_file;

        publishProgress(R.string.progress_find_key);

        PGPSecretKey keySecret = null;
        PGPPrivateKey keyPrivate = null;

        PGPPublicKeyEncryptedData dataEncrypted = null;
        Iterator it = dataList.getEncryptedDataObjects();
        while (keyPrivate == null && it.hasNext()) {
          dataEncrypted = (PGPPublicKeyEncryptedData) it.next();

          keySecret = keyring.getSecretKey(dataEncrypted.getKeyID());
          keyPrivate = keySecret.extractPrivateKey(string[0].toCharArray(), "BC");
        }

        if (keyPrivate == null) return R.string.error_secret_key_missing;

        publishProgress(R.string.progress_decrypt);

        factory = new PGPObjectFactory(dataEncrypted.getDataStream(keyPrivate, "BC"));
        Object message = factory.nextObject();

        if (message instanceof PGPCompressedData) {
          PGPObjectFactory f = new PGPObjectFactory(((PGPCompressedData) message).getDataStream());
          message = f.nextObject();
        }

        ByteArrayOutputStream dataFinal = new ByteArrayOutputStream();
        if (message instanceof PGPLiteralData) {
          InputStream s = ((PGPLiteralData) message).getInputStream();

          int ch;
          while ((ch = s.read()) >= 0) {
            dataFinal.write(ch);
          }
        } else {
          return R.string.error_encryption;
        }

        publishProgress(R.string.progress_parse);

        String[] lines = dataFinal.toString().split("\n");
        for (int i = 0; i < lines.length; ++i) {
          adapter.add(new Entry(lines[i]));
        }
      } catch (Exception e) {
        e.printStackTrace();
        return R.string.error_unknown;
      }

      return 0;
    }

    protected void onProgressUpdate(Integer... messages) {
      progress.setMessage(getString(messages[0]));
      progress.incrementProgressBy(1);
    }

    protected void onPostExecute(Integer result) {
      progress.dismiss();

      adapter.setNotifyOnChange(true);
      adapter.notifyDataSetChanged();

      if (result > 0) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Vecna.this);

        builder.setTitle(getString(R.string.error));
        builder.setMessage(getString(result));
        builder.setCancelable(false);
        builder.setPositiveButton(android.R.string.ok, null);

        builder.show();
      }

      findViewById(android.R.id.list).requestFocus();
    }
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

    adapter = new PasswordEntryAdapter(this);
    adapter.setNotifyOnChange(false);
    setListAdapter(adapter);

    Security.addProvider(new BouncyCastleProvider());

    if (savedInstanceState != null) {
      passphrase = savedInstanceState.getString("passphrase");
      adapter.populate(savedInstanceState.getStringArray("entries"));
      adapter.notifyDataSetChanged();
    }

    getListView().setLongClickable(true);
    getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
      public boolean onItemLongClick(AdapterView<?> parent, View v, int pos, long id) {
        onListItemLongClick(parent, v, pos, id);
        return true;
      }
    });
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.search:
        // just shows the soft keyboard and lets the list view deal with searching
        InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
        imm.showSoftInput(getListView(), 0);
        return true;
      case R.id.refresh:
        updateEntries();
        return true;
      case R.id.settings:
        Intent intent = new Intent(this, Preferences.class);
        startActivityForResult(intent, 0);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override protected void onResume() {
    super.onResume();

    if (adapter.getCount() == 0) updateEntries();
  }

  @Override protected void onListItemClick(ListView parent, View v, int pos, long id) {
    final Entry entry = (Entry) adapter.getItem(pos);

    ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setText(entry.password);
    Toast.makeText(Vecna.this, getString(R.string.copied, entry.account), Toast.LENGTH_SHORT).show();
  }

  protected void onListItemLongClick(AdapterView parent, View v, int pos, long id) {
    final Entry entry = (Entry) adapter.getItem(pos);

    AlertDialog.Builder builder = new AlertDialog.Builder(this);

    LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
    final View layout = inflater.inflate(R.layout.entry, (ViewGroup) findViewById(R.id.layout_root));

    ((TextView) layout.findViewById(R.id.account )).setText(entry.account);
    ((TextView) layout.findViewById(R.id.user    )).setText(entry.user);
    ((TextView) layout.findViewById(R.id.password)).setText(entry.password);

    builder.setView(layout);

    builder.setPositiveButton(R.string.show_entry_copy, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setText(entry.password);
        Toast.makeText(Vecna.this, getString(R.string.copied, entry.account), Toast.LENGTH_SHORT).show();
      }
    });

    builder.setNegativeButton(R.string.show_entry_close, null);
    builder.setNeutralButton(R.string.show_entry_show, null);

    final AlertDialog dialog = builder.create();

    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override public void onShow(DialogInterface dialogInterface) {
        final Button show = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        show.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View view) {
            EditText password = (EditText) layout.findViewById(R.id.password);

            if (show.getText().equals(getString(R.string.show_entry_show))) {
              Log.d(TAG, "Show clicked");
              password.setTransformationMethod(null);
              show.setText(R.string.show_entry_hide);
            } else {
              Log.d(TAG, "Hide clicked");
              password.setTransformationMethod(new PasswordTransformationMethod());
              show.setText(R.string.show_entry_show);
            }
          }
        });
      }
    });

    dialog.show();
  }

  @Override public void onSaveInstanceState(Bundle savedInstanceState) {
    super.onSaveInstanceState(savedInstanceState);
    savedInstanceState.putString("passphrase", passphrase);
    savedInstanceState.putStringArray("entries", adapter.toStringArray());
  }

  private void getPassphrase() {
    final EditText pass = new EditText(this);
    pass.setSingleLine();
    pass.setTransformationMethod(new PasswordTransformationMethod());

    AlertDialog.Builder builder = new AlertDialog.Builder(this);

    builder.setMessage(getString(R.string.prompt));
    builder.setView(pass);
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        Vecna.this.passphrase = pass.getText().toString();
        new ReadEntriesTask().execute(passphrase);
      }
    });

    builder.show();
  }

  private void updateEntries() {
    TextView empty = (TextView) findViewById(android.R.id.empty);

    if (settings.getString("passwords", "").isEmpty() || settings.getString("key_file", "").isEmpty()) {
      empty.setText(R.string.settings);
    } else {
      empty.setText(R.string.empty);
      if (passphrase.isEmpty()) {
        getPassphrase();
      } else {
        new ReadEntriesTask().execute(passphrase);
      }
    }
  }
}
