package com.example.exercicio02_maps

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.usb.UsbEndpoint
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.example.exercicio02_maps.databinding.ActivityMapsBinding
import com.google.android.gms.maps.model.*
import java.io.IOException
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var locationListener: LocationListener? = null
    private var locationManager: LocationManager? = null
    private var usermaker: Marker? = null
    private var droneMarker: Marker? = null
    private var isMapReady: Boolean = false
    private var polyline: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)



        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID)

        locationListener = object : LocationListener{
            override fun onLocationChanged(location: Location) {
                val usrPosition = LatLng(location.latitude, location.longitude)

                if (usermaker != null) {
                    usermaker!!.remove()
                }

                usermaker = mMap.addMarker(
                    MarkerOptions()
                        .position(usrPosition)
                        .title("Localização atual")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.home))
                )
                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(usrPosition, 19.0f)
                )
                //setLinha(usrPosition, droneMarker)
            }
        }

        binding.btnAdd.setOnClickListener{
            if(binding.editText.text.toString() != "") {
                var geoloc : LatLng? = geocoding(binding.editText.text.toString())
                //droneSetup(geoloc)
            }
        }

        isMapReady = true
        checkPermission()
        setupLocation()
    }

    fun geocoding(descricaoLocal : String) : LatLng? {
        val geocoder = Geocoder(applicationContext, Locale.getDefault())
        try {
            val local = geocoder.getFromLocationName(descricaoLocal, 1)
            if(local != null && local.size > 0) {
                var destino = LatLng(local[0].latitude, local[0].longitude)
                return destino
            }
        }catch (e: IOException) {
            e.message
        }
        return null
    }

    fun setLinha(startPoint: LatLng, endpoint: LatLng) {
        var polylineOptions = PolylineOptions()
        polylineOptions?.add(startPoint)
        polylineOptions?.add(endpoint)

        var results = FloatArray(1)
        Location.distanceBetween(startPoint.latitude, startPoint.longitude, endpoint.latitude, endpoint.longitude, results)

        if (results[0] <= 500.0f) {
            polylineOptions?.color(Color.GREEN)?.width(20.0f)
        } else {
            polylineOptions?.color(Color.RED)?.width(20.0f)
        }

        if (polyline != null) {
            polyline?.remove()
        }
        polyline = mMap.addPolyline(polylineOptions)
    }

    fun droneSetup(descricaoLocal: String?) {
        val geocoder = Geocoder(applicationContext, Locale.getDefault())
        try {
            val local = geocoder.getFromLocationName(descricaoLocal, 1)
            if (local != null && local.size > 0) {
                var destino = LatLng(local[0].latitude, local[0].longitude)
                if (droneMarker != null) {
                    droneMarker?.remove()
                }

                droneMarker = mMap.addMarker(
                    MarkerOptions().position(destino)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.drone))
                        .title("Destino")
                )
                Toast.makeText(applicationContext, local[0].getAddressLine(0).toString(),Toast.LENGTH_SHORT).show()

                if(droneMarker != null && usermaker != null) {
                    setLinha(usermaker!!.position, droneMarker!!.position)
                }
            } else {
                Toast.makeText(applicationContext, "Local não encontrado", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            e.message
        }
    }

    fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "Permissões Ativadas", Toast.LENGTH_SHORT).show()
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
            ) {
                alertaPermissaoNegada()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            setupLocation() // Método acoplado para setar a localização
        }
    }

    fun alertaPermissaoNegada() {
        val alert = AlertDialog.Builder(this)
        alert.setTitle("Permissões Requeridas")
        alert.setMessage("Para continuar utilizando todos os recursos do aplicativo, é altamente recomendado autorizar o acesso a sua localização.")
        alert.setCancelable(false)
        alert.setPositiveButton(
            "Corrigir"
        ) { dialog, which ->
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        alert.setNegativeButton(
            "Cancelar"
        ) { dialog, which ->
            Toast.makeText(getApplicationContext(), "Algumas das funcionalidades do app foram desabilitadas.", Toast.LENGTH_LONG).show();
        }
        val alertDialog = alert.create()
        alertDialog.show()
    }

    fun setupLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000, 10f, locationListener!!
            )
        }
    }

}