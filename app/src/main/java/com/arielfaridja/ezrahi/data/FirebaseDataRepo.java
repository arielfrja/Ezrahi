//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.data;

import static com.arielfaridja.ezrahi.entities.GlobalConsts.ACT_SP;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import androidx.room.Room;

import com.arielfaridja.ezrahi.Consts;
import com.arielfaridja.ezrahi.R;
import com.arielfaridja.ezrahi.entities.ActPermission;
import com.arielfaridja.ezrahi.entities.ActUser;
import com.arielfaridja.ezrahi.entities.Activity;
import com.arielfaridja.ezrahi.entities.Callback;
import com.arielfaridja.ezrahi.entities.Callback.Response;
import com.arielfaridja.ezrahi.entities.Latlng;
import com.arielfaridja.ezrahi.entities.Report;
import com.arielfaridja.ezrahi.entities.ReportStatus;
import com.arielfaridja.ezrahi.entities.ReportType;
import com.arielfaridja.ezrahi.entities.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FirebaseDataRepo implements IDataRepo {
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final CollectionReference activities;
    private final CollectionReference users;
    private final CollectionReference actUsers;
    private final CollectionReference reports;
    private final Context context;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    ListenerRegistration currentActivityUsersListener;
    AppDatabase localDb;
    ListenerRegistration currentUsersActivitiesListener;
    String TAG = "FirebaseDataRepo";
    Data data;
    private ListenerRegistration currentUsersListener;


    public FirebaseDataRepo(Context context) {
        //useFirebaseEmulators();//debug only!!
        this.context = context;
        localDb = Room.databaseBuilder(context, AppDatabase.class, "database").build();
        this.users = this.db.collection("Users");
        this.activities = this.db.collection("Activities");
        this.actUsers = this.db.collection("ActUsers");
        this.reports = this.db.collection("Reports");
        data = new Data(context);
        if (user_isSignedIn()) user_setCurrent(mAuth.getCurrentUser().getUid());
        activity_getAllByUser(mAuth.getUid(), response -> {
            data.currentUsersActivities.setValue(response.getActivities());
            if (!data.currentActivity.getId().isEmpty()) {
                setCurrentActivityUsersListener();
                setCurrentUsersListener();
            }
        });
    }

    private void saveActivityToSP(Activity activity) {
        SharedPreferences sp = context.getSharedPreferences(ACT_SP, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("id", activity.getId());
        editor.putString("Name", activity.getName());
        editor.putString("owner", activity.getOwner().getId());
        String routes = new Gson().toJson(activity.getRoutesSrc());
        editor.putString("routes", routes);
        editor.commit();

        //TODO: put activity's permissions and users to DB, not SP!
    }

    /***
     * A mathod to add a listener to the server's data about updates of this activity's users
     */
    void setCurrentActivityUsersListener() {
        currentActivityUsersListener = this.actUsers.whereEqualTo("ActId", data.currentActivity.getId()).addSnapshotListener((value, error) -> {
            if (error != null) try {
                throw error;
            } catch (FirebaseFirestoreException e) {
                e.printStackTrace();
            }
            else if (value != null) {
                for (DocumentChange doc : value.getDocumentChanges()) {
                    String userId = doc.getDocument().get("UserId").toString();
                    String actId = doc.getDocument().get("ActId").toString();
                    int permission = ((Long) doc.getDocument().get("Permission")).intValue();
                    switch (doc.getType()) {
                        case REMOVED:
                            data.currentActivityUsers.getValue().remove(userId);
                            currentUsersListener.remove();
                            setCurrentUsersListener();
                            break;
                        case ADDED: {
                            data.currentActivityUsers.getValue().put(userId, new ActUser(actId, userId, permission));
                            user_get(userId, response -> {
                                data.currentActivityUsers.getValue().get(userId).setUser(response.getUser());
                            });
                            if (currentUsersListener != null) currentUsersListener.remove();
                            setCurrentUsersListener();
                            break;
                        }
                        case MODIFIED:
                            data.currentActivityUsers.getValue().get(userId).setRole(permission);
                            //only permission is not read-only field.
                            break;
                    }
                }

            }
        });
    }

    /***
     * A mathod to add a listener to the server's data about updates of this user's acticties
     */
    void setCurrentUsersActivitiesListener() {
        currentUsersActivitiesListener = this.actUsers.whereEqualTo("UserId", mAuth.getCurrentUser().getUid()).addSnapshotListener((value, error) -> {
            if (error != null) try {
                throw error;
            } catch (FirebaseFirestoreException e) {
                e.printStackTrace();
            }
            else for (DocumentChange doc : value.getDocumentChanges()) {
                String actId = (String) doc.getDocument().get("ActId");
                switch (doc.getType()) {
                    case ADDED:
                        activity_get(actId, response -> {
                            data.currentUsersActivities.getValue().put(actId, response.getActivity());
                        });
                        break;

                    case REMOVED:
                        data.currentUsersActivities.getValue().remove(actId);
                        break;

                    case MODIFIED: //should not be happened
                        break;
                }
            }
        });
    }

    @Override
    public MutableLiveData<Activity> activity_get(String actId, Callback callback) {
        this.activities.document(actId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                User u;
                user_get((String) task.getResult().get("Owner"), new Callback() {
                    @Override
                    public void onResponse(Response response) {
                        callback.onResponse(new Response(docSnapshotToActivity(task.getResult(), response.getUser())));
                    }
                });
                //callback.onResponse(new Response(docSnapshotToActivity(task.getResult(), )));
            } else callback.onResponse(new Response(task.getException()));
        });
        return null;
    }

    @Override
    public MutableLiveData<HashMap<String, Activity>> activity_getAllByCurrentUser() {
        return data.currentUsersActivities;
    }

    @Override
    public void activity_getAllByUser(String uId, Callback callback) {
        actUsers.whereEqualTo("UserId", uId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                HashMap<String, Activity> activities = new HashMap<>();
                ArrayList<String> activitiesIds = new ArrayList<>();

                //Create Array, so we can use it to query from "Activities" collection.
                task.getResult().getDocuments().forEach(documentSnapshot -> {
                    activitiesIds.add((String) documentSnapshot.get("ActId"));
                });

                if (activitiesIds.size() > 0) {
                    this.activities.whereIn(FieldPath.documentId(), activitiesIds).get().addOnCompleteListener(task1 -> {
                        QuerySnapshot qs = task1.getResult();
                        if (task1.isSuccessful()) {
                            qs.getDocuments().forEach(docSnapshot -> user_get(docSnapshot.get("Owner").toString(), response -> {
                                activities.put(docSnapshot.getId(), docSnapshotToActivity(docSnapshot, response.getUser()));
                                if (activities.size() == activitiesIds.size())
                                    callback.onResponse(new Response<>(activities));
                            }));
                        }

                    });
                } else callback.onResponse(new Response(new HashMap<String, Activity>()));
            } else callback.onResponse(new Response<>(task.getException()));
        });
