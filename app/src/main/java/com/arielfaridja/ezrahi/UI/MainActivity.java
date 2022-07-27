//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.arielfaridja.ezrahi.UI;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import com.arielfaridja.ezrahi.R;
import com.arielfaridja.ezrahi.entities.Latlng;
import com.arielfaridja.ezrahi.entities.User;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController.Visibility;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MainActivity extends AppCompatActivity implements View.OnFocusChangeListener {
    //delete that after modification
    /////
    MapView map;
    IMapController mapController;
    MyLocationNewOverlay myLocationOverlay;
    User user;
    private Toolbar toolbar;
    private SearchView searchView;
    /////

    FrameLayout container;


    public MainActivity() {
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setUser();
        Context context = this.getApplicationContext();
        this.setContentView(R.layout.activity_main);


        //Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        this.findViews();
        this.mapDefinition();
        MenuInflater inflater = this.getMenuInflater();
        this.searchView = (SearchView) this.toolbar.getMenu().getItem(0).getActionView();
        this.toolbar.setOnFocusChangeListener(this);
        this.toolbar.setOnClickListener((view) -> {
            this.searchView.setIconified(false);
        });
    }

    private void setUser() {
        this.user = new User(this.getIntent().getStringExtra("id"), this.getIntent().getStringExtra("firstName"), this.getIntent().getStringExtra("lastName"), this.getIntent().getStringExtra("phone"), this.getIntent().getStringExtra("email"), new Latlng(this.getIntent().getDoubleExtra("longitude", 32.0D), this.getIntent().getDoubleExtra("latitude", 34.0D)));
    }

    public void onBackPressed() {
        if (!this.searchView.isIconified()) {
            this.searchView.setIconified(true);
        } else {
            super.onBackPressed();
        }

    }

    private void findViews() {
        ////delete that after modification
        //this.map = this.findViewById(R.id.map);
        this.toolbar = this.findViewById(R.id.toolbar);
        container = findViewById(R.id.container);
    }

    private void mapDefinition() {
        this.map.setTileSource(TileSourceFactory.MAPNIK);
        this.map.setMultiTouchControls(true);
        this.map.getZoomController().setVisibility(Visibility.SHOW_AND_FADEOUT);
        this.mapController = this.map.getController();
        this.mapController.setZoom(10.0D);
        this.mapController.setCenter(new GeoPoint(31.776551D, 35.233808D));
        this.myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), this.map);
        this.myLocationOverlay.enableMyLocation();
        this.myLocationOverlay.enableFollowLocation();
        this.map.getOverlays().add(this.myLocationOverlay);
        this.mapController.setCenter(this.myLocationOverlay.getMyLocation());
    }

    public User getUser() {
        return user;
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
}
