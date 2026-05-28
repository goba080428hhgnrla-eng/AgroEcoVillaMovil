package com.example.agroconectavilla

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class ConfirmacionPedidoActivity : AppCompatActivity() {

    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración para OSMDroid (debe ir antes de setContentView)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_confirmacion_pedido)

        map = findViewById(R.id.mapa_osm)

        // Configuración del mapa
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(16.5)

        // Punto de entrega en Villa del Carbón
        val startPoint = GeoPoint(19.7289, -99.4622)
        mapController.setCenter(startPoint)

        // Configuración del marcador
        val startMarker = Marker(map)
        startMarker.position = startPoint
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.title = "Punto de entrega"
        map.overlays.add(startMarker)
    }

    override fun onResume() {
        super.onResume()
        map.onResume() // Necesario para OSMDroid
    }

    override fun onPause() {
        super.onPause()
        map.onPause() // Necesario para OSMDroid
    }
}