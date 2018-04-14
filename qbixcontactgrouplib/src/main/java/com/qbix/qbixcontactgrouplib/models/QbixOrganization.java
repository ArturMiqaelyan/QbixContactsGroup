package com.qbix.qbixcontactgrouplib.models;

public class QbixOrganization {

    /**
     * TYPES
     *
     * TYPE_CUSTOM - 0 (actual type in {@link QbixOrganization#customType})
     * TYPE_WORK - 1
     * TYPE_OTHER - 2
     *
     * PHONETIC_NAME_STYLE
     * JAPANESE - 4
     * KOREAN - 5
     * PINYIN - 3
     * UNDEFINED - 0
     */

    String company;
    int type;
    String customType;
    String title;
    String department;
    String jobDescription;
    String symbol;
    String phoneticName;
    String officeLocation;
    String phoneticNameStyle;
}
