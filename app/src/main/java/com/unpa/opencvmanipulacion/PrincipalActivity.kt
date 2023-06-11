package com.unpa.opencvmanipulacion

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import org.opencv.core.*
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc


class PrincipalActivity : AppCompatActivity() {

    private lateinit var image: ImageView
    private lateinit var imageOriginal: ImageView
    private lateinit var imageGris: ImageView
    private lateinit var imageRojo: ImageView
    private lateinit var imageAzul: ImageView
    private lateinit var imageVerde: ImageView
    private lateinit var imageBinaria: ImageView
    private lateinit var imageContorno: ImageView
    private lateinit var imageSegRojo: ImageView

    private val REQUEST_IMAGE_CAPTURE = 1
    private val CAMERA_PERMISSION_REQUEST_CODE = 2
    private val STORAGE_PERMISSION_REQUEST_CODE = 3

    var imageUri: Uri? = null


    /**Metodo que se encarga de cargar una imagen de forma local utilizando pickmedia api > 33*/
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){ uri ->
        if (uri != null){
            image.setImageURI(uri)
            imageOriginal.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        // Inicializar OpenCV
        OpenCVLoader.initDebug()

        title = " "

        //image = findViewById<ImageView>(R.id.image)
        imageOriginal = findViewById(R.id.ImgOriginal)
        imageGris = findViewById(R.id.grises)
        imageRojo = findViewById(R.id.rojo)
        imageAzul = findViewById(R.id.azul)
        imageVerde = findViewById(R.id.verde)
        imageBinaria = findViewById(R.id.binaria)
        imageContorno = findViewById(R.id.contornos)
        imageSegRojo = findViewById(R.id.segRojo)

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

        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    //Guardar a la galeria
                } else {
                    Toast.makeText(
                        this,
                        "Permiso de escritura en almacenamiento denegado. No se puede guardar la imagen.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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

            imageOriginal.setImageBitmap(imageBitmap)

            convertirEscalaGris(imageBitmap)

            convertirTresCanalesRGBToGray(imageBitmap)

            convertirImgBinaria(imageBitmap)

            dibujarContornos(imageBitmap)

            processImage(imageBitmap)

        }
    }

    private fun convertirEscalaGris(bitmap: Bitmap?){
        // Procesar la imagen en escala de grises
        val imageMat = Mat()
        Utils.bitmapToMat(bitmap, imageMat)
        val grayMat = Mat()
        Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_BGR2GRAY)
        val grayBitmap = Bitmap.createBitmap(grayMat.cols(), grayMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(grayMat, grayBitmap)
        imageGris.setImageBitmap(grayBitmap)
    }

    private fun convertirTresCanalesRGBToGray(imageBitmap: Bitmap?){
        // Crear una matriz de OpenCV a partir del bitmap
        val originalMat = Mat(imageBitmap!!.height, imageBitmap!!.width, CvType.CV_8UC4)
        Utils.bitmapToMat(imageBitmap, originalMat)
        // Convertir la imagen a escala de grises
        val grayMat2 = Mat(originalMat.rows(), originalMat.cols(), CvType.CV_8UC1)
        Imgproc.cvtColor(originalMat, grayMat2, Imgproc.COLOR_BGR2GRAY)
        // Crear una imagen en escala de grises con tres canales
        val grayRgbMat = Mat(originalMat.rows(), originalMat.cols(), CvType.CV_8UC3, Scalar(0.0, 0.0, 0.0))
        val channels = ArrayList<Mat>()
        channels.add(grayMat2)
        channels.add(grayMat2)
        channels.add(grayMat2)
        Core.merge(channels, grayRgbMat)
        // Convertir la matriz en un bitmap
        val grayRgbBitmap = Bitmap.createBitmap(grayRgbMat.cols(), grayRgbMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(grayRgbMat, grayRgbBitmap)
        // Mostrar los canales por separado en ImageViews
        val redBitmap = Bitmap.createBitmap(channels[2].cols(), channels[2].rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(channels[2], redBitmap)
        imageRojo.setImageBitmap(redBitmap)

        val greenBitmap = Bitmap.createBitmap(channels[1].cols(), channels[1].rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(channels[1], greenBitmap)
        imageVerde.setImageBitmap(greenBitmap)

        val blueBitmap = Bitmap.createBitmap(channels[0].cols(), channels[0].rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(channels[0], blueBitmap)
        imageAzul.setImageBitmap(blueBitmap)
    }

    private fun convertirImagenBinaria(imageBitmap: Bitmap?){
        // Crear una matriz de OpenCV a partir del bitmap
        val originalMat = Mat(imageBitmap!!.height, imageBitmap!!.width, CvType.CV_8UC4)
        Utils.bitmapToMat(imageBitmap, originalMat)
        val grayMat = Mat()
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY)

        val thresholdValue = 127.0 // Umbral para binarizar la imagen
        val maxValue = 255.0 // Valor máximo para los píxeles binarios
        val binaryMat = Mat()
        Imgproc.threshold(grayMat, binaryMat, thresholdValue, maxValue, Imgproc.THRESH_BINARY)

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(binaryMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val resultMat = originalMat.clone()
        for (i in contours.indices) {
            Imgproc.drawContours(resultMat, contours, i, Scalar(0.0, 0.0, 255.0), 2)
        }

        val resultBitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resultMat, resultBitmap)
        imageBinaria.setImageBitmap(resultBitmap)
    }

    private fun convertirImgBinaria(bitmap: Bitmap?){
        val grayBitmap = Bitmap.createBitmap(bitmap!!.width, bitmap!!.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayBitmap)
        val paint = Paint()
        val matrix = ColorMatrix()
        matrix.setSaturation(0f) // Configurar la matriz de color para convertir a escala de grises
        val filter = ColorMatrixColorFilter(matrix)
        paint.colorFilter = filter
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        }
        val threshold = 127 // Umbral para binarizar la imagen
        val binaryBitmap = Bitmap.createBitmap(grayBitmap.width, grayBitmap.height, Bitmap.Config.ARGB_8888)
        val thresholdedPixels = IntArray(grayBitmap.width * grayBitmap.height)
        grayBitmap.getPixels(thresholdedPixels, 0, grayBitmap.width, 0, 0, grayBitmap.width, grayBitmap.height)
        for (i in thresholdedPixels.indices) {
            val pixel = thresholdedPixels[i]
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            val grayscale = (red + green + blue) / 3
            val binary = if (grayscale > threshold) Color.WHITE else Color.BLACK
            thresholdedPixels[i] = binary
        }
        binaryBitmap.setPixels(thresholdedPixels, 0, grayBitmap.width, 0, 0, grayBitmap.width, grayBitmap.height)
        imageBinaria.setImageBitmap(binaryBitmap)
    }

    private fun dibujarContornos(bitmap: Bitmap?){
        val imageMat = Mat()
        Utils.bitmapToMat(bitmap, imageMat)

        val grayMat = Mat()
        Imgproc.cvtColor(imageMat, grayMat, Imgproc.COLOR_BGR2GRAY) // Convertir a escala de grises

        val threshold = 127
        val binaryMat = Mat()
        Imgproc.threshold(grayMat, binaryMat, threshold.toDouble(), 255.0, Imgproc.THRESH_BINARY)

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(binaryMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val resultMat = imageMat.clone() // Crear una copia de la imagen original
        Imgproc.drawContours(resultMat, contours, -1, Scalar(0.0, 255.0, 0.0), 2) // Dibujar los contornos en verde

        val resultBitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resultMat, resultBitmap)
        imageContorno.setImageBitmap(resultBitmap)

    }

    private fun  segmentacionColorsRojos(bitmap: Bitmap?){
        // Crear una matriz OpenCV a partir del bitmap
        val imageMat = Mat()
        Utils.bitmapToMat(bitmap, imageMat)

        // Convertir la imagen a HSV
        val hsvImage = Mat()
        Imgproc.cvtColor(imageMat, hsvImage, Imgproc.COLOR_RGB2HSV)

        // Definir los rangos de color para el rojo
        val lowerRed = Scalar(0.0, 100.0, 100.0)
        val upperRed = Scalar(10.0, 255.0, 255.0)

        // Crear la máscara para los píxeles rojos
        val redMask = Mat()
        Core.inRange(hsvImage, lowerRed, upperRed, redMask)

        // Aplicar la máscara a la imagen original
        val redObjects = Mat()
        Core.bitwise_and(imageMat, imageMat, redObjects, redMask)

        // Convertir la matriz resultante a un bitmap para mostrarla en un ImageView
        val resultBitmap = Bitmap.createBitmap(redObjects.cols(), redObjects.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(redObjects, resultBitmap)


        // Mostrar el resultado en un ImageView
        imageSegRojo.setImageBitmap(resultBitmap)

    }

    private fun processImage(bitmap: Bitmap?) {
        // Cargar imagen original
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)

        // Convertir la imagen de BGR a HSV
        val hsvMat = Mat()
        Imgproc.cvtColor(srcMat, hsvMat, Imgproc.COLOR_BGR2HSV)

        // Definir rango de colores para el rojo en HSV
        val lowerRed = Scalar(0.0, 100.0, 100.0)
        val upperRed = Scalar(10.0, 255.0, 255.0)

        // Crear una máscara para el color rojo
        val mask = Mat()
        Core.inRange(hsvMat, lowerRed, upperRed, mask)

        // Aplicar la máscara al canal rojo de la imagen original
        val redChannel = Mat()
        Core.bitwise_and(srcMat, srcMat, redChannel, mask)

        // Convertir la imagen resultante a Bitmap
        val outputBitmap = Bitmap.createBitmap(redChannel.cols(), redChannel.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(redChannel, outputBitmap)

        // Mostrar la imagen resultante en el ImageView
        imageSegRojo.setImageBitmap(outputBitmap)
    }

    private fun segmentacionColores(bitmap: Bitmap?){

        // Crear un objeto Mat a partir del Bitmap
        val imageMat = Mat(bitmap!!.height, bitmap.width, CvType.CV_8UC3)
        Utils.bitmapToMat(bitmap, imageMat)

        // Convertir la imagen de BGR a RGB
        Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2RGB)

        // Convertir la imagen de BGR a HSV
        val hsvMat = Mat()
        Imgproc.cvtColor(imageMat, hsvMat, Imgproc.COLOR_RGB2HSV)

        // Definir rangos de colores para rojo, verde, azul y amarillo
        val minRed = Scalar(0.0, 100.0, 100.0)
        val maxRed = Scalar(10.0, 255.0, 255.0)

        val minGreen = Scalar(40.0, 100.0, 100.0)
        val maxGreen = Scalar(80.0, 255.0, 255.0)

        val minBlue = Scalar(100.0, 100.0, 100.0)
        val maxBlue = Scalar(130.0, 255.0, 255.0)

        val minYellow = Scalar(20.0, 100.0, 100.0)
        val maxYellow = Scalar(40.0, 255.0, 255.0)

        // Crear máscaras para cada rango de color
        val maskRed = Mat()
        Core.inRange(hsvMat, minRed, maxRed, maskRed)

        val maskGreen = Mat()
        Core.inRange(hsvMat, minGreen, maxGreen, maskGreen)

        val maskBlue = Mat()
        Core.inRange(hsvMat, minBlue, maxBlue, maskBlue)

        val maskYellow = Mat()
        Core.inRange(hsvMat, minYellow, maxYellow, maskYellow)

        // Aplicar las máscaras a la imagen original
        val objectsRed = Mat()
        Core.bitwise_and(imageMat, imageMat, objectsRed, maskRed)

        val objectsGreen = Mat()
        Core.bitwise_and(imageMat, imageMat, objectsGreen, maskGreen)

        val objectsBlue = Mat()
        Core.bitwise_and(imageMat, imageMat, objectsBlue, maskBlue)

        val objectsYellow = Mat()
        Core.bitwise_and(imageMat, imageMat, objectsYellow, maskYellow)


        // Mostrar las imágenes en ImageView
        val resultBitmapRed = Bitmap.createBitmap(objectsRed.cols(), objectsRed.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(objectsRed, resultBitmapRed)
        imageSegRojo.setImageBitmap(resultBitmapRed)

        /*
        val resultBitmapGreen = Bitmap.createBitmap(objectsGreen.cols(), objectsGreen.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(objectsGreen, resultBitmapGreen)
        imageGreen.setImageBitmap(resultBitmapGreen)

        val resultBitmapBlue = Bitmap.createBitmap(objectsBlue.cols(), objectsBlue.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(objectsBlue, resultBitmapBlue)
        imageBlue.setImageBitmap(resultBitmapBlue)

        val resultBitmapYellow = Bitmap.createBitmap(objectsYellow.cols(), objectsYellow.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(objectsYellow, resultBitmapYellow)
        imageYellow.setImageBitmap(resultBitmapYellow)

         */

    }

    /**Se lanza una ventana para cargar una imagen y se
     * genera un uid unico para nombrar a la imagen de esa forma */
    private fun cargarImagen() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

/*
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.downloadIcon){
            Toast.makeText(baseContext, "Se va a descargar la imagen", Toast.LENGTH_SHORT).show()
            guardarImagenGaleria()
            return true
        }
        return super.onOptionsItemSelected(item)
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_principal, menu)
        return true
    }
 */
}