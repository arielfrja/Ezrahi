//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.UI.Main;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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

import com.arielfaridja.ezrahi.R;
import com.arielfaridja.ezrahi.UI.Login.LoginActivity;
import com.arielfaridja.ezrahi.entities.Latlng;
import com.arielfaridja.ezrahi.entities.User;
import com.google.android.material.navigation.NavigationView;


public class MainActivity extends AppCompatActivity implements View.OnFocusChangeListener {
    User user;
    private Toolbar toolbar;
    private SearchView searchView;
    NavController navController;
    private NavigationView navView;
    private DrawerLayout drawerLayout;
    private AppBarConfiguration appBarConfiguration;
    private MainActivityViewModel model;
    Boolean isReady = false;

    public MainActivity() {
    }

    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        model = new ViewModelProvider(this).get(MainActivityViewModel.class);
        splashScreen.setKeepOnScreenCondition(() -> !isReady);
        MainActivity thisActivity = this;
        model.getIsSignIn().observe(this, aBoolean -> {
            if (aBoolean != null)
                if (!aBoolean) {
                    Intent intent = new Intent(thisActivity, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    setIsReady(true);

                    startActivity(intent);
                } else {
                    model.loadUserDatafromSP(getSharedPreferences("UserSharedPref", MODE_PRIVATE));
                    model.loadActivityDatafromSP(getSharedPreferences("ActivitySharedPref", MODE_PRIVATE));
                    setIsReady(true);
                }
            if (model.getActivity().getId() == null) {
                setIsReady(true);
                showDialog(thisActivity);


            }
        });
        model.initUser(getIntent());
        Context context = this.getApplicationContext();
        this.setContentView(R.layout.activity_main);
        //Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        //this.mapDefinition();
        this.findViews();

        //check data repo functions
        //model.dataRepo.user_add(user);


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

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.searchbar_actions, menu);
        return true;
    }

    private void setupNavigationComponent() {
        NavHostFragment container = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.container);
        navController = container.getNavController();
        AppBarConfiguration appBarConfiguration =
                new AppBarConfiguration.Builder(navController.getGraph()).setDrawerLayout(drawerLayout)
                        .build();
        NavigationUI.setupWithNavController(navView, navController);
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);
    }

    private void initUser() {
        this.user = new User(this.getIntent().getStringExtra("id"),
                this.getIntent().getStringExtra("firstName"),
                this.getIntent().getStringExtra("lastName"),
                this.getIntent().getStringExtra("phone"),
                this.getIntent().getStringExtra("email"),
                new Latlng(this.getIntent().getDoubleExtra("longitude", 32.0D),
                        this.getIntent().getDoubleExtra("latitude", 34.0D)));
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

    private void findViews() {
        ////delete that after modification
        //this.map = this.findViewById(R.id.map);
        this.toolbar = this.findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
    }

    public User getUser() {
        return model.getUser();
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

    public boolean getIsReady() {
        return isReady;
    }

    public void setIsReady(boolean isSignedIn) {
        this.isReady = isSignedIn;
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
