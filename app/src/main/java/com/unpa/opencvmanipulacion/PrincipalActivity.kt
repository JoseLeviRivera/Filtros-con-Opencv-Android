package com.unpa.opencvmanipulacion

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc


class PrincipalActivity : AppCompatActivity() {

    /**Declaracion de la ImageViews para mostrar los filtros con openCV*/
    private lateinit var imageOriginal: ImageView
    private lateinit var imageGris: ImageView
    private lateinit var imageRojoSeg: ImageView
    private lateinit var imageAzulSeg: ImageView
    private lateinit var imageVerdeSeg: ImageView
    private lateinit var imageRojo: ImageView
    private lateinit var imageAzul: ImageView
    private lateinit var imageVerde: ImageView
    private lateinit var imageBinaria: ImageView
    private lateinit var imageContorno: ImageView
    private lateinit var imageSegRojo: ImageView
    private lateinit var imageRotar: ImageView
    private lateinit var imagePrescalada: ImageView
    private lateinit var imageTrasladada: ImageView

    /**Variables para ver las respuestas de onResult*/
    private val REQUEST_IMAGE_CAPTURE = 1
    private val CAMERA_PERMISSION_REQUEST_CODE = 2
    private val STORAGE_PERMISSION_REQUEST_CODE = 3

    /**Declaracion de variable uri, se utiliza para pasarle la referencia a la imageView Original*/
    var imageUri: Uri? = null


