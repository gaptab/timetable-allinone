package danielabbott.personalorganiser

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import java.io.File

object ImagePick {

    const val EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 0
    const val IMAGE_PICK_REQUST_CODE = 0
    const val TAG = "ImagePick"

    // Does not open the gallery if permissions had to be requested - call the function
    // again in onRequestPermissionsResult
    fun pickImage(context: Context, fragment: Fragment, askForPermission: Boolean = true) {
        // Get permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(
                    context.applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PERMISSION_GRANTED
            ) {
                if (askForPermission) {
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    fragment.requestPermissions(
                        permissions,
                        EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE
                    )
                }
                return
            }
        }

        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        fragment.startActivityForResult(intent, IMAGE_PICK_REQUST_CODE, null)
    }

    // useCacheFile: Use cache file if available. If cache file does not exist, will be created.
    // photoId: Used as name of cache file
    fun createImage(context: Context, uri: Uri, useCacheFile: Boolean, photoId: Long): ImageView {
        val img = ImageView(context)

        img.adjustViewBounds = true

        val maxSize = (200 * context.resources.displayMetrics.density).toInt()
        img.maxWidth = maxSize
        img.maxHeight = maxSize

        var imageLoaded = false

        if (useCacheFile) {
            try {
                val cacheFile = File("${context.applicationContext.cacheDir}/img$photoId")
                if (cacheFile.exists()) {
                    val stream = cacheFile.inputStream()
                    var bitmap = BitmapDrawable(context.resources, stream).bitmap
                    img.setImageBitmap(bitmap)
                    imageLoaded = true
                    stream.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cache file for image: $e")
            }
        }

        if (!imageLoaded) {
            // Load and shrink image
            // Without shrinking, high resolution images will not display (OpenGL error in Android UI)

            val stream = context.contentResolver.openInputStream(uri)
            if (stream != null) {
                var bitmap = BitmapDrawable(context.resources, stream).bitmap

                if (bitmap.width > maxSize || bitmap.height > maxSize) {
                    bitmap = Bitmap.createScaledBitmap(
                        bitmap, maxSize,
                        (bitmap.height * (maxSize / bitmap.width.toFloat())).toInt(), true
                    )
                }

                try {
                    val cacheFile = File("${context.applicationContext.cacheDir}/img$photoId")
                    cacheFile.createNewFile()
                    val outStream = cacheFile.outputStream()
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outStream)) {
                        throw Exception("Bitmap compress error")
                    }
                    outStream.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving cache file for image: $e")
                }

                img.setImageBitmap(bitmap)
                stream.close()
            }
        }

        img.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        img.setPadding(5, 5, 10, 5)

        img.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(uri, "image/*")
            context.startActivity(intent)
        }

        // Set image rotation

        try {
            // https://stackoverflow.com/a/14066265/11498001

            val stream2 = context.contentResolver.openInputStream(uri)
            val exif = ExifInterface(stream2!!)
            val orientation: Int = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    img.rotation = 180.0f
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    img.rotation = 90.0f
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    img.rotation = 270.0f
                }
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                    img.scaleX = -1.0f
                }
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    img.scaleY = -1.0f
                }
            }
            stream2.close()
        } catch (_: Exception) {
        }

        return img
    }

}