//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.arielfaridja.ezrahi.entities.Callback;
import com.arielfaridja.ezrahi.entities.Latlng;
import com.arielfaridja.ezrahi.entities.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.json.JSONArray;

public class FirebaseDataRepo implements DataRepo {
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();


    User currentUser;
    String TAG = "FirebaseDataRepo";
    CollectionReference users;


    public FirebaseDataRepo() {
        initData(null);

        this.users = this.db.collection("Users");
    }

    @Override
    public void initData(Context context) {
        //for debug only!
        //useEmulators();
    }

    private void useEmulators() {
        String ip = "192.168.1.59";
        mAuth.useEmulator(ip,9099);
        db.useEmulator(ip,8080);
    }

    public Boolean user_add(User user) {
        final Boolean[] res = new Boolean[1];
        this.db.collection("users").add(user).addOnSuccessListener(documentReference -> {
            Log.d(FirebaseDataRepo.this.TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
            res[0] = true;
        }).addOnFailureListener(new OnFailureListener() {
            public void onFailure(@NonNull Exception e) {
                Log.w(FirebaseDataRepo.this.TAG, "Error adding document", e);
                res[0] = false;
            }
        });
        return res[0];
    }

    public Boolean user_remove(String uId) {
        return null;
    }

    public Boolean user_update(User user) {
        return null;
    }

    public MutableLiveData<User> user_get(String uId, Callback callback) {
        this.users.document(uId).get().addOnCompleteListener(
                (task) -> {
            if (task.isSuccessful()) {
                if(task.getResult().exists())
                    callback.onResponse(
                            new Callback.Response(
                                    new User(
                                            ((DocumentSnapshot) task.getResult()).getId(), ((DocumentSnapshot) task.getResult()).get("firstName").toString(), ((DocumentSnapshot) task.getResult()).get("lastName").toString(), ((DocumentSnapshot) task.getResult()).get("email").toString(), ((DocumentSnapshot) task.getResult()).get("phone").toString(), new Latlng(((GeoPoint) ((DocumentSnapshot) task.getResult()).get("location")).getLatitude(), ((GeoPoint) ((DocumentSnapshot) task.getResult()).get("location")).getLongitude()))));
                else
                    callback.onResponse(new Callback.Response(new Exception("user not exist")));
            } else {
                callback.onResponse(new Callback.Response(task.getException()));
            }

        });
        return null;
    }

    public JSONArray user_getAll() {
        return null;
    }

    public Latlng user_getLocation(String uId) {
        return null;
    }

    public void auth_email_user_register(final User user, String password) {
        this.mAuth.createUserWithEmailAndPassword(user.getEmail(), password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            public void onSuccess(AuthResult authResult) {
                user.setId(authResult.getUser().getUid());
                FirebaseDataRepo.this.user_add(user);
                FirebaseDataRepo.this.updateCurrentUser(authResult.getUser());
            }
        }).addOnFailureListener(new OnFailureListener() {
            public void onFailure(@NonNull Exception e) {
                Log.w(FirebaseDataRepo.this.TAG, "createUserWithEmail:failure", e);
            }
        });
    }

    public LiveData<User> auth_email_user_login(String email, String password, Callback callback) {
        this.mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener((task) -> {
            if (task.isSuccessful()) {
                this.user_get(((AuthResult) task.getResult()).getUser().getUid(), callback);
            } else {
                callback.onResponse(new Callback.Response(task.getException()));
            }

        });
        return null;
    }

    @Override
    public User currentUser_get(Callback callback) {
        if(this.mAuth.getCurrentUser()!=null)
            if(currentUser != null)
                callback.onResponse(new Callback.Response(currentUser));
            else
                user_get(this.mAuth.getCurrentUser().getUid(), response -> {
                    currentUser = response.getUser();
                    callback.onResponse(response);
                });
            else
                callback.onResponse(new Callback.Response());
        return currentUser;
    }

    @Override
    public Boolean currentUser_updateLocation(Latlng location) {
        currentUser.setLocation(location);
        //update in DB.
        return null;
    }

    private void updateCurrentUser(FirebaseUser user) {
        if (user != null) {
        }

    }
}
