package com.qbix.qbixcontactgrouplib;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.qbix.qbixcontactgrouplib.models.QbixGroup;

import java.util.ArrayList;
import java.util.List;

public class QUsersCordova extends CordovaPlugin {

    //Actions
    private final String GET_ALL_LABELS_ACTION = "getAll";
    private final String GET_ONE_OR_MORE_LABELS_ACTION = "get";
    private final String REMOVE_CONTACT_FROM_LABEL_ACTION = "removeContact";
    private final String ADD_CONTACT_TO_LABEL_ACTION = "addContact";
    private final String REMOVE_LABEL_ACTION = "remove";

    private final String READ = Manifest.permission.READ_CONTACTS;
    private final String WRITE = Manifest.permission.WRITE_CONTACTS;
    private final String ACCOUNTS = Manifest.permission.GET_ACCOUNTS;

    //Request code for the permissions picker (Pick is async and uses intents)
    private final int ALL_LABELS_REQ_CODE = 8;
    private final int LABELS_BY_SOURCE_ID_REQ_CODE = 9;
    private final int REMOVE_CONTACT_FROM_LABEL_REQ_CODE = 10;
    private final int ADD_CONTACT_TO_LABEL_REQ_CODE = 11;
    private final int REMOVE_LABEL_REQ_CODE = 12;

    //Error codes for returning with error plugin result
    protected static final String UNKNOWN_ERROR = "unknown error";
    protected static final String SUCCESS = "success";
    protected static final String NOT_SUPPORTED_ERROR = "not supported error";
    protected static final String PERMISSION_DENIED_ERROR = "permission denied error";

    private JSONArray executeArgs;
    private GroupAccessor groupAccessor;
    private CallbackContext callbackContext;   // The callback context from which we were invoked.

    /**
     * Constructor.
     */
    public QUsersCordova() {
    }

    private void getReadPermission(int requestCode) {
        PermissionHelper.requestPermission(this, requestCode, READ);
    }

    private void getWritePermission(int requestCode) {
        PermissionHelper.requestPermission(this, requestCode, WRITE);
    }

