package com.qbix.qbixcontactgrouplib.models;

import java.util.ArrayList;
import java.util.List;

public class QbixContact {


    String displayName;
    QbixName name;
    List<QbixOrganization> organizations;
    List<QbixAddress> addresses;
    List<QbixPhone> phones;
    List<QbixEmail> emails;
    List<QbixIm> ims;
    List<QbixWebsite> websites;
    List<QbixPhoto> photos;
    String note;
    String nickname;
    String birthday;

    public QbixContact() {
        displayName = "";
        name = new QbixName();
        organizations = new ArrayList<>();
        addresses = new ArrayList<>();
        phones = new ArrayList<>();
        emails = new ArrayList<>();
        ims = new ArrayList<>();
        websites = new ArrayList<>();
        photos =new ArrayList<>();
        note = "";
        nickname = "";
        birthday = "";
    }
}
