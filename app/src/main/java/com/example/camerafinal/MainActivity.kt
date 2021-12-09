package com.example.camerafinal

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import android.net.Uri
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.camerafinal.databinding.ActivityMainBinding
import androidx.core.app.ActivityCompat
import android.content.Intent
import androidx.core.net.toUri
import android.content.pm.ResolveInfo
import android.database.Cursor
import android.media.ExifInterface
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface.*
import java.util.*


// TODO: Add API Level Checks for specific tags
class MainActivity : AppCompatActivity() {

    private var imgView: ImageView? = null
    private var flagPhoto = PhotoSource.NONE
    private lateinit var layout: View
    private var globUri: Uri? = null

    @SuppressLint("InlinedApi")
    private val cameraPermissions = when (Build.VERSION.SDK_INT ) { 29 ->
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
        30 ->
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                Manifest.permission.CAMERA
            )
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
    }

    @SuppressLint("InlinedApi")
    private val galleryPermissions =
        when (Build.VERSION.SDK_INT ) { 29 ->
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
            30 ->
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_MEDIA_LOCATION,
                )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        }


    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                isGranted: Boolean ->
            if (isGranted) {
                attemptPhoto()
            }
            else {
                Toast.makeText(
                    this, "Without camera permissions, you are unable to take a " +
                            "photo using this app.", Toast.LENGTH_LONG).show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        layout = binding.root
        setContentView(layout)

        val btnGallery = binding.btnGallery
        val btnTakePhoto = binding.btnTakePhoto
        val btnEditExif = binding.btnEditExif
        imgView = binding.imageView

        btnGallery.setOnClickListener {
            if (hasPermissions(this, galleryPermissions)) {
                val intent: Intent
                if (Build.VERSION.SDK_INT >= 30) {
                    val eci = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        intent = Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                        type = "image/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                } else {
                        intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                        type = "image/*"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                }
                try {
                    startActivityForResult(intent, 1)
                } catch (e: Exception) {
                    Toast.makeText(this, e.printStackTrace().toString(), Toast.LENGTH_LONG).show()
                }
            } else {
                ActivityCompat.requestPermissions(this, galleryPermissions, 2)
            }
        }
        btnTakePhoto.setOnClickListener {
            if (hasPermissions(this, cameraPermissions)) {
                if (Build.VERSION.SDK_INT >= 27) {
                    attemptPhoto()
                } else {
                    invokeCamera()
                }
            } else {
                ActivityCompat.requestPermissions(this, cameraPermissions, 3)
            }
        }
        btnEditExif.setOnClickListener {
            when (flagPhoto) {
                PhotoSource.NONE -> Toast.makeText(
                    this,
                    "You haven't selected an image!",
                    Toast.LENGTH_LONG
                ).show()
                PhotoSource.TAKEN -> {
                    takenIntent()
                }
                PhotoSource.GALLERY -> {
                    val intent = Intent(this, SecondActivity::class.java)
                    intent.flags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION + Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    val imgUri = (imgView as ImageView).tag as Uri
                    flagPhoto = PhotoSource.GALLERY
                    intent.putExtra("com.example.camerafinal.galleryUri", imgUri.toString())
                    intent.putExtra("com.example.camerafinal.flag", flagPhoto.toString())
                    startActivity(intent)
                }

            }

        }
    }

    private fun verifyPerms(permissions: Array<out String>, grantResults: IntArray): Boolean {
        var flag = true
        for (i in permissions.indices) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                flag = false
            }
        }
        return flag
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2) {
            val flag = verifyPerms(permissions, grantResults)
            if (flag) {
                val intent: Intent
                if (Build.VERSION.SDK_INT >= 30) {
                    val eci = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    intent = Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                        type = "image/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                } else {
                    intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                        type = "image/*"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                }
                try {
                    startActivityForResult(intent, 1)
                } catch (e: Exception) {
                    Toast.makeText(this, e.printStackTrace().toString(), Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Denied", Toast.LENGTH_SHORT).show()
            }
        }
        if (requestCode == 3){
            val flag = verifyPerms(permissions, grantResults)
            if (flag) {
                invokeCamera()
            } else {
                Toast.makeText(this, "Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hasPermissions(ctx: Context, perms: Array<String>): Boolean {
        for (perm in perms) {
            if (ActivityCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

    private fun takenIntent() {
        val intent = Intent(this, SecondActivity::class.java)
        val uri = (imgView as ImageView).tag as Uri
        intent.putExtra("com.example.camerafinal.takenUri", uri.toString())
        intent.putExtra("com.example.camerafinal.flag", flagPhoto.toString())
        startActivity(intent)
    }

    private fun galleryIntent() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        startActivityForResult(intent, 1)
    }

    private fun attemptPhoto() {
        if (cameraExists()) {
            takePhoto()
        } else {
            val alert = AlertDialog.Builder(this)
            alert.setMessage("Your device does not have a camera, and cannot perform the following action.")
            alert.show()
        }
    }

    private fun invokeCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            if (Build.VERSION.SDK_INT >= 27) {
                //startActivity(takePictureIntent)
                startActivityForResult(takePictureIntent, 0)
            } else {
                startActivityForResult(takePictureIntent, 0)
            }

        } catch (e: ActivityNotFoundException) {
            // display error state to the user
            e.printStackTrace()
        }
    }

    private fun takePhoto() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                invokeCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA)
            }
            else -> {
                cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA)
            }
        }
    }

    private fun cameraExists(): Boolean {
        if (applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            return true
        }
        return false
    }

    //TODO: Check ExifInterface to see if can retrieve by filename(String) in API level 5
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            0 ->
                if (resultCode != RESULT_CANCELED) {
                    if (resultCode == RESULT_OK && data != null) {
                        makeTakenIntent(data)
                    }
                }
            1 ->
                if (resultCode != RESULT_CANCELED) {
                    if (resultCode == RESULT_OK && data != null) {
                        val imgUri = data.data
                        flagPhoto = PhotoSource.GALLERY
                        imgView?.setImageURI(imgUri)
                        imgView?.tag = imgUri
                        val intent = Intent(this, SecondActivity::class.java)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        intent.putExtra("com.example.camerafinal.galleryUri", imgUri.toString())
                        intent.putExtra("com.example.camerafinal.flag", flagPhoto.toString())
                        startActivity(intent)
                    }
                }
        }
    }



    private fun makeTakenIntent(data: Intent?) {
        val uri = setImage(data)
        val intent = Intent(this, SecondActivity::class.java)
        intent.putExtra("com.example.camerafinal.takenUri", uri.toString())
        intent.putExtra("com.example.camerafinal.flag", flagPhoto.toString())
        startActivity(intent)
    }

    private fun setImage(data: Intent?): Uri? {
        flagPhoto = PhotoSource.TAKEN
        val selectedImage = data!!.extras?.get("data") as Bitmap
        imgView?.setImageBitmap(selectedImage)
        val uri: Uri? = if (Build.VERSION.SDK_INT >= 29) {
            modernInsert(selectedImage)
        } else {
            oldInsert(selectedImage)
        }
        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        return uri
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun modernInsert(selectedImage: Bitmap): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, System.currentTimeMillis().toString())
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri: Uri? =
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val outStream = contentResolver.openOutputStream(uri!!)
        selectedImage.compress(Bitmap.CompressFormat.JPEG, 90, outStream)
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        contentResolver.update(uri, values, null, null)
        imgView?.tag = uri
        outStream?.close()
        return uri
    }

    private fun oldInsert(selectedImage: Bitmap): Uri {
        val uri = MediaStore.Images.Media.insertImage(this.contentResolver, selectedImage, System.currentTimeMillis().toString(), "null").toUri()
        val outStream = contentResolver.openOutputStream(uri)
        //TODO: thread this compress call
        selectedImage.compress(Bitmap.CompressFormat.JPEG, 90, outStream)
        imgView?.tag = uri
        outStream?.close()
        return uri
    }

    private fun getPathFromURI(uri: Uri):String {
        //i might be doing something wrong with the URI
        //val uriExternal = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageId = DocumentsContract.getDocumentId(uri)
        val result: String
        //hardcoded this and it returned something but not the right thing
        //val testUri = "content://com.android.providers.media.documents/document".toUri()
        val cursor: Cursor? = contentResolver.query(uri, null, null,
            null, null)
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = uri.path.toString()
        } else {
            cursor.moveToFirst()
            val idx: Int = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result
    }

    enum class PhotoSource {
        NONE, GALLERY, TAKEN
    }

}