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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PasswordEntryAdapter extends ArrayAdapter<Entry> {
  public PasswordEntryAdapter(Context context) {
    super(context, android.R.id.empty);
  }

  @Override public View getView(int pos, View view, ViewGroup group) {
    if (view == null) {
      view = View.inflate(getContext(), android.R.layout.two_line_list_item, null);
    }

    Entry entry = (Entry) getItem(pos);

    ((TextView) view.findViewById(android.R.id.text1)).setText(entry.account);
    ((TextView) view.findViewById(android.R.id.text2)).setText(entry.user);

    return view;
  }

  public String[] toStringArray() {
    int count = getCount();

    String[] entries = new String[count];
    for (int i = 0; i < count; ++i) {
      entries[i] = getItem(i).toLine();
    }

    return entries;
  }

  public void populate(String[] entries) {
    for (int i = 0; i < entries.length; ++i) {
      add(new Entry(entries[i]));
    }
  }
}
