package com.EQMobile.WorkSample

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import okhttp3.*
import java.io.IOException

import androidx.core.content.ContextCompat

public data class LocationEvent(val lat: Float, val lon: Float, val time: Long, val ext: String)

public class Library {
    private val TAG = "[Library] "
    private var context: Context
    private var sUrl : String? = "https://httpbin.org/post"
    private var httpUrl : HttpUrl? = null
    private var sResponse : String? = null
    private var isHttpUrlSet : Boolean = false

    private var locationManager: LocationManager
    private var locationDelay = 320000L
    private val eventList = mutableListOf<LocationEvent>()

    constructor(context: Context) {
        this.context = context
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager;
    }

    fun setup(): Boolean {
        return true
    }

    @TargetApi(android.os.Build.VERSION_CODES.M)
    @Synchronized
    fun log(event: LocationEvent) {
        if (context == null) {
            println(TAG + "Context null")
            return
        }

        var lat = event.lat
        var lon = event.lon
        var time = event.time
        if (lat > 90.0f || lat < -90.0f) lat = 0.0f
        if (lon > 180.0f || lon < -180.0f) lon = 0.0f
        if (time <= 0) time = java.util.Calendar.getInstance().getTime().time
        eventList.add(LocationEvent(lat, lon, time , event.ext))

        var provider: String? = null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            provider = LocationManager.GPS_PROVIDER
            println(TAG + "Permission fine location")
        }
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            provider = LocationManager.NETWORK_PROVIDER
            println(TAG + "Permission coarse location")
        }
        else {
            println(TAG + "No location permission")
        }
        if (provider != null) {
            var locationListener = object : LocationListener1() {
                override fun onLocationChanged(p0: Location) {
                    val longitude = p0.longitude.toFloat()
                    val latitude = p0.latitude.toFloat()
                    println(TAG + "latitude: " + latitude + "longitude: " + longitude)
                    locationManager.removeUpdates(this)
                    setLocationReceived()

                    for (event1 in eventList) {
                        sendHttpRequest(LocationEvent(latitude,longitude, event1.time, event1.ext))
                    }
                    eventList.clear()
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                    super.onStatusChanged(provider, status, extras)
                    println(TAG + "onStatusChanged")
                }

                override fun onProviderEnabled(provider: String) {
                    super.onProviderEnabled(provider)
                    println(TAG + "onProviderEnabled")
                }

                override fun onProviderDisabled(provider: String) {
                    super.onProviderDisabled(provider)
                    println(TAG + "onProviderDisabled")
                }
            }
            locationManager.requestLocationUpdates(provider!!, 0, 0f, locationListener!!)

            var runnable = object : Runnable {
                private var listenerForLocation : LocationListener1? = null

                fun setListener(listener: LocationListener1) {
                    listenerForLocation = listener
                }

                override fun run() {
                    if (listenerForLocation?.isLocationReceived() == false) {
                        println(TAG + "Time out")
                        locationManager!!.removeUpdates(listenerForLocation!!)
                        listenerForLocation = null
                    }

                    for (event1 in eventList) {
                        sendHttpRequest(event1)
                    }
                    eventList.clear()
                }
            }
            runnable.setListener(locationListener!!)
            android.os.Handler().postDelayed(runnable, locationDelay)
        }
        else {
            for (event1 in eventList) {
                sendHttpRequest(event1)
            }
            eventList.clear()
        }
    }

    protected fun sendHttpRequest(event: LocationEvent) {
        var client : OkHttpClient = OkHttpClient().newBuilder().build()
        var requestBody : RequestBody = FormBody.Builder()
                .add("lat", event.lat.toString())
                .add("lon", event.lon.toString())
                .add("time", event.time.toString())
                .add("ext", event.ext)
                .build()
        var request : Request.Builder = Request.Builder()
        if (isHttpUrlSet) request.url(httpUrl!!)
        else if (sUrl != null && sUrl!!.length > 0) request.url(sUrl!!)
        else {
            println(TAG + "Url empty")
            return
        }
        request.post(requestBody)
        var call : Call = client.newCall(request.build());
        call.enqueue(
                object: Callback {
                    override fun onFailure(call : Call, e : IOException) {
                        sResponse = e.message
                        println(TAG + "E: " + sResponse);
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            sResponse = response.body?.string()
                            println(TAG + "S: " + sResponse)
                        }
                    }
                }
        )
        println(TAG + "Send event: (" + event.lat + "," + event.lon + "," + event.time + "," + event.ext + ")");
    }

    fun setUrl(url: String) {
        sUrl = url;
    }

    fun setHttpUrl(url: HttpUrl) {
        httpUrl = url
        if (httpUrl != null) isHttpUrlSet = true
    }

    fun getResponse(): String? {
        return sResponse
    }

    abstract class LocationListener1 : LocationListener {
        private var locationReceived = false

        fun isLocationReceived() : Boolean {
            return locationReceived
        }

        fun setLocationReceived() {
            locationReceived = true
        }
    }
}