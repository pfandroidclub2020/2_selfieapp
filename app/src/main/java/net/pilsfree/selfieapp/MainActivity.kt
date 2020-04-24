package net.pilsfree.selfieapp

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var imageUri : Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button1.setOnClickListener {
            withPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                // https://stackoverflow.com/a/10382217
                val cv = ContentValues()
                cv.put(MediaStore.Images.Media.TITLE, "Selfie Snap")
                cv.put(MediaStore.Images.Media.DESCRIPTION, "Selfie Snap")
                imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)

                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(intent, 1)
            }
        }

        button2.setOnClickListener {
            withPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                val path = MediaStore.Images.Media.
                    insertImage(contentResolver,imageView.drawable.toBitmap(),"selfie","selfie")
                val uri = Uri.parse(path)
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "image/png"
                intent.putExtra(Intent.EXTRA_STREAM,uri)
                val intentChooser = Intent.createChooser(intent,"Sdilet selfie")
                startActivity(intentChooser)
            }
        }

        bw.setOnClickListener {
            blackWhite()
        }
    }

    private fun blackWhite() {
        val bitmap = imageView.drawable.toBitmap().copy(Bitmap.Config.ARGB_8888, true)
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val color = bitmap.getPixel(x,y)
                // ARGB_8888 format
                // 11111111 10101011 00011101 11100011
                // [ ALPHA] [ RED  ] [ GREEN] [ BLUE ]

                val alpha = 0xFF
                val red = color.shr(16) and 0xFF
                // shr(16)
                // 00000000 00000000 11111111 10101011
                // and 0xFF
                // 00000000 00000000 00000000 11111111
                // =
                // 00000000 00000000 00000000 10101011

                val green = color.shr(8) and 0xFF
                val blue = color and 0xFF

                // https://www.tutorialspoint.com/dip/grayscale_to_rgb_conversion.htm
                val grey = (red.toFloat() * 0.3 + green.toFloat() * 0.59 + blue.toFloat() * 0.11).toInt()
                val newColor = alpha.shl(24) + grey.shl(16) + grey.shl(8) + grey
                bitmap.setPixel(x,y,newColor)
            }
        }
        imageView.setImageBitmap(bitmap)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && imageUri != null) {
            var preview = MediaStore.Images.Media.getBitmap(contentResolver,imageUri)
            // kouknem se jesli to nemame nahodou pretoceny
            if (preview.width > preview.height) {
                preview = preview.rotate(-90F)
            }
            // moc velky museli bysme predelavat filtry
            while (preview.width > 900) {
                preview = Bitmap.createScaledBitmap(preview,900,1200,false)
            }
            imageView.setImageBitmap(preview)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    // ohledne opravneni
    var lastRequestID = 1000
    private val finnishMap = mutableMapOf<Int,() -> Unit>()

    fun withPermission(permission:String, onFinnish : () -> Unit ) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            onFinnish()
        } else {
            lastRequestID++
            finnishMap[lastRequestID] = onFinnish
            ActivityCompat.requestPermissions(this, arrayOf(permission),lastRequestID)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            finnishMap[requestCode]?.invoke()
            finnishMap.remove(requestCode)
        }
    }
}
