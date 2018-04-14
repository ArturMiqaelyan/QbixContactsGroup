package com.qbix.qbixcontactgrouplib;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GroupHelper {

    private volatile static GroupHelper instance = null;

    private GroupHelper() {
    }

    public static GroupHelper getInstance() {

        if (instance == null) {
            synchronized (GroupHelper.class) {
                if (instance == null) {
                    instance = new GroupHelper();
                }
            }
        }

        return instance;
    }

    /**
     * Gets all existing rawContactId and label id pairs
     *
     * @param context Context instance for db interactions
     * @return RawIdLabelId object that contains {@link RawIdLabelId#labelId} and {@link RawIdLabelId#rawId}
     */
    protected List<RawIdLabelId> getExistingRawIdLabelIdPairs(Context context) {
        List<RawIdLabelId> list = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
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
     * @param context Context instance for db interactions
     * @return HashMap that contains rawContactId and contact id
     * (key - rawContactId, value - contact id)
     */
    protected HashMap<String, String> getExistingRawIdContactIdPairs(Context context) {
        HashMap<String, String> map = new HashMap<>();
        Cursor cursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI,
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
     * @param context Context instance for db interactions
     * @param contactIds Array of contactIds which rawContactId's are needed
     * @return rawContactId array
     */
    protected String[] getRawContactIds(Context context, String[] contactIds) {
        List<String> rawIdList = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[]{ContactsContract.RawContacts._ID},
                ContactsContract.RawContacts.CONTACT_ID + GroupHelper.getInstance().getSuffix(contactIds.length),
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
     * Gets all account names of for rawContactIds and set them into HashMap.
     *
     * @param context Context instance for db interactions
     * @param rawContactIds rawContactIds which account names wanted to be returned
     * @return HashMap that contains rawContactId and its account name
     * (key - raw contact id, value - account name)
     */
    protected HashMap<String, String> getRawContactIdAccountNamePair(Context context, String[] rawContactIds) {
        HashMap<String, String> map = new HashMap<>();
        Cursor cursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI,
                new String[]{
                        ContactsContract.RawContacts._ID,
                        ContactsContract.RawContacts.ACCOUNT_NAME
                },
                ContactsContract.RawContacts._ID + GroupHelper.getInstance().getSuffix(rawContactIds.length),
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
     * @param context Context instance for db interactions
     * @param sourceId sourceId which labelIds wanted to be returned
     * @return HashMap that contains rawContactId and its account name
     * (key - account name, value - label id)
     */
    protected HashMap<String, String> getAccountNameLabelIdPair(Context context, String sourceId) {
        HashMap<String, String> map = new HashMap<>();
        Cursor cursor = context.getContentResolver().query(ContactsContract.Groups.CONTENT_URI,
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
     * @param context Context instance for db interactions
     * @param rawContactIds Array of rawContactIds which labels wanted to be returned
     * @return HashMap that contains rawContactId and label id
     * (key - rawContactId, value - label id)
     */
    protected HashMap<String, String> getExistingRawIdLabelIdPairs(Context context, String[] rawContactIds) {
        HashMap<String, String> map = new HashMap<>();
        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                new String[]{
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.Data.RAW_CONTACT_ID,
                        ContactsContract.Data.DATA1
                },
                ContactsContract.Data.MIMETYPE + "='" +
                        ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE +
                        "' AND " + ContactsContract.Data.RAW_CONTACT_ID + GroupHelper.getInstance().getSuffix(rawContactIds.length),
                rawContactIds,
                null);
        while (cursor.moveToNext()) {
            map.put(cursor.getString(cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID)),
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1)));
        }
        cursor.close();
        return map;
    }

    /**
     * @param context Context instance for getting account manager
     *
     * Forces the system to sync all accounts.(if you dont sync some deleted data can be shown to user
     * as before till system syncs automatically).
     */
    protected void requestSyncNow(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                AccountManager accountManager = AccountManager.get(context);
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
}
