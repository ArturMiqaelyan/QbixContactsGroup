package com.qbix.qbixcontactgrouplib;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
                        ContactsContract.Groups._ID,
                        ContactsContract.Groups.SOURCE_ID,
                        ContactsContract.Groups.TITLE,
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
        List<RawIdLabelId> rawIdLabelIds = getExistingRawIdLabelIdPairs();
        HashMap<String, String> rawIdContactIdPair = getExistingRawIdContactIdPairs();
        while (cursor.moveToNext()) {
            QbixGroup group = new QbixGroup();

            group.sourceId = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.SOURCE_ID));
            group.title = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.TITLE));
            group.notes = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.NOTES));
            group.summaryCount = cursor.getInt(cursor.getColumnIndex(ContactsContract.Groups.SUMMARY_COUNT));
            group.isVisible = cursor.getInt(cursor.getColumnIndex(ContactsContract.Groups.GROUP_VISIBLE)) == 0;
            group.isDeleted = cursor.getInt(cursor.getColumnIndex(ContactsContract.Groups.DELETED)) == 1;
            group.shouldSync = cursor.getInt(cursor.getColumnIndex(ContactsContract.Groups.SHOULD_SYNC)) == 1;
            group.readOnly = cursor.getInt(cursor.getColumnIndex(ContactsContract.Groups.GROUP_IS_READ_ONLY)) == 1;
            Log.i("group_info_checker", "id: " + cursor.getString(cursor.getColumnIndex(ContactsContract.Groups._ID)));
            Log.i("group_info_checker", "source_id: " + group.sourceId);
            Log.i("group_info_checker", "title: " + group.title);
            Log.i("group_info_checker", "notes: " + group.notes);
            Log.i("group_info_checker", "summary_count: " + group.summaryCount);
            Log.i("group_info_checker", "is_visible: " + group.isVisible);
            Log.i("group_info_checker", "deleted: " + group.isDeleted);
            Log.i("group_info_checker", "should_sync: " + group.shouldSync);
            Log.i("group_info_checker", "read_only: " + group.readOnly);
            String labelId = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups._ID));
            List<Integer> rawIds = new ArrayList<>();
            for (int i = 0; i < rawIdLabelIds.size(); i++) {
                if (rawIdLabelIds.get(i).labelId.equals(labelId)) {
                    rawIds.add(Integer.valueOf(rawIdLabelIds.get(i).rawId));
                }
            }
            List<Integer> contactIds = getContactIds(rawIdContactIdPair, rawIds);
            if (!sourceIds.contains(cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.SOURCE_ID)))) {
                group.contactIds = contactIds;
                sourceIds.add(cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.SOURCE_ID)));
                labels.add(group);
            } else {
                int index = sourceIds.indexOf(cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.SOURCE_ID)));
                labels.get(index).contactIds.addAll(contactIds);
                Log.i("group_info_checker", "group: " + cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.SOURCE_ID)) + " is existing");
            }

        }
        cursor.close();
        return labels;
    }

    /**
     * Converts given rawContactId/contactId pairs into only contact id list.
     *
     * @param rawIdContactId HashMap that contains rawContactId(key) and contactId(value)
     * @param rawIds         List of rawContactIds that needed to be converted.
     * @return List converted contactIds
     */
    public List<Integer> getContactIds(HashMap<String, String> rawIdContactId, List<Integer> rawIds) {
        List<Integer> contactIds = new ArrayList<>();
        for (int i = 0; i < rawIdContactId.size(); i++) {
            if (!contactIds.contains(Integer.valueOf(rawIdContactId.get(String.valueOf(rawIds.get(i)))))) {
                contactIds.add(Integer.valueOf(rawIdContactId.get(String.valueOf(rawIds.get(i)))));
                Log.i("contactId_checker", "rawId: " + rawIds.get(i) + "\ncontactId: " + rawIdContactId.get(String.valueOf(rawIds.get(i))));
            } else {
                Log.i("contactId_checker", "contains: " + rawIdContactId.get(String.valueOf(rawIds.get(i))));
            }
        }
        return rawIds;
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
                new String[]{ContactsContract.RawContacts._ID},
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
     * @param sourceId   source id which label wanted to be added
     * @param contactIds Contact id's array to which label should be added
     * @return success message if succeeded and exception message if failed
     */
    protected String addLabelToContacts(String sourceId, String[] contactIds) {
        ArrayList<ContentProviderOperation> ops =
                new ArrayList<>();
        String[] rawContactIds = getRawContactIds(contactIds);
        HashMap<String, String> rawIdAccName = getRawContactIdAccountNamePair(rawContactIds);
        HashMap<String, String> accNameLabelId = getAccountNameLabelIdPair(sourceId);
        HashMap<String, String> existingLabels = getExistingRawIdLabelIdPairs(rawContactIds);
        for (int i = 0; i < rawContactIds.length; i++) {
            String labelId = accNameLabelId.get(rawIdAccName.get(rawContactIds[i]));
            if (labelId != null) {
                if (!existingLabels.get(rawContactIds[i]).equals(labelId)) {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactIds[i])
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.Data.DATA1, accNameLabelId.get(rawIdAccName.get(rawContactIds[i])))
                            .withYieldAllowed(i == contactIds.length - 1)
                            .build());
                } else {
                    Log.d("duplicate_checker", "duplicate!!! " + rawContactIds[i]);
                }
            } else {
                Log.d("duplicate_checker", "no label for that contact" + rawContactIds[i]);
            }
        }

        try {
            ContentProviderResult[] results = app.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            if (results.length >= 1) {
                Log.d("duplicate_checker", results[0].toString());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return e.getMessage()
        } catch (OperationApplicationException e) {
            e.printStackTrace();
            return e.getMessage();
        }
        return GroupManager.SUCCESS;
    }

    /**
     * Gets all account names of for rawContactIds and set them into HashMap.
     *
     * @param rawContactIds rawContactIds which account names wanted to be returned
     * @return HashMap that contains rawContactId and its account name
     * (key - raw contact id, value - account name)
     */
    private HashMap<String, String> getRawContactIdAccountNamePair(String[] rawContactIds) {
        HashMap<String, String> map = new HashMap<>();
        Cursor cursor = app.getActivity().getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI,
                new String[]{
                        ContactsContract.RawContacts._ID,
                        ContactsContract.RawContacts.ACCOUNT_NAME
                },
                ContactsContract.RawContacts._ID + getSuffix(rawContactIds.length),
                rawContactIds,
                null);
        while (cursor.moveToNext()) {
            map.put(cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts._ID)),
                    cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)));
        }
        cursor.close();
        return map;
    }

    /**
     * Gets all label ids for sourceId and binds them to their account names.
     *
     * @param sourceId sourceId which labelIds wanted to be returned
     * @return HashMap that contains rawContactId and its account name
     * (key - account name, value - label id)
     */
    private HashMap<String, String> getAccountNameLabelIdPair(String sourceId) {
        HashMap<String, String> map = new HashMap<>();
        Cursor cursor = app.getActivity().getContentResolver().query(ContactsContract.Groups.CONTENT_URI,
                new String[]{
                        ContactsContract.Groups._ID,
                        ContactsContract.Groups.SOURCE_ID,
                        ContactsContract.Groups.ACCOUNT_NAME
                },
                ContactsContract.Groups.SOURCE_ID + "='" + sourceId + "'",
                null,
                null);
        while (cursor.moveToNext()) {
            map.put(cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.ACCOUNT_NAME)),
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Groups._ID)));
        }
        cursor.close();
        return map;
    }

    /**
     * Gets all existing labels for given rawContactIds.
     *
     * @param rawContactIds Array of rawContactIds which labels wanted to be returned
     * @return HashMap that contains rawContactId and label id
     * (key - rawContactId, value - label id)
     */
    private HashMap<String, String> getExistingRawIdLabelIdPairs(String[] rawContactIds) {
        HashMap<String, String> map = new HashMap<>();
        Cursor cursor = app.getActivity().getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                new String[]{
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.Data.RAW_CONTACT_ID,
                        ContactsContract.Data.DATA1
                },
                ContactsContract.Data.MIMETYPE + "='" +
                        ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE +
                        "' AND " + ContactsContract.Data.RAW_CONTACT_ID + getSuffix(rawContactIds.length),
                rawContactIds,
                null);
        while (cursor.moveToNext()) {
            map.put(cursor.getString(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID)),
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1)));
        }
        cursor.close();
        return map;
    }

    private List<RawIdLabelId> getExistingRawIdLabelIdPairs() {
        List<RawIdLabelId> list = new ArrayList<>();
        Cursor cursor = app.getActivity().getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                new String[]{
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.Data.RAW_CONTACT_ID,
                        ContactsContract.Data.DATA1
                },
                ContactsContract.Data.MIMETYPE + "='" +
                        ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'",
                null,
                null);
        while (cursor.moveToNext()) {
            RawIdLabelId rawIdLabelId = new RawIdLabelId(cursor.getString(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID)),
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1)));
            list.add(rawIdLabelId);
        }
        cursor.close();
        return list;
    }

    /**
     * Gets all existing contactIds for all rawContactIds.
     *
     * @return HashMap that contains rawContactId and contact id
     * (key - rawContactId, value - contact id)
     */
    private HashMap<String, String> getExistingRawIdContactIdPairs() {
        HashMap<String, String> map = new HashMap<>();
        Cursor cursor = app.getActivity().getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI,
                new String[]{
                        ContactsContract.RawContacts._ID,
                        ContactsContract.RawContacts.CONTACT_ID
                },
                null,
                null,
                null);
        while (cursor.moveToNext()) {
            map.put(cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts._ID)),
                    cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)));
        }
        cursor.close();
        return map;
    }


    /**
     * Gets cursor, that contains all source ids and label ids for given source id.
     * (can contain multiple values, because Android System allows to create groups with the same name)
     *
     * @param sourceId Label source id
     * @return Cursor, that contains all groups that matches given sourceId name.
     */
    private Cursor getGroupTitle(String sourceId) {
        Cursor cursor = app.getActivity().getContentResolver().query(
                ContactsContract.Groups.CONTENT_URI,
                new String[]{
                        ContactsContract.Groups._ID,
                        ContactsContract.Groups.SOURCE_ID
                }, ContactsContract.Groups.SOURCE_ID + "=?", new String[]{sourceId}, null
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
     * @param sourceId Source id that is wanted to be deleted
     * @return success message if succeed and exception message if failed
     */
    protected String removeLabelFromData(String sourceId) {
        ArrayList<ContentProviderOperation> ops =
                new ArrayList<>();

        ops.add(ContentProviderOperation.newDelete(ContactsContract.Groups.CONTENT_URI)
                .withSelection(ContactsContract.Groups.SOURCE_ID + "=?", new String[]{sourceId})
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


    private void getAllContactsForLabel(String sourceId) {

    }

}
