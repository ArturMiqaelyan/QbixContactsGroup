package com.qbix.qbixcontactgrouplib;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.List;

public class GroupManager extends CordovaPlugin {

    private final String GET_ALL_LABELS_ACTION = "getAll";

    private final String READ = Manifest.permission.READ_CONTACTS;
    private final String WRITE = Manifest.permission.WRITE_CONTACTS;

    //Request code for the permissions picker (Pick is async and uses intents)
    private final int ALL_LABELS_REQ_CODE = 8;

    //Error codes for returning with error plugin result
    private final int UNKNOWN_ERROR = 0;
    private final int NOT_SUPPORTED_ERROR = 1;
    private final int PERMISSION_DENIED_ERROR = 2;

    private GroupAccessor groupAccessor;
    private CallbackContext callbackContext;   // The callback context from which we were invoked.

    /**
     * Constructor.
     */
    public GroupManager() {
    }

    private void getReadPermission(int requestCode) {
        PermissionHelper.requestPermission(this, requestCode, READ);
    }

    private void getWritePermission(int requestCode) {
        PermissionHelper.requestPermission(this, requestCode, WRITE);
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArray of arguments for the plugin.
     * @param callbackContext   The callback context used when calling back into JavaScript.
     * @return                  True if the action was valid, false otherwise.
     */
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

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
        }
        return false;
    }

    /**
     * Gets all labels asynchronously and set result to callback context's as success.
     * It may get empty list if there are no visible labels or all are visible ones are marked as
     * deleted.
     * @throws JSONException
     */
    private void getLabels() throws JSONException {
        this.cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                List<QbixGroup> labels = groupAccessor.getAllLabels(this.cordova);
                if (labels != null) {
                    callbackContext.success(labels);
                } else {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, UNKNOWN_ERROR));
                }
            }
        });
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
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
