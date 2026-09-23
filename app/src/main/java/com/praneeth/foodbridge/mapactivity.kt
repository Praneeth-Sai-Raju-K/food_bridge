package com.praneeth.foodbridge

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MapActivity : AppCompatActivity() {

    private var locationText = "Bengaluru"

    override fun onCreate(savedInstanceState: Bundle?) {
        Configuration.getInstance().userAgentValue = packageName
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        locationText = intent.getStringExtra("location") ?: "Bengaluru"
        val foodName = intent.getStringExtra("foodName") ?: "Donation Location"

        val mapTitleText = findViewById<TextView>(R.id.mapTitleText)
        mapTitleText.text = foodName

        val mapView = findViewById<MapView>(R.id.osmMapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val fallbackPoint = GeoPoint(12.9716, 77.5946)
        mapView.controller.setZoom(14.0)
        mapView.controller.setCenter(fallbackPoint)
        addMarker(mapView, fallbackPoint, locationText)

        Thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val results = geocoder.getFromLocationName(locationText, 1)
                if (!results.isNullOrEmpty()) {
                    val result = results[0]
                    val point = GeoPoint(result.latitude, result.longitude)
                    runOnUiThread {
                        mapView.overlays.clear()
                        mapView.controller.setCenter(point)
                        addMarker(mapView, point, locationText)
                        mapView.invalidate()
                    }
                }
            } catch (e: Exception) {
                // Geocoding failed - fallback marker stays
            }
        }.start()

        findViewById<Button>(R.id.openInGoogleMapsButton).setOnClickListener {
            val encodedLocation = Uri.encode(locationText)
            val gmmIntentUri = Uri.parse("geo:0,0?q=$encodedLocation")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            try {
                startActivity(mapIntent)
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$encodedLocation")))
            }
        }
    }

    private fun addMarker(mapView: MapView, point: GeoPoint, title: String) {
        val marker = Marker(mapView)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = title
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        findViewById<MapView>(R.id.osmMapView).onResume()
    }

    override fun onPause() {
        super.onPause()
        findViewById<MapView>(R.id.osmMapView).onPause()
    }
}