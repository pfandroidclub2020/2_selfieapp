package net.pilsfree.selfieapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.graphics.drawable.toBitmap
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button1.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
            startActivityForResult(intent,1)
        }

        button2.setOnClickListener {
            val path = MediaStore.Images.Media.
                insertImage(contentResolver,imageView.drawable.toBitmap(),"selfie","selfie")
            val uri = Uri.parse(path)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/png"
            intent.putExtra(Intent.EXTRA_STREAM,uri)
            val intentChooser = Intent.createChooser(intent,"Sdilet selfie")
            startActivity(intentChooser)
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
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val image = data!!.extras!!.get("data") as Bitmap
            imageView.setImageBitmap(image)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
