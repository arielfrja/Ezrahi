//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.entities;

public class Response {
    private User user;
    private Activity activity;
    private Exception exception;

    public Response() {
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

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
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
}
