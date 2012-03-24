package org.eatabrick.vecna;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Vecna extends ListActivity {
  private final static String TAG = "Vecna";

  private final static int MESSAGE_DECRYPT = 0x47010001;

  private ArrayList<Entry> list = new ArrayList<Entry>();
  private BaseAdapter adapter;
  private SharedPreferences settings;

  private Handler handler = new Handler();
  private Runnable loadEntries = new Runnable() {
    public void run() {
      setEmptyText(R.string.loading);
      list.clear();
      adapter.notifyDataSetChanged();

      if (settings.getBoolean("encrypted", true)) {
        decryptFile();
      } else {
        readPasswordFile();
      }
      
      adapter.notifyDataSetChanged();
    }
  };


  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

    adapter = new BaseAdapter() {
      public int getCount() {
        return list.size();
      }

      public Object getItem(int pos) {
        return list.get(pos);
      }

      public long getItemId(int pos) {
        return pos;
      }

      public View getView(int pos, View view, ViewGroup viewGroup) {
        if (view == null) {
          view = View.inflate(Vecna.this, android.R.layout.two_line_list_item, null);
        }

        Entry entry = (Entry) getItem(pos);

        ((TextView) view.findViewById(android.R.id.text1)).setText(entry.account);
        ((TextView) view.findViewById(android.R.id.text2)).setText(entry.user);

        return view;
      }
    };

    setListAdapter(adapter);
    getListView().setTextFilterEnabled(true);
  }
    
  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent = null;
    switch (item.getItemId()) {
      case R.id.settings:
        intent = new Intent(this, Preferences.class);
        startActivityForResult(intent, 0);
        return true;
      case R.id.refresh:
        handler.post(loadEntries);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override protected void onResume() {
    super.onResume();
    handler.post(loadEntries);
  }

  @Override protected void onListItemClick(ListView parent, View v, int pos, long id) {
    final Entry entry = list.get(pos);

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

  private void setEmptyText(int resourceId) {
    ((TextView) findViewById(android.R.id.empty)).setText(resourceId);
  }

  private void readPasswordFile() {
    try {
      File f = new File(settings.getString("passwords", ""));
      FileInputStream is = new FileInputStream(f);
      BufferedReader buf = new BufferedReader(new InputStreamReader(is));

      String line = new String();
      while ((line = buf.readLine()) != null) {
        list.add(new Entry(line));
      }
    } catch (FileNotFoundException e) {
      list.clear();
      setEmptyText(R.string.error_file_not_found);
    } catch (Exception e) {
      list.clear();
      e.printStackTrace();
      setEmptyText(R.string.error_file_format);
    }
  }

  private void decryptFile() {
    String data = "";

    try {
      File f = new File(settings.getString("passwords", ""));
      FileInputStream is = new FileInputStream(f);
      BufferedReader buf = new BufferedReader(new InputStreamReader(is));

      String line = new String();
      while ((line = buf.readLine()) != null) {
        data += line + "\n";
      }

      Intent intent = new Intent("org.thialfihar.android.apg.intent.DECRYPT_AND_RETURN");
      intent.setType("text/plain");
      intent.putExtra("intentVersion", "1");
      intent.putExtra("text", data);

      Log.d(TAG, "Starting decryption with data " + data);
      startActivityForResult(intent, MESSAGE_DECRYPT);
    } catch (FileNotFoundException e) {
      setEmptyText(R.string.error_file_not_found);
    } catch (Exception e) {
      e.printStackTrace();
      setEmptyText(R.string.error_file_format);
    }
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case MESSAGE_DECRYPT:
        Log.d(TAG, "Decryption complete");

        if (resultCode != RESULT_OK || data == null) {
          Log.d(TAG, "Bad result code or data null");
          setEmptyText(R.string.error_could_not_decrypt);
          break;
        } 

        String[] lines = data.getStringExtra("decryptedMessage").split("\n");
        for (int i = 0; i < lines.length; ++i) {
          list.add(new Entry(lines[i]));
        }
        break;
    }
  }
}
