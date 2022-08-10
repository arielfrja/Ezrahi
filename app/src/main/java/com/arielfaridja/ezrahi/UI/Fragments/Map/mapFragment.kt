package com.arielfaridja.ezrahi.UI.Fragments.Map

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.UI.Main.MainActivity
import com.arielfaridja.ezrahi.entities.User
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class mapFragment : Fragment() {
    private var toolbar: Toolbar? = null
    var map: MapView? = null
    var mapController: IMapController? = null
    var myLocationOverlay: MyLocationNewOverlay? = null
    var user: User? = null


    private lateinit var model: MapViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.fragment_map, container, false)
        if (activity is MainActivity) {
            val mainActivity = activity as MainActivity
            user = mainActivity.user
            mainActivity.supportActionBar!!.title = null
            mainActivity.setToolbarFloating(true)
        }
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        this.findViews(view)
        this.mapDefinition()



        return view
    }


    private fun findViews(view: View) {
        map = view.findViewById(R.id.map)
        this.toolbar = activity?.findViewById<Toolbar>(R.id.toolbar)
    }


    private fun mapDefinition() {
        map!!.setTileSource(TileSourceFactory.MAPNIK)
        map!!.setMultiTouchControls(true)
        map!!.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        mapController = map!!.controller
        (mapController as IMapController).setZoom(10.0)
        (mapController as IMapController).setCenter(GeoPoint(31.776551, 35.233808))
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
        myLocationOverlay!!.enableMyLocation()
        myLocationOverlay!!.enableFollowLocation()
        map!!.overlays.add(myLocationOverlay)
        (mapController as IMapController).setCenter(myLocationOverlay!!.myLocation)
    }

    companion object {
        fun newInstance() = mapFragment()
    }
}