package com.arielfaridja.ezrahi.UI

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.UI.Login.LoginActivity
import com.arielfaridja.ezrahi.UI.Main.MainActivity
import com.arielfaridja.ezrahi.data.DataRepoFactory
import com.arielfaridja.ezrahi.data.IDataRepo
import com.arielfaridja.ezrahi.entities.Callback
import com.arielfaridja.ezrahi.entities.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class StartupActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private val IDataRepo: IDataRepo by lazy { DataRepoFactory.getInstance(applicationContext) }
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = getSharedPreferences("UserSharedPref", MODE_PRIVATE)
        mAuth = FirebaseAuth.getInstance()
    }

    override fun onStart() {
        super.onStart()
        val currentUser: FirebaseUser? = mAuth.currentUser
        if (currentUser != null) {
            IDataRepo.user_get(currentUser.uid, object : Callback<User> {
                override fun onResponse(response: Callback.Response<User>) {
                    val u: User? = response.user
                    val intent = Intent(this@StartupActivity, MainActivity::class.java)
                    intent.putExtra("firstName", u?.firstName)
                    intent.putExtra("lastName", u?.lastName)
                    intent.putExtra("id", u?.id)
                    intent.putExtra("phone", u?.phone)
                    intent.putExtra("email", u?.email)
                    intent.putExtra("latitude", u?.location?.latitude)
                    intent.putExtra("longitude", u?.location?.longitude)
                    startActivity(intent)
                }
            })
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
