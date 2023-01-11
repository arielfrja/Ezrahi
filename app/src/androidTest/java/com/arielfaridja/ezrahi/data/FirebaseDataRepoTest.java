package com.arielfaridja.ezrahi.data;

import org.junit.Test;

public class FirebaseDataRepoTest {

    FirebaseDataRepo dataRepo = (FirebaseDataRepo) DataRepoFactory.getInstance();

    @Test
    public void testActivity_get() {
    }

    @Test
    public void testActivity_getAllByUser() {
        dataRepo.activity_getAllByUser("Chfo3OzfvVUoysjiqy68UHb0Bgv2",
                response -> {
                });
    }

    @Test
    public void testActivity_getCurrent() {
    }

    @Test
    public void testAuth_email_user_login() {
    }

    @Test
    public void testAuth_email_user_register() {
    }

    @Test
    public void testSetCurrentActivity() {
    }

    @Test
    public void testSetCurrentActivityUsers() {
    }

    @Test
    public void testUser_add() {
    }

    @Test
    public void testUser_get() {
    }

    @Test
    public void testUser_getAll() {
    }

    @Test
    public void testUser_getAllByActivity() {
    }

    @Test
    public void testUser_getLocation() {
    }

    @Test
    public void testUser_isSignedIn() {
    }

    @Test
    public void testUser_remove() {
    }

    @Test
    public void testUser_update() {
    }
}