    /**Metodo que se encarga de cargar una imagen de forma local utilizando pickmedia api > 33*/
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()){ uri ->
        if (uri != null){
            imageOriginal.setImageURI(uri)
            val imageBitmap = getBitmapFromUri(uri)
            processFilter(imageBitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        // Se carga la libreria de OpenCV
        OpenCVLoader.initDebug()

        title = "¡Bienvenido!"  //Agrega un texto al toolbar
        val toolbar: Toolbar = findViewById(R.id.materialToolbar)  //Referencia del toolbar
        setSupportActionBar(toolbar) //Agrega el toolbar declarado como default a la actividad
        val titleTextView = toolbar.getChildAt(0) as? TextView  // Obtén una referencia al TextView del título del Toolbar
        titleTextView?.setTextColor(ContextCompat.getColor(this, R.color.white)) // Cambia el color del texto del título

        //Incializacion de las instancias de las referencias de los ImageViews que se van a usar
        imageOriginal = findViewById(R.id.ImgOriginal)
        imageGris = findViewById(R.id.grises)
        imageRojo = findViewById(R.id.rojo)
        imageAzul = findViewById(R.id.azul)
        imageVerde = findViewById(R.id.verde)
        imageBinaria = findViewById(R.id.binaria)
        imageContorno = findViewById(R.id.contornos)
        imageRotar = findViewById(R.id.rotarImg)
        imagePrescalada = findViewById(R.id.preEscalada)
        imageTrasladada = findViewById(R.id.trasladada)
        imageRojoSeg = findViewById(R.id.segrojo)
        imageAzulSeg = findViewById(R.id.segazul)
        imageVerdeSeg = findViewById(R.id.segverde)

        /*** Boton carga una de forma local con pickmedia*/
        val btnCargarImagen = findViewById<Button>(R.id.btnLoad)
        btnCargarImagen.setOnClickListener {
            cargarImagen() //Carga imagen
        }

        /**Boton para abrir la camara y tomar captura */
        val buttonCapture: Button = findViewById(R.id.btnCamera)
        buttonCapture.setOnClickListener {
            if (checkCameraPermission()) {  //Se verifica que tenga los permisos de camara
                captureImage()   //Captura la imagen con este metodo
            } else {
                requestCameraPermission() //Lanza permiso y verificacion
            }
        }
    }

    /**Funcion para procesar todos los filtros de opencv */
    private fun processFilter(imageBitmap: Bitmap?){
        convertirEscalaGris(imageBitmap)
        showSeparatedChannels(imageBitmap)
        convertirTresCanalesRGBToGray(imageBitmap)
        convertirImgBinaria(imageBitmap)
        dibujarContornos(imageBitmap)
        rotarImagen(imageBitmap)
        preScaleImage(imageBitmap)
        preTranslateImage(imageBitmap)
    }

    /**Metodo usado para verificar los permisos de cámara*/
    private fun checkCameraPermission(): Boolean {
        val resultCamera = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )
        return resultCamera == PackageManager.PERMISSION_GRANTED
    }

    /**Es un método utilizado para solicitar permiso para acceder a la cámara del dispositivo.*/
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * La función onRequestPermissionsResult() es un método de devolución de llamada que se invoca cuando el usuario responde a una solicitud de permiso en tiempo de ejecución. Esta función se utiliza
     * para manejar la respuesta del usuario y tomar acciones en consecuencia.
     * 1. override fun onRequestPermissionsResult(...): Esta línea indica que el método onRequestPermissionsResult() está
     * siendo anulado (sobrescrito) de una superclase.
     * 2. super.onRequestPermissionsResult(requestCode, permissions, grantResults): Llama al método onRequestPermissionsResult() de la superclase para realizar cualquier procesamiento adicional necesario.
     * Es importante incluir esta línea para garantizar que la funcionalidad predeterminada relacionada con los permisos se mantenga.
     * 3. if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) { ... }: Comprueba si el código de solicitud coincide con el código utilizado para solicitar el permiso de la cámara. Si coincide, se realiza
     * el siguiente bloque de código.
     * 4. if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) { ... }: Comprueba si hay resultados de concesión de permisos y si el primer resultado es PackageManager.PERMISSION_GRANTED (permiso concedido).
     * Si se cumple esta condición, se realiza el siguiente bloque de código, que en este caso invoca la función captureImage() para capturar la imagen.
     * 5. else { ... }: Si el permiso de la cámara ha sido denegado, se muestra un mensaje de tostada (Toast.makeText(...)) informando al usuario que no se puede capturar la imagen.
     *6. La siguiente parte del código realiza una verificación similar para el permiso de almacenamiento, específicamente para Android 11 (API nivel 30) o superior. Si se ha denegado el permiso de escritura en almacenamiento,
     * se muestra un mensaje de tostada informando al usuario que no se puede guardar la imagen.
     * */
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

    /**
     * La función captureImage() se utiliza para iniciar la actividad de captura de imágenes
     * del dispositivo mediante la cámara.
     * */
    private fun captureImage() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    /**
     * La función onActivityResult() se utiliza para recibir el resultado de una actividad que ha
     * sido iniciada mediante startActivityForResult()
     * */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) { //Si se hace la captura
            val imageBitmap = data?.extras?.get("data") as Bitmap? //Se declara un dato de tipo bitmap
            imageOriginal.setImageBitmap(imageBitmap) //Se establece en la imagenOriginal de ImageView
           processFilter(imageBitmap) //Se pasa el dato bitmap para procesar los filtros de opencv
        }
    }

    /**Funcion que convierte una uri a Bitmap */
    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**Funcion que convierte a escala de grises*/
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

    /**Funcion que se usar para convertir los tres canales a grises */
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
        imageRojoSeg.setImageBitmap(redBitmap)

        val greenBitmap = Bitmap.createBitmap(channels[1].cols(), channels[1].rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(channels[1], greenBitmap)
        imageVerdeSeg.setImageBitmap(greenBitmap)

        val blueBitmap = Bitmap.createBitmap(channels[0].cols(), channels[0].rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(channels[0], blueBitmap)
        imageAzulSeg.setImageBitmap(blueBitmap)
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

    /** Funcion para convertir una imagen binaria en Android
     * La función convertirImgBinaria() se encarga de convertir una imagen en escala de grises a una
     * imagen binaria, donde los píxeles se clasifican en blanco o negro según un umbral específico.
     * */
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

    /**Funcion que se usa para dibujar los contornos de una imagen */
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

    /**Funcion que se usa para rotar una imagen */
    private fun rotarImagen(bitmap: Bitmap?){
        // Cargar imagen original
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)
        // Obtener el tamaño de la imagen
        val size = Point(srcMat.cols().toDouble(), srcMat.rows().toDouble())
        // Obtener el tamaño de la imagen
        val tam = srcMat.size()

        // Definir la matriz de transformación para la rotación
        val rotationMatrix = Imgproc.getRotationMatrix2D(size, 45.0, 1.0)

        // Realizar la rotación de la imagen
        val rotatedMat = Mat()
        Imgproc.warpAffine(srcMat, rotatedMat, rotationMatrix, tam, Imgproc.INTER_LINEAR)

        // Convertir la imagen resultante a Bitmap
        val outputBitmap = Bitmap.createBitmap(rotatedMat.cols(), rotatedMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rotatedMat, outputBitmap)

        // Mostrar la imagen resultante en el ImageView
        imageRotar.setImageBitmap(outputBitmap)

    }

    /**Funcion para preTrasladar una imagen */
    private fun preTranslateImage(bitmap: Bitmap?) {
        // Cargar imagen original
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)
        // Obtener el tamaño de la imagen
        val size = Point(srcMat.cols().toDouble(), srcMat.rows().toDouble())
        // Obtener el tamaño de la imagen
        val tam = srcMat.size()
        // Definir la matriz de transformación para el desplazamiento
        val preTranslationMatrix = Mat.zeros(2, 3, CvType.CV_32FC1)
        preTranslationMatrix.put(0, 0, 1.0, 0.0, 100.0)
        preTranslationMatrix.put(1, 0, 0.0, 1.0, 50.0)
        // Realizar el desplazamiento previo de la imagen
        val preTranslatedMat = Mat()
        Imgproc.warpAffine(srcMat, preTranslatedMat, preTranslationMatrix, tam, Imgproc.INTER_LINEAR)
        // Convertir la imagen resultante a Bitmap
        val preTranslatedBitmap = Bitmap.createBitmap(preTranslatedMat.cols(), preTranslatedMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(preTranslatedMat, preTranslatedBitmap)
        // Mostrar la imagen resultante en el ImageView
        imageTrasladada.setImageBitmap(preTranslatedBitmap)
    }

    /**Funcion para pre-Escalar una imagen usando opencv en android*/
    private fun preScaleImage(bitmap: Bitmap?) {
        // Cargar imagen original
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)
        // Obtener el tamaño de la imagen
        val size = Point(srcMat.cols().toDouble(), srcMat.rows().toDouble())
        // Obtener el tamaño de la imagen
        val tam = srcMat.size()
        // Definir la matriz de transformación para el escalado
        val preScalingMatrix = Mat.zeros(2, 3, CvType.CV_32FC1)
        preScalingMatrix.put(0, 0, 0.5, 0.0, 0.0)
        preScalingMatrix.put(1, 0, 0.0, 0.5, 0.0)
        // Realizar el escalado previo de la imagen
        val preScaledMat = Mat()
        Imgproc.warpAffine(srcMat, preScaledMat, preScalingMatrix, tam, Imgproc.INTER_LINEAR)
        // Convertir la imagen resultante a Bitmap
        val preScaledBitmap = Bitmap.createBitmap(preScaledMat.cols(), preScaledMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(preScaledMat, preScaledBitmap)
        // Mostrar la imagen resultante en el ImageView
        imagePrescalada.setImageBitmap(preScaledBitmap)
    }

    /**Se lanza una ventana para cargar una imagen y se
     * genera un uid unico para nombrar a la imagen de esa forma */
    private fun cargarImagen() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.salir){
            showExitConfirmationDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_principal, menu)
        return true
    }

    /**Funcion para separa los canales RGB y mostrar en los imagesViews*/
    private fun showSeparatedChannels(bitmap: Bitmap?) {
        val width = bitmap!!.width
        val height = bitmap.height

        // Crear bitmaps para cada canal
        val redBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val greenBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val blueBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Obtener los píxeles de la imagen original
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Recorrer cada píxel y asignar los valores de los canales a los bitmaps correspondientes
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)

            redBitmap.setPixel(i % width, i / width, Color.rgb(red, 0, 0))
            greenBitmap.setPixel(i % width, i / width, Color.rgb(0, green, 0))
            blueBitmap.setPixel(i % width, i / width, Color.rgb(0, 0, blue))
        }

        // Mostrar los bitmaps de los canales en ImageViews separados
        val redImageView: ImageView = findViewById(R.id.rojo)
        redImageView.setImageBitmap(redBitmap)

        val greenImageView: ImageView = findViewById(R.id.verde)
        greenImageView.setImageBitmap(greenBitmap)

        val blueImageView: ImageView = findViewById(R.id.azul)
        blueImageView.setImageBitmap(blueBitmap)
    }

    private fun showExitConfirmationDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Salir")
        alertDialogBuilder.setMessage("¿Estás seguro de que deseas salir?")
        alertDialogBuilder.setPositiveButton("Sí") { _, _ ->
            // Finalizar todas las actividades
            finishAffinity()
            // Cerrar la aplicación
            System.exit(0)
        }
        alertDialogBuilder.setNegativeButton("No", null)
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
}
