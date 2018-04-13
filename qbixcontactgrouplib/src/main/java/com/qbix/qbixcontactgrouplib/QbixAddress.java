package com.qbix.qbixcontactgrouplib;

public class QbixAddress {

    /**
     * TYPES
     *
     * TYPE_CUSTOM - 0 (actual type in {@link QbixAddress#customType})
     * TYPE_HOME - 1
     * TYPE_WORK - 2
     * TYPE_OTHER - 3
     */

    String formattedAddress;
    int type;
    String customType;
    String street;
    String pobox;   //Post Office Box number
    String neiborhood;
    String city;
    String region;
    String postcode;
    String country;
}
