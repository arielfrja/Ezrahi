package com.arielfaridja.ezrahi.UI.Main

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import com.arielfaridja.ezrahi.LocationTrackingService
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.UI.Login.LoginActivity
import com.arielfaridja.ezrahi.data.DataRepoFactory
import com.arielfaridja.ezrahi.entities.Activity
import com.arielfaridja.ezrahi.entities.User
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), View.OnFocusChangeListener {
    var user: User? = null
    var currentActivity: Activity? = null
    lateinit var navController: NavController
    lateinit var sharedPreferences: SharedPreferences
    var isReady: Boolean = false
    private lateinit var toolbar: Toolbar
    private lateinit var searchView: SearchView
    private lateinit var navView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var model: MainActivityViewModel
    private var doubleBackToExitPressedOnce = false

    companion object {
        private var isVisible = false
        fun getIsVisible(): Boolean = isVisible
        fun setIsVisible(visible: Boolean) { isVisible = visible }
    }

    fun getIsReady(): Boolean = isReady
    fun setIsReady(isSignedIn: Boolean) { isReady = isSignedIn }
    fun getCurrentUser(): User? = model.getUser()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        requestLocationPermissionsIfNeeded()
        initApp()
        splashScreen.setKeepOnScreenCondition { !isReady }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val thisActivity = this
        initModel(thisActivity)
        initContentViews()
        // הפעלת השירות רק אם יש הרשאות
        if (hasAllLocationPermissions()) {
            startLocationService()
        }
    }

    private fun requestLocationPermissionsIfNeeded() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
        val notGranted = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 1001)
        }
    }

    private fun initApp() {
        DataRepoFactory.getInstance(applicationContext)
        // TODO: get basic data
    }

    override fun onPause() {
        super.onPause()
        setIsVisible(false)
    }

    override fun onResume() {
        super.onResume()
        setIsVisible(true)
    }

    private fun initModel(thisActivity: MainActivity) {
        model = ViewModelProvider(this).get(MainActivityViewModel::class.java)
        model.getIsSignIn().observe(this) { aBoolean ->
            if (aBoolean != null) {
                if (!aBoolean) {
                    goToLogin(thisActivity)
                } else {
                    loadDataPref()
                    setIsReady(true)
                    if (model.getActivity() == null) {
                        showDialog(thisActivity)
                    }
                }
            }
        }
        model.getCurrentUsersActivities().observe(this) { activities ->
            if (activities != null) {
                if (activities.size > 0) {
                    // TODO: show select activity dialog
                } else {
                    // TODO: show there is no activity connected to this
                }
            }
        }
        model.getCurrentActivity().observe(this) { activity ->
            if (activity != null) currentActivity = activity
        }
        model.initUser(intent)
    }

    private fun initContentViews() {
        setContentView(R.layout.activity_main)
        findViews()
        setSupportActionBar(toolbar)
        val actionBar: ActionBar? = supportActionBar
        setupNavigationComponent()
    }

    private fun goToLogin(thisActivity: MainActivity) {
        val intent = Intent(thisActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        setIsReady(true)
        startActivity(intent)
    }

    private fun loadDataPref() {
        // model.loadUserDataFromSP(getSharedPreferences("UserSharedPref", MODE_PRIVATE))
        // model.loadActivityDataFromSP(getSharedPreferences(ACT_SP, MODE_PRIVATE))
    }

    private fun showDialog(thisActivity: MainActivity) {
        val view = thisActivity.layoutInflater.inflate(R.layout.no_activity_dialog, null)
        val dialog = AlertDialog.Builder(this).setView(view).create()
        val uidBox: TextView = view.findViewById(R.id.dialog_uId_box)
        val createActivityButton: Button = view.findViewById(R.id.dialog_create_activity_button)
        uidBox.text = model.getUser()?.id
        uidBox.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(model.getUser()?.id, model.getUser()?.id)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(thisActivity, "Text copied!", Toast.LENGTH_SHORT).show()
            true
        }
        createActivityButton.setOnClickListener {
            navController.navigate(R.id.action_mapFragment_to_activityOverviewFragment)
            dialog.cancel()
            // TODO: go to activity overview on edit mode
        }
        dialog.show()
    }

    private fun findViews() {
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
    }

    private fun setupNavigationComponent() {
        val container = supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment
        navController = container.navController
        appBarConfiguration = AppBarConfiguration.Builder(
            R.id.nav_map, R.id.nav_activity_overview, R.id.nav_chat, R.id.nav_speed_dial
        ).setDrawerLayout(drawerLayout).build()
        NavigationUI.setupWithNavController(navView, navController)
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration)
    }

    private fun hasAllLocationPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
        return permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startLocationService() {
        DataRepoFactory.getInstance(applicationContext)
        startService(Intent(applicationContext, LocationTrackingService::class.java))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startLocationService()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.searchbar_actions, menu)
        return true
    }

    override fun onFocusChange(view: View?, b: Boolean) {
        if (!b) {
            searchView.isIconified = true
            toolbar.setBackgroundColor(resources.getColor(R.color.transparent, theme))
        } else {
            toolbar.setBackgroundColor(resources.getColor(com.google.android.material.R.color.design_default_color_primary_variant, theme))
        }
    }

    fun setToolbarFloating(b: Boolean) {
        val toolbarContainer: CardView = findViewById(R.id.toolbarContainer)
        val container: FragmentContainerView = findViewById(R.id.container)
        val layoutParams = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val layoutParams2 = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        )
        layoutParams.topToTop = 0
        layoutParams.endToEnd = 0
        layoutParams.startToStart = 0
        layoutParams.verticalBias = 0f
        layoutParams.bottomToBottom = 0
        layoutParams2.rightToRight = 0
        layoutParams2.leftToLeft = 0
        if (!b) {
            layoutParams.setMargins(0, 0, 0, 0)
            layoutParams2.topToBottom = toolbarContainer.id
            layoutParams2.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
        if (b) {
            layoutParams.setMargins(32, 32, 32, 0)
            layoutParams2.topToTop = 0
            layoutParams2.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
        toolbarContainer.layoutParams = layoutParams
        container.layoutParams = layoutParams2
    }

    override fun onBackPressed() {
        closeKeyboard()
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        if (!this.searchView.isIconified) {
            this.searchView.isIconified = true
        } else {
            if (navController.currentDestination?.id != R.id.nav_map) {
                navController.navigate(R.id.nav_map)
            } else {
                if (doubleBackToExitPressedOnce) {
                    super.onBackPressed()
                    return
                }
                this.doubleBackToExitPressedOnce = true
                Toast.makeText(this, R.string.Click_again_to_exit, Toast.LENGTH_SHORT).show()
                Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
            }
        }
    }

    private fun closeKeyboard() {
        val view = currentFocus
        if (view != null) {
            val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            manager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
