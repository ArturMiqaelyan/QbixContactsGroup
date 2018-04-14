package com.qbix.qbixcontactgrouplib.models;

public class QbixEmail {
    /**
     * TYPES
     *
     * TYPE_CUSTOM - 0 (actual type in {@link QbixEmail#customType})
     * TYPE_HOME - 1
     * TYPE_WORK - 2
     * TYPE_OTHER - 3
     */

    String address;
    int type;
    String customType = null;
}
