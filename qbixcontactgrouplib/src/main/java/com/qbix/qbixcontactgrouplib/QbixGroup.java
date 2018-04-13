package com.qbix.qbixcontactgrouplib;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class QbixGroup {

    String sourceId;
    String title;
    String notes;
    int summaryCount;
    boolean isVisible;
    boolean isDeleted;
    boolean shouldSync;
    boolean readOnly;
    List<Integer> contactIds;

    public JSONObject toJson() {
        try {
            JSONObject jsonGroup = new JSONObject();
            jsonGroup.put("sourceId", sourceId);
            jsonGroup.put("notes", notes);
            jsonGroup.put("summaryCount", summaryCount);
            jsonGroup.put("isVisible", isVisible);
            jsonGroup.put("isDeleted", isDeleted);
            jsonGroup.put("shouldSync", shouldSync);
            jsonGroup.put("readOnly", readOnly);
            JSONArray jsonContactIds = new JSONArray();
            for (int i = 0; i <contactIds.size(); i++) {
                JSONObject contactId = new JSONObject();
                contactId.put("contactId",contactIds.get(i));
                jsonContactIds.put(contactId);
            }
            jsonGroup.put("contactIds",jsonContactIds);
            return jsonGroup;
        } catch (Exception e) {
            return null;
        }
    }
}
