package com.qbix.qbixcontactgrouplib;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
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
                ContactsContract.Groups.CONTENT_SUMMARY_URI,
                new String[]{
                        ContactsContract.Groups.SOURCE_ID,
                        ContactsContract.Groups.NOTES,
                        ContactsContract.Groups.SUMMARY_COUNT,
                        ContactsContract.Groups.GROUP_VISIBLE,
                        ContactsContract.Groups.DELETED,
                        ContactsContract.Groups.SHOULD_SYNC,
                        ContactsContract.Groups.GROUP_IS_READ_ONLY
                },
                null,
                null,
                null);
        List<String> sourceIds = new ArrayList<>();
        while (cursor.moveToNext()) {
            QbixGroup group = new QbixGroup();
            group.id = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.SOURCE_ID));
            group.notes = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.NOTES));
            group.summaryCount = cursor.getInt(cursor.getColumnIndex(ContactsContract.Groups.SUMMARY_COUNT));
            group.isVisible = cursor.getInt(cursor.getColumnIndex(ContactsContract.Groups.GROUP_VISIBLE)) == 0;
            group.isDeleted = cursor.getInt(cursor.getColumnIndex(ContactsContract.Groups.DELETED)) == 1;
            group.shouldSync = cursor.getInt(cursor.getColumnIndex(ContactsContract.Groups.SHOULD_SYNC)) == 1;
            group.readOnly = cursor.getInt(cursor.getColumnIndex(ContactsContract.Groups.GROUP_IS_READ_ONLY)) == 1;
            Log.i("group_info_checker", "id: " + group.id);
            Log.i("group_info_checker", "notes: " + group.notes);
            Log.i("group_info_checker", "summary_count: " + group.summaryCount);
            Log.i("group_info_checker", "is_visible: " + group.isVisible);
            Log.i("group_info_checker", "deleted: " + group.isDeleted);
            Log.i("group_info_checker", "should_sync: " + group.shouldSync);
            Log.i("group_info_checker", "read_only: " + group.readOnly);

            if(!sourceIds.contains(cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.SOURCE_ID)))){
                sourceIds.add(cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.SOURCE_ID)));
                labels.add(group);
            }else{
                Log.i("group_info_checker", "group: "+ cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.SOURCE_ID))+" is existing");
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
     * Gets all raw_contact_id's for given contactId array.
     *
     * @param contactIds Array of contactIds which rawContactId's are needed
     * @return rawContactId array
     */
    private String[] getRawContactIds(String[] contactIds) {
        List<String> rawIdList = new ArrayList<>();
        Cursor cursor = app.getActivity().getContentResolver().query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[]{
                        ContactsContract.RawContacts._ID,
                },
                ContactsContract.RawContacts.CONTACT_ID + getSuffix(contactIds.length),
                contactIds, null
        );
        while (cursor.moveToNext()) {
            rawIdList.add(cursor.getString(0));
        }
        cursor.close();
        String[] rawIdArray = new String[rawIdList.size()];
        for (int i = 0; i < rawIdArray.length; i++) {
            rawIdArray[i] = rawIdList.get(i);
            Log.d("raw_id_checker", rawIdArray[i]);
        }
        return rawIdArray;
    }

    /**
     * Removes label from contacts.
     *
     * @param labelId    The label id that wanted to be removed
     * @param contactIds Array of contact ids from which label must be removed
     * @return success message if succeeded and exception message if failed
     */
    protected String removeLabelFromContacts(String labelId, String[] contactIds) {
        ArrayList<ContentProviderOperation> ops =
                new ArrayList<>();
        String[] rawIds = getRawContactIds(contactIds);
        ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection(ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE
                                + "' AND " + ContactsContract.Data.DATA1 + "='" + labelId
                                + "' AND " + ContactsContract.Data.RAW_CONTACT_ID + getSuffix(rawIds.length),
                        rawIds)
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
     *
     * @param labelId    Label id that wanted to be added
     * @param contactIds Contact id's array to which label should be added
     * @return success message if succeeded and exception message if failed
     */
    protected String addLabelToContacts(String labelId, String[] contactIds) {
        ArrayList<ContentProviderOperation> ops =
                new ArrayList<>();
        String[] rawIds = getRawContactIds(contactIds);
        List<String> existingContacts = getExistingContacts(labelId);
        for (int i = 0; i < rawIds.length; i++) {
            if (!existingContacts.contains(rawIds[i])) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawIds[i])
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.Data.DATA1, labelId)
                        .build());
                Log.d("duplicate_checker", "added: " + rawIds[i]);
            } else {
                Log.d("duplicate_checker", "duplicate!!! " + rawIds[i]);
            }
        }
        try {
            ContentProviderResult[] results = app.getActivity().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            if (results.length >= 1) {
                Log.d("duplicate_checker", results[0].toString());
                return GroupManager.SUCCESS;
            } else {
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
     *
     * @param labelId label's id
     * @return List of contact ids that are attached to label
     */
    private List<String> getExistingContacts(String labelId) {
        List<String> contactIds = new ArrayList<>();
        Cursor cursor = getContactsForLabel(new String[]{labelId});
        while (cursor.moveToNext()) {
            contactIds.add(cursor.getString(0));
            Log.d("duplicate_list_checker", "" + cursor.getString(0));
        }
        return contactIds;
    }

    /**
     * Gets cursor, that contains all group titles and ids for given label name.
     * (can contain multiple values, because Android System allows to create groups with the same name)
     *
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
     *
     * @param labelIds label ids
     * @return Cursor, that contains all contact ids that matches given label.
     */
    private Cursor getContactsForLabel(String[] labelIds) {
        if (labelIds.length != 0) {
            Cursor cursor = app.getActivity().getContentResolver().query(
                    ContactsContract.Data.CONTENT_URI,
                    new String[]{
                            ContactsContract.Data.RAW_CONTACT_ID,
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

    /**
     * Forces the system to sync all accounts.(if you dont sync some deleted data can be shown to user
     * as before till system syncs automatically).
     */
    private void requestSyncNow() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                AccountManager accountManager = AccountManager.get(app.getActivity());
                Account[] accounts = accountManager.getAccounts();
                boolean isMasterSyncOn = ContentResolver.getMasterSyncAutomatically();


                for (Account account : accounts) {

                    Log.d("sync_checker", "account=" + account);
                    int isSyncable = ContentResolver.getIsSyncable(account,
                            ContactsContract.AUTHORITY);
                    boolean isSyncOn = ContentResolver.getSyncAutomatically(account,
                            ContactsContract.AUTHORITY);
                    Log.d("sync_checker", "Syncable=" + isSyncable + " SyncOn=" + isSyncOn);
                    if (isSyncable > 0 /* && isSyncOn */) {
                        Log.d("sync_checker", "request Sync");
                        Bundle bundle = new Bundle();
                        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_OVERRIDE_TOO_MANY_DELETIONS, true);
                        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                        ContentResolver.requestSync(account, ContactsContract.AUTHORITY, bundle);
                    }
                }
            }
        }, "SyncLauncher").start();

    }

    /**
     * Removes label from database and syncs all accounts.
     *
     * @param labelId Label id that is wanted to be deleted
     * @return success message if succeed and exception message if failed
     */
    protected String removeLabelFromData(String labelId) {
        ArrayList<ContentProviderOperation> ops =
                new ArrayList<>();

        ops.add(ContentProviderOperation.newDelete(ContactsContract.Groups.CONTENT_URI)
                .withSelection(ContactsContract.Groups._ID + "=?", new String[]{labelId})
                .withYieldAllowed(true)
                .build());
        try {
            ContentProviderResult[] result = app.getActivity().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            Log.d("delete_checker", "removeLabelFromData: " + result.toString());
        } catch (RemoteException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (OperationApplicationException e) {
            Log.d("error_tag", e.getMessage());
            e.printStackTrace();
            return e.getMessage();
        }
        requestSyncNow();
        return GroupManager.SUCCESS;
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
