package com.unpa.opencvmanipulacion

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.OpenCVLoader

class PrincipalActivity : AppCompatActivity() {

    private lateinit var image: ImageView

    private val REQUEST_IMAGE_CAPTURE = 1
    private val CAMERA_PERMISSION_REQUEST_CODE = 2

    var imageUri: Uri? = null


    /**Metodo que se encarga de cargar una imagen de forma local utilizando pickmedia api > 33*/
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){ uri ->
        if (uri != null){
            image.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        if (OpenCVLoader.initDebug()) {
            Toast.makeText(baseContext, "Se cargo OpenCv", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(baseContext, "No cargo OpenCv", Toast.LENGTH_SHORT).show()
        }

        image = findViewById<ImageView>(R.id.image)

        val btnCargarImagen = findViewById<Button>(R.id.btnLoad)

        btnCargarImagen.setOnClickListener {
            cargarImagen()
        }

        val buttonCapture: Button = findViewById(R.id.btnCamera)
        buttonCapture.setOnClickListener {
            if (checkCameraPermission()) {
                captureImage()
            } else {
                requestCameraPermission()
            }
        }
    }


    private fun checkCameraPermission(): Boolean {
        val resultCamera = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )
        return resultCamera == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImage()
            } else {
                Toast.makeText(
                    this,
                    "Permiso de cámara denegado. No se puede capturar la imagen.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun captureImage() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap?
            image.setImageBitmap(imageBitmap)
        }
    }

    /**Se lanza una ventana para cargar una imagen y se
     * genera un uid unico para nombrar a la imagen de esa forma */
    private fun cargarImagen() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

}