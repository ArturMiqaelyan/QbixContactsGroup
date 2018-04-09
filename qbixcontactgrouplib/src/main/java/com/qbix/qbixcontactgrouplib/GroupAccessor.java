package com.qbix.qbixcontactgrouplib;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
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

    protected GroupAccessor(CordovaInterface context) {
        this.app = context;
    }

    /**
     * Gets all available groups for users.
     * It will not get any system related labels
     * (such as "Starred in Android" or "My Contacts") and labels that are marked for deletion.
     *
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

    /**
     * Builds a string for selection query, based on selection arguments count.
     *
     * @param count Size of selectionArgs
     * @return string for query selection (example: "IN(?,?...,?)")
     */
    private String getSuffix(int count) {
        String selectionSuffix = " IN(";
        if (count == 1) {
            //checks if there is 1 argument
            selectionSuffix += "?)";
        } else {
            for (int i = 0; i < count; i++) {
                if (i == 0) {
                    //for first argument
                    selectionSuffix += "?";
                } else if (i != (count - 1)) {
                    //for following arguments
                    selectionSuffix += ",?";
                } else {
                    //for last argument
                    selectionSuffix += ",?)";
                }
            }
        }
        Log.d("suffix_checker", "count: " + count + " suffix: " + selectionSuffix);
        return selectionSuffix;
    }

    /**
     * Removes label from contacts.
     *
     * @param labelId The label id that wanted to be removed
     * @param args    Array of contact ids from which label must be removed
     * @return success message if succeeded and exception message if failed
     */
    protected String removeLabelFromContacts(String labelId, String[] args) {
        ArrayList<ContentProviderOperation> ops =
                new ArrayList<>();

        ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection(ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE
                                + "' AND " + ContactsContract.Data.DATA1 + "='" + labelId
                                + "' AND " + ContactsContract.Data.CONTACT_ID + getSuffix(args.length),
                        args)
                .build());
        try {
            app.getActivity().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
            return e.getMessage();
        }
        return GroupManager.SUCCESS;
    }

    /**
     * Converts label title into its id
     *
     * @param title Title of label
     * @return array of ids that matches title requirement
     */
    private String[] getLabelId(String title) {
        List<String> idsList = new ArrayList<>();
        Cursor cursor = app.getActivity().getContentResolver().query(
                ContactsContract.Groups.CONTENT_URI,
                new String[]{
                        ContactsContract.Groups._ID,
                        ContactsContract.Groups.TITLE
                }, ContactsContract.Groups.TITLE + "=?", new String[]{title}, null
        );

        while (cursor.moveToNext()) {
            idsList.add(cursor.getString(0));
        }
        String[] idsArray = new String[idsList.size()];
        for (int i = 0; i < idsList.size(); i++) {
            idsArray[i] = idsList.get(i);
        }
        return idsArray;
    }

    /**
     * Add label to given contacts.
     * @param labelId Label id that wanted to be added
     * @param contactIds Contact id's array to which label should be added
     * @return success message if succeeded and exception message if failed
     */
    protected String addLabelToContacts(String labelId, String[] contactIds) {
        ArrayList<ContentProviderOperation> ops =
                new ArrayList<>();
        List<String> existingContacts = getExistingContacts(labelId);
        for (int i = 0; i < contactIds.length; i++) {
            if (!existingContacts.contains(contactIds[i])) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, contactIds[i])
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.Data.DATA1, labelId)
                        .build());
                Log.d("duplicate_checker", "added: "+ contactIds[i]);
            }else {
                Log.d("duplicate_checker", "duplicate!!! "+contactIds[i]);
            }
        }
        try {
            ContentProviderResult[] results = app.getActivity().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            if(results.length>=1){
                Log.d("duplicate_checker", results[0].toString());
                return GroupManager.SUCCESS;
            }else {
                return GroupManager.UNKNOWN_ERROR;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * Gets all contacts attached to given label.
     * @param labelId label's id
     * @return List of contact ids that are attached to label
     */
    private List<String> getExistingContacts(String labelId){
        List<String> contactIds = new ArrayList<>();
        Cursor cursor = getContactsForLabel(new String[]{labelId});
        while (cursor.moveToNext()){
            contactIds.add(cursor.getString(0));
            Log.d("duplicate_list_checker", ""+cursor.getString(0));
        }
        return contactIds;
    }

    /**
     * Gets cursor, that contains all group titles and ids for given label name.
     * (can contain multiple values, because Android System allows to create groups with the same name)
     * @param label label name (title)
     * @return Cursor, that contains all groups that matches given label name.
     */
    private Cursor getGroupTitle(String label) {
        Cursor cursor = app.getActivity().getContentResolver().query(
                ContactsContract.Groups.CONTENT_URI,
                new String[]{
                        ContactsContract.Groups._ID,
                        ContactsContract.Groups.TITLE
                }, ContactsContract.Groups.TITLE + "=?", new String[]{label}, null
        );
        return cursor;
    }

    /**
     * Gets all contact ids, that have given label attached to them.
     * @param labelIds label ids
     * @return Cursor, that contains all contact ids that matches given label.
     */
    private Cursor getContactsForLabel(String[] labelIds) {
        if (labelIds.length != 0) {
            Cursor cursor = app.getActivity().getContentResolver().query(
                    ContactsContract.Data.CONTENT_URI,
                    new String[]{
                            ContactsContract.Data.CONTACT_ID,
                            ContactsContract.Data.DATA1
                    },
                    ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "' AND " + ContactsContract.Data.DATA1 + getSuffix(labelIds.length),
                    labelIds, null
            );
            return cursor;
        } else {
            return null;
        }
    }

    private void getAllContactsForLabel(String label) {
        List<String> args = new ArrayList<>();
        Cursor groupCursor = getGroupTitle(label);
        while (groupCursor.moveToNext()) {
            String id = groupCursor.getString(0);
            args.add(id);
            Log.d("id_checker", "id " + id);
        }
        groupCursor.close();
        String[] argsArray = new String[args.size()];
        for (int i = 0; i < args.size(); i++) {
            argsArray[i] = args.get(i);
        }
        Cursor dataCursor = getContactsForLabel(argsArray);
        if (dataCursor == null) {
            Log.d("group_info_checker", "no matches");
            return;
        }
        while (dataCursor.moveToNext()) {
            String id = dataCursor.getString(0);
            String groupId = dataCursor.getString(1);
            Log.d("group_info_checker", "groupTitle : " + groupId + " contact_id: " + id);
        }
        dataCursor.close();
    }

}
