package mx.teknoeducativa.agroconectavilla

import android.app.Application
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration

class AgroConectaApplication : Application() {

    companion object {
        lateinit var instance: AgroConectaApplication
            private set
        lateinit var fusedLocationClient: FusedLocationProviderClient
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Inicializar FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configurar OSMdroid
        val config = Configuration.getInstance()
        config.load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

        // Establecer directorio de caché para tiles de mapas
        config.osmdroidBasePath = cacheDir
        config.osmdroidTileCache = java.io.File(cacheDir, "osmdroid_tiles")

        // User agent para OSM
        config.setUserAgentValue("AgroConectaVilla/1.0")
    }
}