//        activities.whereNotEqualTo(field, null).get().addOnCompleteListener(task ->
//        {
//            if (task.isSuccessful()) {
//                ArrayList<Activity> activitiesList = new ArrayList<>();
//                HashMap<String, Activity> activities = new HashMap<>();
//                for (QueryDocumentSnapshot docRef : task.getResult()
//                ) {
//                    activitiesList.add(docSnapshotToActivity(docRef));
//                    activities.put(docRef.getId(), docSnapshotToActivity(docRef));
//                }
//                callback.onResponse(new Response(activitiesList));
//            } else
//                callback.onResponse(new Response(new Exception("we got into a porblem")));
//        });
    }

    @Override
    public Activity activity_getCurrent() {
        return data.currentActivity;
    }

    @Override
    public void activity_setCurrent(String actId) {
        activity_get(actId, response -> {
            if (response.getActivity() != null) {
                data.currentActivity = response.getActivity();
                saveActivityToSP(data.currentActivity);
                user_getAllByCurrentActivity();
            }
        });
    }

    public LiveData<User> auth_email_user_login(String email, String password, Callback callback) {
        this.mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener((task) -> {
            if (task.isSuccessful()) {
                if (task.getResult().getUser().isEmailVerified()) {
                    this.user_get(task.getResult().getUser().getUid(), response -> {
                        User u = response.getUser();
                        user_saveCurrentToSP(u);
                        callback.onResponse(response);
                    });
                    //this.user_get(task.getResult().getUser().getUid(), callback);
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

    private void user_saveCurrentToSP(User u) {
        SharedPreferences sp = context.getSharedPreferences(context.getString(R.string.current_user_pref), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("Email", u.getEmail());
        editor.putString("FirstName", u.getFirstName());
        editor.putString("LastName", u.getLastName());
        editor.putString("Uid", u.getId());
        editor.putString("Phone", u.getPhone());
        editor.putString("Email", u.getEmail());
        editor.apply();
    }

    public void auth_email_user_register(final User user, String password, Callback callback) {
        this.mAuth.createUserWithEmailAndPassword(user.getEmail(), password).addOnSuccessListener(authResult -> {
            user.setId(authResult.getUser().getUid());
            FirebaseDataRepo.this.user_add(user);
            FirebaseDataRepo.this.updateLocalUser(authResult.getUser());
            callback.onResponse(new Response(user));
        }).addOnFailureListener(e -> {
            Log.e(FirebaseDataRepo.this.TAG, "createUserWithEmail:failure", e);
            callback.onResponse(new Response(e));
        });
    }

    /***
     * a method do add user to database. user id defined automatically.
     * @param user
     * @return true if action success.
     */
    public Boolean user_add(User user) {
        final Boolean[] res = new Boolean[1];
        Map<String, Object> data = user.toHashMap();
        data.put("Location", new GeoPoint((double) data.get("Latitude"), (double) data.get("Longitude")));
        data.put("LastUpdate", Timestamp.now());
        data.remove("Latitude");
        data.remove("Longitude");
        data.remove("uId");
        //            public void onSuccess(DocumentReference documentReference) {
//                Log.d(FirebaseDataRepo.this.TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
//                res[0] = true;
//            }
        this.users.document(user.getId()).set(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.e(TAG, "user added successfully");
                res[0] = true;
            } else {
                Log.w(FirebaseDataRepo.this.TAG, "Error adding document", task.getException());
                res[0] = false;
            }
        }).addOnFailureListener(e -> {

        });
        return res[0];
    }

    public MutableLiveData<User> user_get(String uId, com.arielfaridja.ezrahi.entities.Callback callback) {
        this.users.document(uId).get().addOnCompleteListener((task) -> {
            if (task.isSuccessful()) {
                callback.onResponse(new Response(docSnapshotToUser(task.getResult())));
            } else {
                callback.onResponse(new Response(task.getException()));
            }
        });
        return null;
    }

    public JSONArray user_getAll() {
        return null;
    }

    @Override
    public HashMap<String, ActUser> user_getAllByActivity(String actId, Callback callback) {
        actUsers.whereEqualTo("ActId", actId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                HashMap<String, ActUser> users = new HashMap<>();
                for (QueryDocumentSnapshot docRef : task.getResult()) {
                    String userId = (String) docRef.get("UserId");
                    user_get(userId, response -> {
                        users.put(userId, new ActUser(actId, response.getUser(), ((Long) docRef.get("Permission")).intValue()));
                        if (users.size() == task.getResult().size())
                            callback.onResponse(new Response<>(users));
                    });
                }
            }


        });
        return null;

    }

    @Override
    public void report_getAllByActivity(String actId, Callback callback) {
        reports.whereEqualTo("ActId", actId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                HashMap<String, Report> reports = new HashMap<>();
                for (QueryDocumentSnapshot docRef : task.getResult()) {
                    String reporterId = (String) docRef.get("ReporterId");
                    Latlng location = new Latlng((GeoPoint) docRef.get("Location"));
                    Report r = new Report(actId,
                            data.currentActivityUsers.getValue().get(reporterId), (String) docRef.get("Title"),
                            (String) docRef.get("Description"),
                            location,
                            (Date) docRef.get("ReportTime"),
                            (ReportStatus) docRef.get("Status"),
                            (ReportType) docRef.get("Type")
                    );
                    reports.put(docRef.getId(), r);
                }
                data.currentActivityReports.setValue(reports);
            }
        });
    }

    @Override
    public void report_add(Report report, Callback callback) {
        HashMap data = new HashMap();
        data.put("ActId", report.getActId());
        data.put("ReporterId", report.getReporter().getId());
        data.put("Title", report.getTitle());
        data.put("Description", report.getDescription());
        data.put("Location", new GeoPoint(report.getLocation().getLatitude(), report.getLocation().getLongitude()));
        data.put("Time", new Timestamp(report.getReportTime()));
        data.put("Status", report.getReportStatus().getValue());
        data.put("Type", report.getReportType().getValue());
        reports.add(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onResponse(new Response<>(Consts.REPORT_ADD_SUCCESS + ":" + task.getResult().getId()));

            } else callback.onResponse(new Response(task.getException()));
        });
    }

    @Override
    public MutableLiveData<HashMap<String, ActUser>> user_getAllByCurrentActivity() {
        if (currentUsersListener == null)
            setCurrentUsersListener();
        return data.currentActivityUsers;
    }

    @Override
    public User user_getCurrent() {
        return data.currentUser;
    }

    public Latlng user_getLocation(String uId) {
        GeoPoint loc = (GeoPoint) this.users.document(uId).get().getResult().get("Location");
        return new Latlng(loc.getLatitude(), loc.getLongitude());
    }

    public Boolean user_isSignedIn() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().isEmailVerified();
        }
        return false;
    }

    public Boolean user_remove(String uId) {
        return null;
    }

    @Override
    public void user_setCurrent(String uId) {
        user_get(uId, response -> {
            if (response.getUser() != null) user_saveCurrentToSP(response.getUser());
            data.currentUser = response.getUser();
        });
    }

    public Boolean user_update(User user) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("Location", new GeoPoint(user.getLocation().getLatitude(), user.getLocation().getLongitude()));
        map.put("LastUpdate", Timestamp.now());
        users.document(user.getId()).update(map);
        return null;
    }

    private void updateLocalUser(FirebaseUser user) {
        if (user != null) {
        }

    }

    private Activity docSnapshotToActivity(DocumentSnapshot doc, User owner) {
        Activity act = new Activity();
        act.setId(doc.getId());
        act.setName((String) doc.get("Name"));
        act.setRoutesSrc((ArrayList) doc.get("Routes"));
        act.setOwner(owner);
        ArrayList<Map<String, Object>> permissions = ((ArrayList) doc.get("Permissions"));
        for (int i = 0; i < permissions.size(); i++) {
            act.getPermissions().add(ActPermission.mapToPermission(permissions.get(i), i));
            act.getPermissions().get(i).setActId(act.getId());
        }

//        for (String id : users.keySet()) {
//            user_get(id, response -> {
//                User u = response.getUser();
//                act.getUsers().add(new ActUser(doc.getId(), u, users.get(id).intValue()));
//            });
//        }
        return act;
    }

    @NonNull
    private User docSnapshotToUser(DocumentSnapshot snapshot) {
        return new User(snapshot.getId(), snapshot.get("FirstName").toString(), snapshot.get("LastName").toString(), snapshot.get("Phone").toString(), snapshot.get("Email").toString(), new Latlng(((GeoPoint) snapshot.get("Location"))));
    }

    void setCurrentUsersListener() {
        String[] usersIds = data.currentActivityUsers.getValue().keySet().toArray(new String[0]);
        if (usersIds.length > 0)
            currentUsersListener = this.users.whereIn(FieldPath.documentId(), Arrays.asList(usersIds)).addSnapshotListener((value, error) -> {
                if (error != null) try {
                    throw error;
                } catch (FirebaseFirestoreException e) {
                    e.printStackTrace();
                }
                else if (value != null) {
                    for (DocumentChange doc : value.getDocumentChanges()) {
                        User u = docSnapshotToUser(doc.getDocument());
                        switch (doc.getType()) {
                            case REMOVED:
                                data.currentActivityUsers.getValue().remove(u.getId());
                                //TODO: on cloud functions, to remove all its actUsers.
                                break;
                            case ADDED: {
                                //should not be happen, this block here to be sure about that:)
                                break;
                            }
                            case MODIFIED:
                                data.currentActivityUsers.getValue().get(u.getId()).setUser(u);
                                //update the user on local collection
                                break;
                        }
                    }

                }
            });
    }

    /***
     * debug only! to use firebase emulators instead of actual firebase abilities.
     */
    private void useFirebaseEmulators() {
        mAuth.useEmulator("10.0.0.4", 8080);
        db.useEmulator("10.0.0.4", 9099);
    }

    /***
     * class to gather all the data already pulled
     */
    class Data {
        MutableLiveData<HashMap<String, Report>> currentActivityReports = new MutableLiveData<HashMap<String, Report>>();
        User currentUser = new User();
        Activity currentActivity = new Activity();
        MutableLiveData<HashMap<String, ActUser>> currentActivityUsers = new MutableLiveData<>();
        MutableLiveData<HashMap<String, Activity>> currentUsersActivities = new MutableLiveData<>();

        public Data(Context context) {
            super();
            //TODO: get data from local database
            loadDataFromSP(context);
            String currentActId = PreferenceManager.getDefaultSharedPreferences(context).getString("currentActivity", "");
            if (context.getSharedPreferences(ACT_SP, Context.MODE_PRIVATE).getAll().isEmpty() && !currentActId.isEmpty()) {
                activity_get(currentActId, response -> {
                    saveActivityToSP(response.getActivity());
                });
            }
            currentActivityUsers.setValue(new HashMap<>());
        }

        /***
         * A method to load the user and activity already logged in to current application session
         * @param context context of current application
         */
        public void loadDataFromSP(Context context) {
            loadUserFromSP(context);

            SharedPreferences sp;
            sp = context.getSharedPreferences(ACT_SP, Context.MODE_PRIVATE);
            String actId = sp.getString("id", "");
            if (!actId.isEmpty()) {
                currentActivity.setId(actId);
                currentActivity.setName(sp.getString("name", ""));
                user_get(sp.getString("owner", ""), response -> {
                    currentActivity.setOwner(response.getUser());
                });
                // TODO: uncomment next line
                loadCurrentActivityUsers(sp);
            }
        }

        //load current user from SharedPreferences
        private void loadUserFromSP(Context context) {
            SharedPreferences sp = context.getSharedPreferences("UserSharedPref", Context.MODE_PRIVATE);
            if (sp != null) {
                currentUser.setId(sp.getString("Uid", ""));
                currentUser.setFirstName(sp.getString("FirstName", ""));
                currentUser.setLastName(sp.getString("LastName", ""));
//                currentUser.setLocation(new Latlng(
//                        Double.parseDouble(sp.getString("Latitude", "0.0")),
//                        Double.parseDouble(sp.getString("Longitude", "0.0"))));
                currentUser.setPhone(sp.getString("Phone", ""));
                currentUser.setEmail(sp.getString("Email", ""));
                if (mAuth.getUid() != null)
                    user_get(mAuth.getUid(), response -> {
                        currentUser = response.getUser();
                        user_saveCurrentToSP(currentUser);
                    });
            }
        }

        private void loadCurrentActivityUsers(SharedPreferences sp) {
            if (sp != null) ; //load from sp
            else user_getAllByCurrentActivity();
        }
    }


}
