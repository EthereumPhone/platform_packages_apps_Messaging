/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.messaging.ui.contact;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.SectionIndexer;

import java.util.HashMap;
import java.util.Map;
import android.database.MatrixCursor;

import com.android.messaging.R;
import com.android.messaging.util.Assert;

public class ContactListAdapter extends CursorAdapter implements SectionIndexer {
    private final ContactListItemView.HostInterface mClivHostInterface;
    private final boolean mNeedAlphabetHeader;
    private ContactSectionIndexer mSectionIndexer;

    private Cursor uniqueCursor; // Member to hold the unique cursor

    public ContactListAdapter(final Context context, final Cursor cursor,
            final ContactListItemView.HostInterface clivHostInterface,
            final boolean needAlphabetHeader) {
        super(context, cursor, 0);
        mClivHostInterface = clivHostInterface;
        mNeedAlphabetHeader = needAlphabetHeader;
        uniqueCursor = removeDuplicates(cursor); // Remove duplicates and store
        mSectionIndexer = new ContactSectionIndexer(uniqueCursor); // Use uniqueCursor for indexing
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        Assert.isTrue(view instanceof ContactListItemView);
        if (uniqueCursor != null && !uniqueCursor.isClosed()) {
            uniqueCursor.moveToPosition(cursor.getPosition());
            final ContactListItemView contactListItemView = (ContactListItemView) view;
            String alphabetHeader = null;
            if (mNeedAlphabetHeader) {
                final int position = cursor.getPosition();
                final int section = mSectionIndexer.getSectionForPosition(position);
                // Check if the position is the first in the section.
                if (mSectionIndexer.getPositionForSection(section) == position) {
                    alphabetHeader = (String) mSectionIndexer.getSections()[section];
                }
            }
            contactListItemView.bind(cursor, mClivHostInterface, mNeedAlphabetHeader, alphabetHeader);
        }
        
    }


    public Cursor removeDuplicates(Cursor cursor) {
        // Define the columns
        String[] columns = new String[]{
            "contact_id", "display_name", "photo_thumb_uri", "data1",
            "data2", "data3", "lookup", "_id", "sort_key"
        };
    
        // Create a MatrixCursor to store unique entries
        MatrixCursor uniqueCursor = new MatrixCursor(columns);
    
        // HashMap to store unique contacts with contact_id as key
        Map<String, Object[]> uniqueEntries = new HashMap<>();
    
        // Check if the cursor is valid
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String contactId = cursor.getString(cursor.getColumnIndex("contact_id"));
    
                // Check if this contact_id is already processed
                if (!uniqueEntries.containsKey(contactId)) {
                    // Extract data for each column
                    Object[] rowData = new Object[columns.length];
                    for (int i = 0; i < columns.length; i++) {
                        int columnIndex = cursor.getColumnIndex(columns[i]);
                        switch (cursor.getType(columnIndex)) {
                            case Cursor.FIELD_TYPE_STRING:
                                rowData[i] = cursor.getString(columnIndex);
                                break;
                            case Cursor.FIELD_TYPE_INTEGER:
                                rowData[i] = cursor.getInt(columnIndex);
                                break;
                            case Cursor.FIELD_TYPE_FLOAT:
                                rowData[i] = cursor.getFloat(columnIndex);
                                break;
                            case Cursor.FIELD_TYPE_BLOB:
                                rowData[i] = cursor.getBlob(columnIndex);
                                break;
                            case Cursor.FIELD_TYPE_NULL:
                            default:
                                rowData[i] = null;
                                break;
                        }
                    }
                    // Add the row data to the map
                    uniqueEntries.put(contactId, rowData);
                }
            } while (cursor.moveToNext());
        }
    
        // Add unique entries to the MatrixCursor
        for (Object[] rowData : uniqueEntries.values()) {
            uniqueCursor.addRow(rowData);
        }
    
        return uniqueCursor;
    }
    
    

    public void printCursorData(Cursor cursor, boolean isDEBUG) {
        if (cursor != null) {
            // Check if the cursor contains any rows
            if (cursor.moveToFirst()) {
                // Get the index of each column
                int contactIdIndex = cursor.getColumnIndex("contact_id");
                int displayNameIndex = cursor.getColumnIndex("display_name");
                int photoThumbUriIndex = cursor.getColumnIndex("photo_thumb_uri");
                int data1Index = cursor.getColumnIndex("data1");
                int data2Index = cursor.getColumnIndex("data2");
                int data3Index = cursor.getColumnIndex("data3");
                int lookupIndex = cursor.getColumnIndex("lookup");
                int idIndex = cursor.getColumnIndex("_id");
                int sortKeyIndex = cursor.getColumnIndex("sort_key");
    
                // Iterate over the rows in the cursor
                do {
                    // Read the values from each column
                    String contactId = cursor.getString(contactIdIndex);
                    String displayName = cursor.getString(displayNameIndex);
                    String photoThumbUri = cursor.getString(photoThumbUriIndex);
                    String data1 = cursor.getString(data1Index);
                    String data2 = cursor.getString(data2Index);
                    String data3 = cursor.getString(data3Index);
                    String lookup = cursor.getString(lookupIndex);
                    String id = cursor.getString(idIndex);
                    String sortKey = cursor.getString(sortKeyIndex);
    
                    // Print the values
                    if (isDEBUG) {
                        System.out.println("ETHOSDEBUG: Contact ID: " + contactId);
                        System.out.println("ETHOSDEBUG: Display Name: " + displayName);
                        System.out.println("ETHOSDEBUG: Photo Thumbnail URI: " + photoThumbUri);
                        System.out.println("ETHOSDEBUG: Data1: " + data1);
                        System.out.println("ETHOSDEBUG: Data2: " + data2);
                        System.out.println("ETHOSDEBUG: Data3: " + data3);
                        System.out.println("ETHOSDEBUG: Lookup: " + lookup);
                        System.out.println("ETHOSDEBUG: ID: " + id);
                        System.out.println("ETHOSDEBUG: Sort Key: " + sortKey);
                        System.out.println("ETHOSDEBUG: --------------------------------------------------");
                    } else {
                        System.out.println("Contact ID: " + contactId);
                        System.out.println("Display Name: " + displayName);
                        System.out.println("Photo Thumbnail URI: " + photoThumbUri);
                        System.out.println("Data1: " + data1);
                        System.out.println("Data2: " + data2);
                        System.out.println("Data3: " + data3);
                        System.out.println("Lookup: " + lookup);
                        System.out.println("ID: " + id);
                        System.out.println("Sort Key: " + sortKey);
                        System.out.println("--------------------------------------------------");
                    }

    
                } while (cursor.moveToNext());
            } else {
                System.out.println("Cursor is empty.");
            }
        } else {
            System.out.println("Cursor is null.");
        }
    }    

    @Override
    public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        return layoutInflater.inflate(R.layout.contact_list_item_view, parent, false);
    }

    @Override
    public Cursor swapCursor(final Cursor newCursor) {
        uniqueCursor = removeDuplicates(newCursor); // Update uniqueCursor
        mSectionIndexer = new ContactSectionIndexer(uniqueCursor); // Update section indexer
        return super.swapCursor(uniqueCursor); 
    }

    @Override
    public Object[] getSections() {
        return mSectionIndexer.getSections();
    }

    @Override
    public int getPositionForSection(final int sectionIndex) {
        return mSectionIndexer.getPositionForSection(sectionIndex);
    }

    @Override
    public int getSectionForPosition(final int position) {
        return mSectionIndexer.getSectionForPosition(position);
    }
}
