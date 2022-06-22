package com.arielfaridja.ezrahi.data;

public class DataRepoFactory {
    static DataRepo instance = null;

    private DataRepoFactory() {
    }

    public static DataRepo getInstance() {
        if (instance == null) {
            instance = new FirebaseDataRepo();
        }

        return instance;
    }
}