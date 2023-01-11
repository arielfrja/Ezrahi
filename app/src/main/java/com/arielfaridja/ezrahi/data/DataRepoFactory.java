package com.arielfaridja.ezrahi.data;

import android.content.Context;

public class DataRepoFactory {
    static IDataRepo instance = null;

    private DataRepoFactory() {
    }

    public static IDataRepo getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseDataRepo(context);
        }

        return instance;
    }

    public static IDataRepo getInstance() {
        return instance;
    }
}