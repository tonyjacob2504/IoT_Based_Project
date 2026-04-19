package com.aumento.blindstick.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class GlobalPreference {

    private SharedPreferences prefs;
    private Context context;
    SharedPreferences.Editor editor;

    public GlobalPreference(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(Constants.SHARED_PREF, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void addIP(String ip) {
        editor.putString(Constants.IP, ip);
        editor.apply();
    }

    public String RetriveIP() {
        return prefs.getString(Constants.IP, "");
    }

    public void addUID(String uid) {
        editor.putString(Constants.UID, uid);
        editor.apply();
    }

    public String RetriveUID() {
        return prefs.getString(Constants.UID, "");
    }

    public void addName(String name) {
        editor.putString(Constants.NAME, name);
        editor.apply();
    }

    public String RetriveName() {
        return prefs.getString(Constants.NAME, "");
    }

    public void addEmgContact(String contact) {
        editor.putString(Constants.CONTACT, contact);
        editor.apply();
    }

    public String RetriveEmgContact() {
        return prefs.getString(Constants.CONTACT, "");
    }

}
