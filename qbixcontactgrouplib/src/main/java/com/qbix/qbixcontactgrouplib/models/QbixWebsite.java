package com.qbix.qbixcontactgrouplib.models;

public class QbixWebsite {
    /**
     * TYPES
     *
     * TYPE_CUSTOM - 0 (actual type in {@link QbixWebsite#customType})
     * TYPE_HOMEPAGE - 1
     * TYPE_BLOG - 2
     * TYPE_PROFILE - 3
     * TYPE_HOME - 4
     * TYPE_WORK - 5
     * TYPE_FTP - 6
     * TYPE_OTHER - 7
     */

    String url;
    int type;
    String customType;
}
