package com.example.camerafinal

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.example.camerafinal.databinding.ActivitySecondBinding
import android.media.ExifInterface.*
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.util.Rational
import android.widget.*
import android.content.ContentResolver
import android.content.ContentUris
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.View
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileDescriptor


class SecondActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecondBinding
    private lateinit var btnSave: Button
    private lateinit var latText: EditText
    private lateinit var longText: EditText
    private lateinit var altText: EditText
    private lateinit var imageView: ImageView
    private lateinit var gView: View
    private lateinit var gUri: Uri
    private var picFlag = MainActivity.PhotoSource.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecondBinding.inflate(layoutInflater)
        val view = binding.root
        gView = binding.root
        setContentView(view)
        btnSave = binding.btnSave
        latText = binding.latitude
        longText = binding.longitude
        altText = binding.altitude
        imageView = binding.imageView
        val uri: Uri?
        if (intent.hasExtra("com.example.camerafinal.galleryUri")) {
            Toast.makeText(this, "Received gallery image!", Toast.LENGTH_LONG).show()
            uri = intent.getStringExtra("com.example.camerafinal.galleryUri")!!.toUri()
            gUri = uri
            imageView.setImageURI(uri)
            val exif = exifFromGallery(uri)
            if (intent.hasExtra("com.example.camerafinal.flag"))
                picFlag = MainActivity.PhotoSource.valueOf(intent.getStringExtra("com.example.camerafinal.flag")!!)
            val tempLatLong = exif.latLong
            if (tempLatLong != null) {
                latText.setText(tempLatLong[0].toString(), TextView.BufferType.EDITABLE)
                longText.setText(tempLatLong[1].toString(), TextView.BufferType.EDITABLE)
            } else {
                latText.setText("1.0", TextView.BufferType.EDITABLE)
                longText.setText("1.0", TextView.BufferType.EDITABLE)
            }
            val tempAlt = exif?.getAltitude(15000.0)
            if (tempAlt == 15000.0 || tempAlt == 0.0) {
                altText.setText("15000.0", TextView.BufferType.EDITABLE)
            } else {
                altText.setText(tempAlt.toString(), TextView.BufferType.EDITABLE)
            }
        }
        else {
            Toast.makeText(this, "Recieved taken image!", Toast.LENGTH_LONG).show()
            uri = intent.getStringExtra("com.example.camerafinal.takenUri")!!.toUri()
            gUri = uri
            imageView.setImageURI(uri)
            val exif = exifFromTaken(this, uri)
            if (intent.hasExtra("com.example.camerafinal.flag"))
                picFlag = MainActivity.PhotoSource.valueOf(intent.getStringExtra("com.example.camerafinal.flag")!!)
            val tempLatLong = exif.latLong
            if (tempLatLong != null) {
                latText.setText(tempLatLong[0].toString(), TextView.BufferType.EDITABLE)
                longText.setText(tempLatLong[1].toString(), TextView.BufferType.EDITABLE)
            } else {
                latText.setText("1.0", TextView.BufferType.EDITABLE)
                longText.setText("1.0", TextView.BufferType.EDITABLE)
            }
            val tempAlt = exif.getAltitude(15000.0)
            if (tempAlt == 15000.0) {
                altText.setText("15000.0", TextView.BufferType.EDITABLE)
            } else {
                altText.setText(tempAlt.toString(), TextView.BufferType.EDITABLE)
            }
        }

        btnSave.setOnClickListener {
            if (!latText.text.isNullOrEmpty() && !longText.text.isNullOrEmpty() && picFlag == MainActivity.PhotoSource.GALLERY) {
                var exif: ExifInterface? = null
                var responseString = ""
                if (Build.VERSION.SDK_INT >= 30) {
                    val newUri = parseImageUri(gUri)
                    gUri = newUri
                    val pendingIntent =
                        MediaStore.createWriteRequest(contentResolver, mutableListOf(gUri))
                    startIntentSenderForResult(pendingIntent.intentSender, 33, null, 0, 0, 0)
                } else {
                    exif = exifFromGallery(uri)
                    responseString = storeVars(exif)
                    try {
                        exif.saveAttributes()
                        if (Build.VERSION.SDK_INT == 25) {
                            Snackbar.make(gView, responseString, Snackbar.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this,
                                responseString,
                                Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("Error", e.printStackTrace().toString())
                        if (Build.VERSION.SDK_INT == 25) {
                            Snackbar.make(gView, "Failed to save updated file.", Snackbar.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Failed to save updated file.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            } else if (!latText.text.isNullOrEmpty() && !longText.text.isNullOrEmpty() && picFlag == MainActivity.PhotoSource.TAKEN) {
                val exif = exifFromTaken(this, uri)
                val responseString = storeVars(exif)
                try {
                    exif.saveAttributes()
                    if (Build.VERSION.SDK_INT == 25) {
                        Snackbar.make(gView, responseString, Snackbar.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this,
                            responseString,
                            Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("Error", e.printStackTrace().toString())
                    if (Build.VERSION.SDK_INT == 25) {
                        Snackbar.make(gView, "Failed to save updated file.", Snackbar.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Failed to save updated file.", Toast.LENGTH_SHORT).show()
                    }
                }

            } else {
                Toast.makeText(this, "Something has gone very wrong", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun storeVars(exif: ExifInterface?): String {
        val loc = Location("")
        loc.longitude = checkLong(longText.text.toString().toDouble())
        loc.latitude = checkLat(latText.text.toString().toDouble())
        val res = getLatLong(exif!!, loc)
        val lat = res[0]
        val long = res[1]
        val altitude = altText.text.toString()
        getAltitude(exif, altitude)
        return "Image saved with altitude: $altitude m and Lat Long [$lat, $long]"
    }

    private fun checkLat(lat: Double): Double {
        return when {
            lat < -90.0 -> {
                -90.0
            }
            lat > 90.0 -> {
                90.0
            }
            else -> {
                lat
            }
        }
    }

    private fun checkLong(long: Double): Double {
        return when {
            long < -180.0 -> {
                -180.0
            }
            long > 180.0 -> {
                180.0
            }
            else -> {
                long
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            33 -> {
                if (resultCode != RESULT_CANCELED) {
                    val exif = ExifInterface(
                        contentResolver.openFileDescriptor(
                            gUri,
                            "rw"
                        )!!.fileDescriptor
                    )
                    val responseString = storeVars(exif)
                    try {
                        exif.saveAttributes()
                        if (Build.VERSION.SDK_INT == 25) {
                            Snackbar.make(gView, responseString, Snackbar.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this,
                                responseString,
                                Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("Error", e.printStackTrace().toString())
                        if (Build.VERSION.SDK_INT == 25) {
                            Snackbar.make(gView, "Failed to save updated file.", Snackbar.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Failed to save updated file.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun exifFromGallery(uri: Uri): ExifInterface {
        return when (Build.VERSION.SDK_INT) {
            in 24..29 -> {
                val path = getPathFromURI(uri)
                ExifInterface(path)
            }
            29-> {
                val fd = contentResolver.openFileDescriptor(uri, "rw", null)!!.fileDescriptor
                ExifInterface(fd)
            }
            else -> {
                val fd = contentResolver.openFileDescriptor(uri, "r", null)!!.fileDescriptor
                ExifInterface(fd)
            }

        }

    }


    private fun exifFromTaken(ctx: Context, uri: Uri): ExifInterface {
        return ExifInterface(
            ctx.contentResolver.openFileDescriptor(
                uri,
                "rw",
                null
            )!!.fileDescriptor
        )
    }


    private fun getPathFromURI(uri: Uri):String {
        val result: String
        val cursor: Cursor? = contentResolver.query(uri, null, null,
            null, null)
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = uri.path.toString()
        } else {
            cursor.moveToFirst()
            val idx = if (Build.VERSION.SDK_INT <= 29) {
                cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            } else {
                cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)
            }
            result = cursor.getString(idx)
            cursor.close()
        }
        return result
    }


    private fun getLatLong(exif: ExifInterface, testLoc: Location): Array<String> {
        val longitude = testLoc.longitude
        val latitude = testLoc.latitude
        val longArr = Location.convert(longitude, Location.FORMAT_SECONDS).split(":")
        val long = longArr[0] + "/1," + longArr[1] + "/1," + longArr[2] + "/1000"
        val latArr = Location.convert(latitude, Location.FORMAT_SECONDS).split(":")
        val lat = latArr[0] + "/1," + latArr[1] + "/1," + latArr[2] + "/1000"
        setLatLong(exif, long, lat, latitude, longitude)
        return arrayOf(lat, long)
    }

    private fun setLatLong(exif: ExifInterface, long: String, lat: String, latitude: Double, longitude: Double) {
        val longRef = if (longitude < 0) "W" else "E"
        val latRef = if (latitude < 0) "S" else "N"
        exif.setAttribute(TAG_GPS_LATITUDE, lat)
        exif.setAttribute(TAG_GPS_LATITUDE_REF, latRef)
        exif.setAttribute(TAG_GPS_LONGITUDE, long)
        exif.setAttribute(TAG_GPS_LONGITUDE_REF, longRef)
    }

    private fun getAltitude(exif: ExifInterface, strAltitude: String) {
        val altitude = strAltitude.toDouble()
        val rationalAlt = toRational(altitude)
        setAltitude(exif, rationalAlt)
    }

    private fun setAltitude(exif: ExifInterface, altitude: Rational) {
        if (altitude >= Rational(0, 1)) {
            exif.setAttribute(TAG_GPS_ALTITUDE_REF, "0")
            var nAlt = altitude.toString()
            exif.setAttribute(TAG_GPS_ALTITUDE, altitude.toString())
        } else {
            exif.setAttribute(TAG_GPS_ALTITUDE_REF, "1")
            exif.setAttribute(TAG_GPS_ALTITUDE, altitude.toString())
        }
    }

    private fun parseImageUri(uri: Uri): Uri {
        //content://com.google.android.apps.photos.contentprovider/-1/1/content%3A%2F%2Fmedia%2Fexternal%2Fimages%2Fmedia%2F54/ORIGINAL/NONE/image%2Fjpeg/1151848230
        val uriArr = uri.toString().split("/")
        val idSect = uriArr[uriArr.size - 1].split("%3A")
        val newUri = idSect[1]
        return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newUri.toLong())
    }

    private fun toRational(num: Number): Rational {
        return Rational(num.toInt(), 1)
    }

}