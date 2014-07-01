/* Copyright (c) 2014 Alan Berndt
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

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import android.support.v7.app.ActionBarActivity;

public abstract class ActionBarListActivity extends ActionBarActivity {
  private Handler handler = new Handler();
  private boolean finished = false;

  private Runnable requestFocus = new Runnable() {
    public void run() {
      view.focusableViewAvailable(view);
    }
  };

  private AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
    public void onItemClick(AdapterView parent, View v, int position, long id) {
      onListItemClick((ListView) parent, v, position, id);
    }
  };

  private void ensureList() {
    if (view == null) {
      setContentView(android.R.layout.list_content);
    }
  }

  protected ListView view;
  protected ListAdapter adapter;

  protected void onListItemClick(ListView list, View view, int pos, long id) {}

  @Override protected void onRestoreInstanceState(Bundle state) {
    ensureList();
    super.onRestoreInstanceState(state);
  }

  @Override protected void onDestroy() {
    handler.removeCallbacks(requestFocus);
    super.onDestroy();
  }

  @Override public void onSupportContentChanged() {
    super.onSupportContentChanged();

    View empty = findViewById(android.R.id.empty);
    view = (ListView) findViewById(android.R.id.list);
    if (view == null) {
      throw new RuntimeException("Content must have ListView with id android.R.id.list");
    }
    if (empty != null) view.setEmptyView(empty);
    if (finished) setListAdapter(adapter);
    view.setOnItemClickListener(listener);
    handler.post(requestFocus);
    finished = true;
  }

  public void setListAdapter(ListAdapter adapter) {
    getListView().setAdapter(adapter);
  }

  public void setSelection(int position) {
    view.setSelection(position);
  }

  public int getSelectedItemPosition() {
    return view.getSelectedItemPosition();
  }

  public long getSelectedItemId() {
    return view.getSelectedItemId();
  }

  public ListView getListView() {
    ensureList();
    return view;
  }

  public ListAdapter getListAdapter() {
    return adapter;
  }


}