    private void getAccountPermission(int requestCode) {
        PermissionHelper.requestPermission(this, requestCode, ACCOUNTS);
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action          The action to execute.
     * @param args            JSONArray of arguments for the plugin.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return True if the action was valid, false otherwise.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

        this.callbackContext = callbackContext;
        this.executeArgs = args;
        /**
         * Check to see if we are on an Android 1.X device.  If we are return an error as we
         * do not support this as of Cordova 1.0.
         */
        if (android.os.Build.VERSION.RELEASE.startsWith("1.")) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, NOT_SUPPORTED_ERROR));
            return true;
        }

        /**
         * Only create the groupAccessor after we check the Android version or the program will crash
         * older phones.
         */
        if (this.groupAccessor == null) {
            this.groupAccessor = new GroupAccessor(this.cordova);
        }

        if (action.equals(GET_ALL_LABELS_ACTION)) {
            if (PermissionHelper.hasPermission(this, READ)) {
                getLabels();
            } else {
                getReadPermission(ALL_LABELS_REQ_CODE);
            }

            return true;
        } else if (action.equals(GET_ONE_OR_MORE_LABELS_ACTION)) {
            if (PermissionHelper.hasPermission(this, READ)) {
                this.cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        try {
                            getLabels(executeArgs);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, e.getMessage()));
                        }
                    }
                });
            } else {
                getReadPermission(LABELS_BY_SOURCE_ID_REQ_CODE);
            }
        } else if (action.equals(REMOVE_CONTACT_FROM_LABEL_ACTION)) {
            if (PermissionHelper.hasPermission(this, WRITE)) {
                this.cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        try {
                            removeContactFromLabel(executeArgs);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, e.getMessage()));
                        }
                    }
                });
            } else {
                getWritePermission(REMOVE_CONTACT_FROM_LABEL_REQ_CODE);
            }
            return true;
        } else if (action.equals(ADD_CONTACT_TO_LABEL_ACTION)) {
            if (PermissionHelper.hasPermission(this, WRITE)) {
                this.cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        try {
                            addContactToLabel(executeArgs);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, e.getMessage()));
                        }
                    }
                });
            } else {
                getWritePermission(ADD_CONTACT_TO_LABEL_REQ_CODE);
            }
            return true;
        } else if (action.equals(REMOVE_LABEL_ACTION)) {
            if (PermissionHelper.hasPermission(this, ACCOUNTS)) {
                this.cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        removeLabelFromDatabase(executeArgs);
                    }
                });
            } else {
                getAccountPermission(REMOVE_LABEL_REQ_CODE);
            }
            return true;
        }
        return false;
    }

    /**
     * Gets all labels asynchronously and set result to callback context's as success.
     *
     */
    private void getLabels() {
        this.cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                List<QbixGroup> labels = groupAccessor.getAllLabels(this.cordova);
                JSONArray jsonGroups = new JSONArray();
                for (QbixGroup group :
                        labels) {
                    jsonGroups.put(group.toJson());
                }
                if (labels != null) {
                    callbackContext.success(jsonGroups);
                } else {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, UNKNOWN_ERROR));
                }
            }
        });
    }

    /**
     * Gets all labels that have given sourceIds and set result to callback context's as success.
     *
     * @param args Arguments from {@link #execute(String, JSONArray, CallbackContext)} method
     */
    private void getLabels(JSONArray args) {
        try {
            List<String> sourceIdList = new ArrayList<>();
            for (int i = 0; i < args.length(); i++) {
                JSONObject sourceIdJson = args.getJSONObject(i);
                String sourceId = sourceIdJson.getString("labelId");
                sourceIdList.add(sourceId);
            }
            String[] sourceIdArray = new String[sourceIdList.size()];
            for (int i = 0; i < sourceIdList.size(); i++) {
                sourceIdArray[i]=sourceIdList.get(i);
            }
            List<QbixGroup> labels = groupAccessor.getLabelsBySourceId(sourceIdArray);
            JSONArray jsonGroups = new JSONArray();
            for (QbixGroup group :
                    labels) {
                jsonGroups.put(group.toJson());
            }
            if (labels != null) {
                callbackContext.success(jsonGroups);
            } else {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, UNKNOWN_ERROR));
            }
        } catch (JSONException e) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, e.getMessage()));
        }
    }

    /**
     * Removes contacts from given label
     *
     * @param args Arguments from {@link #execute(String, JSONArray, CallbackContext)} method
     */
    private void removeContactFromLabel(JSONArray args) {
        try {
            final JSONObject filter = args.getJSONObject(0);
            final String labelId = filter.getString("labelId");
            final JSONArray contactIds = filter.getJSONArray("contactIds");
            String[] idArray = new String[contactIds.length()];
            for (int i = 0; i < contactIds.length(); i++) {
                JSONObject row = contactIds.getJSONObject(i);
                idArray[i] = row.getString("contactId");
            }
            String removeMessage = groupAccessor.removeLabelFromContacts(labelId, idArray);
            if (removeMessage.equals(SUCCESS)) {
                callbackContext.success();
            } else {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, removeMessage));
            }
        } catch (JSONException e) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, e.getMessage()));
        }
    }

    /**
     * Adds label to given contacts
     *
     * @param args Arguments from {@link #execute(String, JSONArray, CallbackContext)} method
     */
    private void addContactToLabel(JSONArray args) {
        try {
            final JSONObject filter = args.getJSONObject(0);
            final String labelId = filter.getString("labelId");
            final JSONArray contactIds = filter.getJSONArray("contactIds");
            String[] idArray = new String[contactIds.length()];
            for (int i = 0; i < contactIds.length(); i++) {
                JSONObject row = contactIds.getJSONObject(i);
                idArray[i] = row.getString("contactId");
            }
            String addMessage = groupAccessor.addLabelToContacts(labelId, idArray);
            if (addMessage.equals(SUCCESS)) {
                callbackContext.success();
            } else if (addMessage.equals(UNKNOWN_ERROR)) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, addMessage));
            } else {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, addMessage));
            }
        } catch (JSONException e) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, e.getMessage()));
        }
    }

    /**
     * Removes all labels with given sourceId.
     *
     * @param args Arguments from {@link #execute(String, JSONArray, CallbackContext)} method
     */
    private void removeLabelFromDatabase(JSONArray args) {
        try {
            final JSONObject filter = args.getJSONObject(0);
            final String sourceId = filter.getString("labelId");
            String removeMessage = groupAccessor.removeLabelFromData(sourceId);
            if (removeMessage.equals(SUCCESS)) {
                callbackContext.success();
            } else {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, removeMessage));
            }
        } catch (JSONException e) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, e.getMessage()));
        }

    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
                return;
            }
        }
        switch (requestCode) {
            case ALL_LABELS_REQ_CODE:
                getLabels();
                break;
            case REMOVE_CONTACT_FROM_LABEL_REQ_CODE:
                this.cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        try {
                            removeContactFromLabel(executeArgs);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, e.getMessage()));
                        }
                    }
                });
                break;
            case REMOVE_LABEL_REQ_CODE:
                this.cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        removeLabelFromDatabase(executeArgs);
                    }
                });
                break;
            case LABELS_BY_SOURCE_ID_REQ_CODE:
                this.cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        try {
                            getLabels(executeArgs);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, e.getMessage()));
                        }
                    }
                });
                break;
        }
    }

    /**
     * This plugin launches an external Activity when a contact is picked, so we
     * need to implement the save/restore API in case the Activity gets killed
     * by the OS while it's in the background. We don't actually save anything
     * because picking a contact doesn't take in any arguments.
     */
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
        this.groupAccessor = new GroupAccessor(this.cordova);
    }
}
