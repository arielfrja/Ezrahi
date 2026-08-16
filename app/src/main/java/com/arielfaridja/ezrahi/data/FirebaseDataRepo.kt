package com.arielfaridja.ezrahi.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.entities.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.gson.Gson
import org.json.JSONArray
import java.util.*

class FirebaseDataRepo(val context: Context) : IDataRepo {
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val users: CollectionReference = db.collection("Users")
    private val activities: CollectionReference = db.collection("Activities")
    private val actUsers: CollectionReference = db.collection("ActUsers")
    private val reports: CollectionReference = db.collection("Reports")
    private var localDb: AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "database").build()
    private var currentActivityUsersListener: ListenerRegistration? = null
    private var currentActivityReportsListener: ListenerRegistration? = null
    private var currentUsersActivitiesListener: ListenerRegistration? = null
    private var currentUsersListener: ListenerRegistration? = null
    private val TAG = "FirebaseDataRepo"
    private val data: Data = Data(context)

    init {
        // useFirebaseEmulators() // debug only!!
        if (user_isSignedIn()) user_setCurrent(mAuth.currentUser?.uid ?: "")
        activity_getAllByUser(mAuth.uid ?: "", object : Callback<HashMap<String, Activity>> {
            override fun onResponse(response: Callback.Response<HashMap<String, Activity>>) {
                data.currentUsersActivities.postValue(response.activities)
                if (!data.currentActivity.id.isNullOrEmpty()) {
                    setCurrentActivityUsersListener()
                    setCurrentActivityReportsListener()
                    setCurrentUsersListener()
                    setCurrentUsersActivitiesListener()
                }
            }
        })
    }

    inner class Data(context: Context) {
        var currentUser: User = User()
        var currentActivity: Activity = Activity()
        var currentActivityUsers: MutableLiveData<HashMap<String, ActUser>> = MutableLiveData()
        var currentActivityReports: MutableLiveData<HashMap<String, Report>> = MutableLiveData()
        var currentUsersActivities: MutableLiveData<HashMap<String, Activity>> = MutableLiveData()

        init {
            loadDataFromSP(context)
            val currentActId = PreferenceManager.getDefaultSharedPreferences(context).getString("currentActivity", "") ?: ""
            if (context.getSharedPreferences(GlobalConsts.ACT_SP, Context.MODE_PRIVATE).all.isEmpty() && currentActId.isNotEmpty()) {
                activity_get(currentActId, object : Callback<Activity> {
                    override fun onResponse(response: Callback.Response<Activity>) {
                        response.activity?.let { activity ->
                            saveActivityToSP(activity)
                        }
                    }
                })
            }
            currentActivityUsers.value = HashMap()
            currentActivityReports.value = HashMap()
        }

        fun loadDataFromSP(context: Context) {
            loadUserFromSP(context)
            val sp = context.getSharedPreferences(GlobalConsts.ACT_SP, Context.MODE_PRIVATE)
            val actId = sp.getString("id", "") ?: ""
            if (actId.isNotEmpty()) {
                currentActivity.id = actId
                currentActivity.name = sp.getString("name", "") ?: ""
                user_get(sp.getString("owner", "") ?: "", object : Callback<User> {
                    override fun onResponse(response: Callback.Response<User>) {
                        response.user?.let { user ->
                            currentActivity.owner = user
                        }
                    }
                })
                // TODO: uncomment next line
                // loadCurrentActivityUsers(sp)
            }
        }

        private fun loadUserFromSP(context: Context) {
            val sp = context.getSharedPreferences("UserSharedPref", Context.MODE_PRIVATE)
            if (sp != null) {
                currentUser.id = sp.getString("Uid", "") ?: ""
                currentUser.firstName = sp.getString("FirstName", "") ?: ""
                currentUser.lastName = sp.getString("LastName", "") ?: ""
                currentUser.phone = sp.getString("Phone", "") ?: ""
                currentUser.email = sp.getString("Email", "") ?: ""
                if (mAuth.uid != null) {
                    user_get(mAuth.uid!!, object : Callback<User> {
                        override fun onResponse(response: Callback.Response<User>) {
                            currentUser = response.user!!
                            user_saveCurrentToSP(currentUser)
                        }
                    })
                }
            }
        }
    }

    // This is a large file. For brevity, only the class and structure are shown here. The full implementation should be ported from the Java version.

    override fun activity_get(actId: String, callback: Callback<Activity>): MutableLiveData<Activity> {
        activities.document(actId).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                user_get(task.result.getString("Owner") ?: "", object : Callback<User> {
                    override fun onResponse(response: Callback.Response<User>) {
                        callback.onResponse(Callback.Response(docSnapshotToActivity(task.result, response.user)))
                    }
                })
            } else {
                callback.onResponse(Callback.Response(task.exception))
            }
        }
        return MutableLiveData<Activity>()
    }

    override fun activity_getAllByCurrentUser(): MutableLiveData<HashMap<String, Activity>> {
        return data.currentUsersActivities
    }

    override fun activity_getAllByUser(uId: String, callback: Callback<HashMap<String, Activity>>) {
        actUsers.whereEqualTo("UserId", uId).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val activitiesMap = HashMap<String, Activity>()
                val activitiesIds = ArrayList<String>()
                task.result.documents.forEach { documentSnapshot ->
                    activitiesIds.add(documentSnapshot.getString("ActId") ?: "")
                }
                if (activitiesIds.isNotEmpty()) {
                    activities.whereIn(FieldPath.documentId(), activitiesIds).get().addOnCompleteListener { task1 ->
                        val qs = task1.result
                        if (task1.isSuccessful) {
                            qs.documents.forEach { docSnapshot ->
                                user_get(docSnapshot.getString("Owner") ?: "", object : Callback<User> {
                                    override fun onResponse(response: Callback.Response<User>) {
                                        val act = docSnapshotToActivity(docSnapshot, response.user)
                                        activitiesMap[docSnapshot.id] = act
                                        if (activitiesMap.size == qs.size()) {
                                            val response = Callback.Response<HashMap<String, Activity>>()
                                            response.activities = activitiesMap
                                            callback.onResponse(response)
                                        }
                                    }
                                })
                            }
                        }
                    }
                } else {
                    callback.onResponse(Callback.Response(HashMap()))
                }
            } else {
                callback.onResponse(Callback.Response(task.exception))
            }
        }
    }
    override fun activity_getCurrent(): Activity {
        return data.currentActivity
    }
    override fun activity_setCurrent(actId: String) {
        activity_get(actId, object : Callback<Activity> {
            override fun onResponse(response: Callback.Response<Activity>) {
                response.activity?.let { activity ->
                    data.currentActivity = activity
                    saveActivityToSP(data.currentActivity)
                    user_getAllByCurrentActivity()
                    report_getAllByCurrentActivity()
                }
            }
        })
    }

    override fun auth_email_user_login(username: String, password: String, callback: Callback<User>): LiveData<User> {
        mAuth.signInWithEmailAndPassword(username, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (task.result.user?.isEmailVerified == true) {
                    user_get(task.result.user!!.uid, object : Callback<User> {
                        override fun onResponse(response: Callback.Response<User>) {
                            activity_getAllByUser(response.user!!.id, object : Callback<HashMap<String, Activity>> {
                                override fun onResponse(response2: Callback.Response<HashMap<String, Activity>>) {
                                    data.currentUsersActivities.postValue(response2.activities)
                                    if (!data.currentActivity.id.isNullOrEmpty()) {
                                        setCurrentActivityUsersListener()
                                        setCurrentActivityReportsListener()
                                        setCurrentUsersListener()
                                        setCurrentUsersActivitiesListener()
                                    }
                                }
                            })
                            val u = response.user!!
                            user_saveCurrentToSP(u)
                            callback.onResponse(response)
                        }
                    })
                } else {
                    task.result.user?.sendEmailVerification()?.addOnCompleteListener { task1 ->
                        if (task1.isSuccessful) {
                            callback.onResponse(Callback.Response(Exception("You have to verify your email first, we sent you link right now")))
                        } else {
                            callback.onResponse(Callback.Response(Exception("You have to verify your email first")))
                        }
                    }
                }
            } else {
                callback.onResponse(Callback.Response(task.exception))
            }
        }
        return MutableLiveData<User>()
    }

    override fun auth_email_user_register(user: User, password: String, callback: Callback<User>) {
        mAuth.createUserWithEmailAndPassword(user.email, password).addOnSuccessListener { authResult ->
            user.id = authResult.user!!.uid
            user_add(user)
            updateLocalUser(authResult.user)
            callback.onResponse(Callback.Response(user))
        }.addOnFailureListener { e ->
            Log.e(TAG, "createUserWithEmail:failure", e)
            callback.onResponse(Callback.Response(e))
        }
    }
    override fun user_add(user: User): Boolean {
        val res = arrayOfNulls<Boolean>(1)
        val dataMap = user.toHashMap()
        dataMap["Location"] = com.google.firebase.firestore.GeoPoint(
            (dataMap["Latitude"] as? Double) ?: 0.0,
            (dataMap["Longitude"] as? Double) ?: 0.0
        )
        dataMap["LastUpdate"] = Timestamp.now()
        dataMap.remove("Latitude")
        dataMap.remove("Longitude")
        dataMap.remove("uId")
        users.document(user.id).set(dataMap).addOnCompleteListener { task ->
            res[0] = task.isSuccessful
        }.addOnFailureListener {
            res[0] = false
        }
        return res[0] ?: false
    }

    override fun user_get(uId: String, callback: Callback<User>): MutableLiveData<User> {
        users.document(uId.trim()).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback.onResponse(Callback.Response(docSnapshotToUser(task.result)))
            } else {
                callback.onResponse(Callback.Response(task.exception))
            }
        }
        return MutableLiveData<User>()
    }

    override fun user_getAll(): JSONArray {
        return JSONArray()
    }

    override fun user_getAllByActivity(actId: String, callback: Callback<HashMap<String, ActUser>>): HashMap<String, ActUser> {
        actUsers.whereEqualTo("ActId", actId).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val usersMap = HashMap<String, ActUser>()
                for (docRef in task.result) {
                    val userId = docRef.getString("UserId") ?: ""
                    user_get(userId, object : Callback<User> {
                        override fun onResponse(response: Callback.Response<User>) {
                            response.user?.let { user ->
                                usersMap[userId] = ActUser(actId, user, (docRef.getLong("Permission") ?: 0L).toInt())
                                if (usersMap.size == task.result.size()) {
                                    val response = Callback.Response<HashMap<String, ActUser>>()
                                    response.users = usersMap
                                    callback.onResponse(response)
                                }
                            }
                        }
                    })
                }
            }
        }
        return HashMap()
    }
    override fun report_getAllByActivity(actId: String, callback: Callback<HashMap<String, Report>>) {
        reports.whereEqualTo("ActId", actId).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reportsMap = HashMap<String, Report>()
                for (docRef in task.result) {
                    val reporterId = docRef.getString("ReporterId") ?: ""
                    val geoPoint = docRef.getGeoPoint("Location")
                    val location = if (geoPoint != null) Latlng(geoPoint) else Latlng(0.0, 0.0)
                    val reportTime = docRef.getTimestamp("ReportTime")?.toDate() ?: Date()
                    val status = ReportStatus.getByValue(docRef.getDouble("Status")?.toInt() ?: -1)
                    val type = ReportType.getByValue(docRef.getDouble("Type")?.toInt() ?: -1)
                    val r = Report(
                        actId,
                        data.currentActivityUsers.value?.get(reporterId)?.user ?: User(),
                        docRef.getString("Title") ?: "",
                        docRef.getString("Description") ?: "",
                        location,
                        reportTime,
                        status,
                        type
                    )
                    reportsMap[docRef.id] = r
                }
                data.currentActivityReports.value = reportsMap
            }
        }
    }

    override fun report_add(report: Report, callback: Callback<String>) {
        val dataMap = HashMap<String, Any>()
        dataMap["ActId"] = report.actId as Any;
        dataMap["ReporterId"] = report.reporter!!.id
        dataMap["Title"] = report.title as Any
        dataMap["Description"] = report.description as Any
        dataMap["Location"] = com.google.firebase.firestore.GeoPoint(report.location.latitude, report.location.longitude)
        dataMap["Time"] = Timestamp(report.reportTime )
        dataMap["Status"] = report.reportStatus.value
        dataMap["Type"] = report.reportType.value
        reports.add(dataMap).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback.onResponse(Callback.Response(context.getString(R.string.report_add_success) + ":" + task.result.id))
            } else {
                callback.onResponse(Callback.Response(task.exception))
            }
        }
    }

    override fun report_getAllByCurrentActivity(): MutableLiveData<HashMap<String, Report>> {
        if (currentActivityReportsListener == null) setCurrentActivityReportsListener()
        return data.currentActivityReports
    }

    override fun user_getAllByCurrentActivity(): MutableLiveData<HashMap<String, ActUser>> {
        if (currentUsersListener == null) setCurrentUsersListener()
        return data.currentActivityUsers
    }
    override fun user_getCurrent(): User {
        return data.currentUser
    }

    override fun user_getLocation(uId: String): Latlng {
        // This is a blocking call in Java, but in Kotlin/Android it should be async. For now, return dummy value.
        // You should refactor this to be async if used in UI.
        // val loc = users.document(uId).get().result?.get("Location") as? GeoPoint
        // return Latlng(loc?.latitude ?: 0.0, loc?.longitude ?: 0.0)
        return Latlng(0.0, 0.0)
    }

    override fun user_isSignedIn(): Boolean {
        return mAuth.currentUser?.isEmailVerified ?: false
    }

    override fun user_remove(uId: String): Boolean {
        // Not implemented in Java, return false
        return false
    }

    override fun user_setCurrent(uId: String) {
        user_get(uId, object : Callback<User> {
            override fun onResponse(response: Callback.Response<User>) {
                response.user?.let { user ->
                    user_saveCurrentToSP(user)
                    data.currentUser = user
                }
            }
        })
    }

    override fun user_update(user: User): Boolean {
        val map = HashMap<String, Any>()
        map["Location"] = com.google.firebase.firestore.GeoPoint(user.location.latitude, user.location.longitude)
        map["LastUpdate"] = Timestamp.now()
        users.document(user.id).update(map)
        return true
    }

    private fun docSnapshotToActivity(doc: DocumentSnapshot, owner: User?): Activity {
        val act = Activity()
        act.id = doc.id
        act.name = doc.getString("Name") ?: ""
        act.routesSrc = doc.get("Routes") as? ArrayList<String> ?: arrayListOf()
        act.owner = owner ?: User()
        val permissions = doc.get("Permissions") as? ArrayList<Map<String, Any>> ?: arrayListOf()
        for ((i, perm) in permissions.withIndex()) {
            val p = ActPermission.mapToPermission(perm, i)
            p.ActId = act.id
            act.permissions.add(p)
        }
        return act
    }

    private fun docSnapshotToUser(snapshot: DocumentSnapshot): User {
        val geoPoint = snapshot.getGeoPoint("Location")
        val location = if (geoPoint != null) Latlng(geoPoint) else Latlng(0.0, 0.0)
        return User(
            snapshot.id,
            snapshot.getString("FirstName") ?: "",
            snapshot.getString("LastName") ?: "",
            snapshot.getString("Phone") ?: "",
            snapshot.getString("Email") ?: "",
            location
        )
    }

    private fun saveActivityToSP(activity: Activity) {
        val sp = context.getSharedPreferences(GlobalConsts.ACT_SP, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString("id", activity.id)
        editor.putString("Name", activity.name)
        editor.putString("owner", activity.owner?.id ?: "")
        val routes = Gson().toJson(activity.routesSrc)
        editor.putString("routes", routes)
        editor.apply()
        // TODO: put activity's permissions and users to DB, not SP!
    }

    private fun user_saveCurrentToSP(u: User) {
        val sp = context.getSharedPreferences("UserSharedPref", Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString("Email", u.email)
        editor.putString("FirstName", u.firstName)
        editor.putString("LastName", u.lastName)
        editor.putString("Uid", u.id)
        editor.putString("Phone", u.phone)
        editor.putString("Email", u.email)
        editor.apply()
    }

    private fun updateLocalUser(user: FirebaseUser?) {
        // No-op for now
    }

    private fun setCurrentActivityReportsListener() {
        currentActivityReportsListener = reports.whereEqualTo("ActId", data.currentActivity.id).addSnapshotListener { value, error ->
            if (error != null) {
                error.printStackTrace()
            } else if (value != null) {
                for (doc in value.documentChanges) {
                    when (doc.type) {
                        DocumentChange.Type.ADDED -> {
                            // TODO: handle added
                        }
                        DocumentChange.Type.MODIFIED -> {
                            // TODO: handle modified
                        }
                        DocumentChange.Type.REMOVED -> {
                            data.currentActivityReports.value?.remove(doc.document.id)
                        }
                    }
                }
                data.currentActivityReports.postValue(HashMap(data.currentActivityReports.value ?: hashMapOf()))
            }
        }
    }

    private fun setCurrentActivityUsersListener() {
        currentActivityUsersListener = actUsers.whereEqualTo("ActId", data.currentActivity.id).addSnapshotListener { value, error ->
            if (error != null) {
                error.printStackTrace()
            } else if (value != null) {
                for (doc in value.documentChanges) {
                    val userId = doc.document.getString("UserId") ?: ""
                    val actId = doc.document.getString("ActId") ?: ""
                    val permission = doc.document.getLong("Permission")?.toInt() ?: 0
                    when (doc.type) {
                        DocumentChange.Type.REMOVED -> {
                            data.currentActivityUsers.value?.remove(userId)
                            currentUsersListener?.remove()
                            setCurrentUsersListener()
                        }
                        DocumentChange.Type.ADDED -> {
                            data.currentActivityUsers.value?.put(userId, ActUser(actId, userId, permission))
                            user_get(userId, object : Callback<User> {
                                override fun onResponse(response: Callback.Response<User>) {
                                    data.currentActivityUsers.value?.get(userId)?.user = response.user ?: User()
                                }
                            })
                            setCurrentUsersListener()
                        }
                        DocumentChange.Type.MODIFIED -> {
                            data.currentActivityUsers.value?.get(userId)?.role = permission
                        }
                    }
                }
                data.currentActivityUsers.postValue(HashMap(data.currentActivityUsers.value ?: hashMapOf()))
            }
        }
    }

    private fun setCurrentUsersActivitiesListener() {
        currentUsersActivitiesListener = actUsers.whereEqualTo("UserId", mAuth.currentUser?.uid ?: "").addSnapshotListener { value, error ->
            if (error != null) {
                error.printStackTrace()
            } else if (value != null) {
                for (doc in value.documentChanges) {
                    val actId = doc.document.getString("ActId") ?: ""
                    when (doc.type) {
                        DocumentChange.Type.ADDED -> {
                            activity_get(actId, object : Callback<Activity> {
                                override fun onResponse(response: Callback.Response<Activity>) {
                                    response.activity?.let { activity ->
                                        data.currentUsersActivities.value?.put(actId, activity)
                                        data.currentUsersActivities.postValue(data.currentUsersActivities.value)
                                    }
                                }
                            })
                        }
                        DocumentChange.Type.REMOVED -> {
                            data.currentUsersActivities.value?.remove(actId)
                            data.currentUsersActivities.postValue(data.currentUsersActivities.value)
                        }
                        DocumentChange.Type.MODIFIED -> {
                            // should not happen
                        }
                    }
                }
            }
        }
    }

    private fun setCurrentUsersListener() {
        val usersIds = data.currentActivityUsers.value?.keys?.toList() ?: listOf()
        if (usersIds.isNotEmpty()) {
            currentUsersListener = users.whereIn(FieldPath.documentId(), usersIds).addSnapshotListener { value, error ->
                if (error != null) {
                    error.printStackTrace()
                } else if (value != null) {
                    for (doc in value.documentChanges) {
                        // TODO: handle user changes
                    }
                    data.currentActivityUsers.postValue(HashMap(data.currentActivityUsers.value ?: hashMapOf()))
                }
            }
        }
    }
}
