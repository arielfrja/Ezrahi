//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.arielfaridja.ezrahi.entities.Activity;
import com.arielfaridja.ezrahi.entities.Callback;
import com.arielfaridja.ezrahi.entities.Callback.Response;
import com.arielfaridja.ezrahi.entities.Latlng;
import com.arielfaridja.ezrahi.entities.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.json.JSONArray;

import java.util.Map;

public class FirebaseDataRepo implements DataRepo {
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference Activities;
    private final CollectionReference users;
    String TAG = "FirebaseDataRepo";

    public FirebaseDataRepo() {
        //useFirebaseEmulators();//debug only!!
        this.users = this.db.collection("Users");
        this.Activities = this.db.collection("Activities");
    }

    private void useFirebaseEmulators() {
        mAuth.useEmulator("10.0.0.4", 8080);
        db.useEmulator("10.0.0.4", 9099);
    }

    public Boolean user_add(User user) {
        final Boolean[] res = new Boolean[1];
//        Map<String, Object> data = new HashMap<>();
//        data.put("FirstName", user.getFirstName());
//        data.put("LastName", user.getLastName());
//        data.put("Email", user.getEmail());
//        data.put("Phone", user.getPhone());
//        data.put("LastUpdate", Timestamp.now());
        Map<String, Object> data = user.toHashMap();
        data.put("Location", new GeoPoint((double) data.get("Latitude"), (double) data.get("Longitude")));
        data.remove("Latitude");
        data.remove("Longitude");
        //            public void onSuccess(DocumentReference documentReference) {
//                Log.d(FirebaseDataRepo.this.TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
//                res[0] = true;
//            }
        this.users.document(user.getId()).set(data).addOnSuccessListener((OnSuccessListener) o -> res[0] = true).addOnFailureListener(e -> {
            Log.w(FirebaseDataRepo.this.TAG, "Error adding document", e);
            res[0] = false;
        });
        return res[0];
    }

    public Boolean user_remove(String uId) {
        return null;
    }

    public Boolean user_update(User user) {
        return null;
    }

    public MutableLiveData<User> user_get(String uId, com.arielfaridja.ezrahi.entities.Callback callback) {
        this.users.document(uId).get().addOnCompleteListener((task) -> {
            if (task.isSuccessful()) {
                callback.onResponse(new Response(new User(task.getResult().getId(),
                        task.getResult().get("FirstName").toString(),
                        task.getResult().get("LastName").toString(),
                        task.getResult().get("Email").toString(),
                        task.getResult().get("Phone").toString(),
                        new Latlng(((GeoPoint) task.getResult().get("Location")).getLatitude(),
                                ((GeoPoint) task.getResult().get("Location")).getLongitude()))));
            } else {
                callback.onResponse(new Response(task.getException()));
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

    public void auth_email_user_register(final User user, String password, Callback callback) {
        this.mAuth.createUserWithEmailAndPassword(user.getEmail(), password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            public void onSuccess(AuthResult authResult) {
                user.setId(authResult.getUser().getUid());
                FirebaseDataRepo.this.user_add(user);
                FirebaseDataRepo.this.updateLocalUser(authResult.getUser());
                callback.onResponse(new Response(user));
            }
        }).addOnFailureListener(new OnFailureListener() {
            public void onFailure(@NonNull Exception e) {
                Log.e(FirebaseDataRepo.this.TAG, "createUserWithEmail:failure", e);
                callback.onResponse(new Response(e));
            }
        });
    }

    public LiveData<User> auth_email_user_login(String email, String password, Callback callback) {
        this.mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener((task) -> {
            if (task.isSuccessful()) {
                if (task.getResult().getUser().isEmailVerified()) {
                    this.user_get(task.getResult().getUser().getUid(), callback);
                } else {
                    task.getResult().getUser().sendEmailVerification().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful())
                            callback.onResponse(new Response(new Exception("You have to verify your email first, we sent you link right now")));
                        else
                            callback.onResponse(new Response(new Exception("You have to verify your email first")));
                    });
                }
            } else {
                callback.onResponse(new Response(task.getException()));
            }

        });
        return null;
    }

    @Override
    public MutableLiveData<Activity> activity_get(String actId, Callback callback) {
        return null;
    }

    @Override
    public Activity activity_getCurrent() {
        return null;
    }


    private void updateLocalUser(FirebaseUser user) {
        if (user != null) {
        }

    }

    public Boolean user_isSignedIn() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().isEmailVerified();
        }
        return false;
    }
}
