package com.qbix.qbixcontactgrouplib;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * This class defines SDK-independent API for communication with
 * Contacts Provider.
 */
public class GroupAccessor {

    private CordovaInterface app;

    public GroupAccessor(CordovaInterface context) {
        this.app = context;
    }

    /**
     * Gets all available groups for users.
     * It will not get any system related labels
     * (such as "Starred in Android" or "My Contacts") and labels that are marked for deletion.
     * @return list of QbixGroup POJO, that contains group id and group title
     */
    protected List<QbixGroup> getAllLabels() {
        List<QbixGroup> labels = new ArrayList<>();
        //Get all labels (including not visible and deleted ones) from content provider
        Cursor cursor = app.getActivity().getContentResolver().query(
                ContactsContract.Groups.CONTENT_URI,
                new String[]{
                        ContactsContract.Groups._ID,
                        ContactsContract.Groups.TITLE,
                        ContactsContract.Groups.GROUP_VISIBLE,
                        ContactsContract.Groups.DELETED
                },
                null,
                null,
                null);
        while (cursor.moveToNext()) {
            //Check if label is visible and not marked as deleted
            if (cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.GROUP_VISIBLE)).equals("0") &&
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.DELETED)).equals("0")) {
                QbixGroup group = new QbixGroup();
                group.setId(cursor.getLong(cursor.getColumnIndex(ContactsContract.Groups._ID)));
                group.setTitle(cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.TITLE)));
                labels.add(group);
                Log.i("group_info_checker", "id: " + cursor.getLong(cursor.getColumnIndex(ContactsContract.Groups._ID)));
                Log.i("group_info_checker", "title: " + cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.TITLE)));
                Log.i("group_info_checker", "visible: " + cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.GROUP_VISIBLE)));
                Log.i("group_info_checker", "deleted: " + cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.DELETED)));
            }
        }
        cursor.close();
        return labels;
    }
}
