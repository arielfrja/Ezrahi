package com.arielfaridja.ezrahi.UI.Main;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.splashscreen.SplashScreen;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.arielfaridja.ezrahi.LocationTrackingService;
import com.arielfaridja.ezrahi.R;
import com.arielfaridja.ezrahi.UI.Login.LoginActivity;
import com.arielfaridja.ezrahi.data.DataRepoFactory;
import com.arielfaridja.ezrahi.entities.Activity;
import com.arielfaridja.ezrahi.entities.User;
import com.google.android.material.navigation.NavigationView;


public class MainActivity extends AppCompatActivity implements View.OnFocusChangeListener {
    User user;
    Activity currentActivity;
    NavController navController;
    SharedPreferences sharedPreferences;
    Boolean isReady = false;
    private Toolbar toolbar;
    private SearchView searchView;
    private NavigationView navView;
    private DrawerLayout drawerLayout;
    private AppBarConfiguration appBarConfiguration;
    private MainActivityViewModel model;

    public MainActivity() {
    }

    public boolean getIsReady() {
        return isReady;
    }

    public void setIsReady(boolean isSignedIn) {
        this.isReady = isSignedIn;
    }

    public User getUser() {
        return model.getUser();
    }

    public void onBackPressed() {
        closeKeyboard();
        if (!this.searchView.isIconified()) {
            this.searchView.setIconified(true);
        } else {
            //Do nothing
        }

    }

    private void closeKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager manager =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        //show the splash screen, until we see where the user go, and after all data retrieved
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        //basic operations tp happen when app initializing
        initApp();

        //connect the splash screen to isReady, which represent if the activity ready to work
        splashScreen.setKeepOnScreenCondition(() -> !isReady);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        MainActivity thisActivity = this;

        initModel(thisActivity); //even it illegal, we must init the ViewModel with activity, to be able to connect the sharedPreferences
        initContentViews();

        //check data repo functions
        //model.IDataRepo.user_add(user);
    }

    private void initApp() {
        DataRepoFactory.getInstance(getApplicationContext());
        //TODO: get basic data
    }

    private void initModel(MainActivity thisActivity) {
        model = new ViewModelProvider(this).get(MainActivityViewModel.class);
        model.getIsSignIn().observe(this, aBoolean -> {
            if (aBoolean != null)
                if (!aBoolean) {
                    goToLogin(thisActivity);
                } else {
                    startService(new Intent(getApplicationContext(), LocationTrackingService.class));
                    loadDataPref();
                    setIsReady(true);
                    if (model.getActivity() == null) {
                        showDialog(thisActivity);
                    }

                }
        });
        model.getCurrentUsersActivities().observe(this, activities -> {
            if (activities != null)
                if (activities.size() > 0)
                    ;//TODO: show select activity dialog
                else
                    ;//TODO: show there is no activity connected to this

        });
        model.getCurrentActivity().observe(this, activity -> {
            if (activity != null)
                currentActivity = activity;
        });
        model.initUser(getIntent());
    }

    private void initContentViews() {
        this.setContentView(R.layout.activity_main);
        //Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        //this.mapDefinition();
        this.findViews();

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        setupNavigationComponent();
        MenuInflater inflater = this.getMenuInflater();
        this.searchView = (SearchView) this.toolbar.getMenu().getItem(0).getActionView();
        this.toolbar.setOnFocusChangeListener(this);
        this.toolbar.setOnClickListener((view) -> {

            this.searchView.setIconified(false);
        });
    }

    private void goToLogin(MainActivity thisActivity) {
        Intent intent = new Intent(thisActivity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        setIsReady(true);

        startActivity(intent);
    }

    private void loadDataPref() {
//        model.loadUserDataFromSP(getSharedPreferences("UserSharedPref", MODE_PRIVATE));
//        model.loadActivityDataFromSP(getSharedPreferences(ACT_SP, MODE_PRIVATE));
    }

    private void showDialog(MainActivity thisActivity) {
        View view = thisActivity.getLayoutInflater().inflate(R.layout.no_activity_dialog, null);


        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view).create();

        TextView uidBox = view.findViewById(R.id.dialog_uId_box);
        Button createActivityButton = view.findViewById(R.id.dialog_create_activity_button);
        uidBox.setText(model.getUser().getId());
        uidBox.setOnLongClickListener(view1 ->
        {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(model.getUser().getId(), model.getUser().getId());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(thisActivity, "Text copied!", Toast.LENGTH_SHORT).show();
            return true;
        });
        createActivityButton.setOnClickListener(view12 -> {
            navController.navigate(R.id.action_mapFragment_to_activityOverviewFragment);
            dialog.cancel();
            //TODO: go to activity overview on edit mode
        });
        dialog.show();
    }

    private void findViews() {
        ////delete that after modification
        //this.map = this.findViewById(R.id.map);
        this.toolbar = this.findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
    }

    private void setupNavigationComponent() {
        NavHostFragment container = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.container);
        navController = container.getNavController();
        AppBarConfiguration appBarConfiguration =
                new AppBarConfiguration.Builder(R.id.nav_map, R.id.nav_activity_overview, R.id.nav_chat, R.id.nav_speed_dial).setDrawerLayout(drawerLayout)
                        .build();
        NavigationUI.setupWithNavController(navView, navController);
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.searchbar_actions, menu);
        return true;
    }

    @Override
    public void onFocusChange(View view, boolean b) {
        if (!b) {
            this.searchView.setIconified(true);
            this.toolbar.setBackgroundColor(getResources().getColor(R.color.transparent, getTheme()));
        } else {
            this.toolbar.setBackgroundColor(getResources().getColor(com.google.android.material.R.color.design_default_color_primary_variant, getTheme()));
        }

    }

    public void setToolbarFloating(boolean b) {
        CardView toolbarContainer = findViewById(R.id.toolbarContainer);
        FragmentContainerView container = findViewById(R.id.container);

        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        ConstraintLayout.LayoutParams layoutParams2 = new ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);

        layoutParams.topToTop = 0;
        layoutParams.endToEnd = 0;
        layoutParams.startToStart = 0;
        layoutParams.verticalBias = 0f;

        layoutParams.bottomToBottom = 0;
        layoutParams2.rightToRight = 0;
        layoutParams2.leftToLeft = 0;

        if (!b) {

            layoutParams.setMargins(0, 0, 0, 0);

            layoutParams2.topToBottom = toolbarContainer.getId();
        }
        if (b) {

            layoutParams.setMargins(32, 32, 32, 0);

            layoutParams2.topToTop = 0;


        }
        toolbarContainer.setLayoutParams(layoutParams);
        container.setLayoutParams(layoutParams2);

    }
}
