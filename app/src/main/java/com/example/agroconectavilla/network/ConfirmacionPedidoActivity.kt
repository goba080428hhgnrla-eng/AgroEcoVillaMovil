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

        // Configuración para OSMDroid
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_confirmacion_pedido)

        map = findViewById(R.id.mapa_osm)
        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK) // Esto ya no debería estar en rojo
            map.setMultiTouchControls(true)

            val mapController = map.controller
            mapController.setZoom(16.5)

            val startPoint = GeoPoint(19.7289, -99.4622)
            mapController.setCenter(startPoint)

            val startMarker = Marker(map)
            startMarker.position = startPoint
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            startMarker.title = "Punto de entrega"
            map.overlays.add(startMarker)
        }
    }
}