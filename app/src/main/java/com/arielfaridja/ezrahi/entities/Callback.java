//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.entities;

import java.util.ArrayList;
import java.util.HashMap;

public interface Callback {
    void onResponse(Response response);

    class Response<T> {
        private HashMap<String, ActUser> users;
        private HashMap<String, Activity> activities;
        private ArrayList<Activity> activitiesList;
        private String message;
        private User user;
        private Activity activity;
        private Exception exception;

        public Response(ArrayList<Activity> activities) {
            this.activitiesList = activities;
        }

        public Response() {
        }

        public Response(HashMap<String, T> items) {
            activities = new HashMap<>();
            users = new HashMap<>();
            items.forEach((s, o) -> {
                if (o instanceof ActUser) this.users.put(s, (ActUser) o);
                if (o instanceof Activity) this.activities.put(s, (Activity) o);
            });
        }


        public Response(String message) {
            this.message = message;
        }

        public Response(User user) {
            this.user = user;
        }

        public Response(Activity activity) {
            this.activity = activity;
        }


        public Response(Exception exception) {
            this.exception = exception;
        }

        public HashMap<String, Activity> getActivities() {
            return activities;
        }

        public void setActivities(HashMap<String, Activity> activities) {
            this.activities = activities;
        }

        public ArrayList<Activity> getActivitiesList() {
            return activitiesList;
        }

        public void setActivitiesList(ArrayList<Activity> activitiesList) {
            this.activitiesList = activitiesList;
        }

        public Activity getActivity() {
            return this.activity;
        }

        public void setActivity(Activity activity) {
            this.activity = activity;
        }

        public Exception getException() {
            return this.exception;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public User getUser() {
            if (user != null) return this.user;
            return null;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public HashMap<String, ActUser> getUsers() {
            return users;
        }

        public void setUsers(HashMap<String, ActUser> users) {
            this.users = users;
        }
    }


}
