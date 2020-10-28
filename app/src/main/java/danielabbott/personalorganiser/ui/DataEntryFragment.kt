package danielabbott.personalorganiser.ui

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.content.PermissionChecker
import danielabbott.personalorganiser.ImagePick
import danielabbott.personalorganiser.R
import danielabbott.personalorganiser.data.DB
import danielabbott.personalorganiser.data.GoalListData

// Base class for timetable,task,goal edit pages
// Implements functionality common to multiple pages ^
open class DataEntryFragment : DataEntryFragmentBasic() {

    companion object {
        const val TAG = "DataEntryFragment"
    }

    protected var newPhotos = ArrayList<String>()
    protected var imagesToRemove = ArrayList<String>()
    protected lateinit var picturePreviewsView: LinearLayout

    private lateinit var handler: Handler

    // Not used in goals fragment
    protected lateinit var goals: List<GoalListData>

    protected fun init(root: View) {
        handler = Handler {
            val img = it.obj as ImageView
            img.alpha = 0.0f
            picturePreviewsView.addView(img)
            ObjectAnimator.ofFloat(img, "alpha", 0.0f, 1.0f).apply {
                duration = 250
                start()
            }
            true
        }

        root.findViewById<Button>(R.id.bAddPicture).setOnClickListener {
            unsavedData = true
            ImagePick.pickImage(activity!!, this)
        }
    }

    protected fun initGoals(goal: Spinner) {
        // Populate list of goals
        goals = DB.getGoals()
        val testArray = Array<String>(goals.size + 1) { "[No assigned goal]" }
        var i = 1
        DB.getGoals().forEach {
            testArray[i] = it.name
            i += 1
        }
        goal.adapter = ArrayAdapter<String>(
            context!!,
            android.R.layout.simple_spinner_dropdown_item,
            testArray
        )
    }

    // Called when the user has picked an image from the gallery
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ImagePick.IMAGE_PICK_REQUST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null) {
                object : Thread() {
                    override fun run() {
                        addImage(uri)
                        unsavedData = true
                    }
                }.start()
                newPhotos.add(uri.toString())
            }

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == ImagePick.EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()
            && grantResults[0] == PermissionChecker.PERMISSION_GRANTED
        ) {
            // Permission has been granted, open the gallery app
            ImagePick.pickImage(activity!!, this, false)
        }
    }

    protected fun addImage(uri: Uri) {
        // Get/Create entry in database
        val photoId_ = DB.getPhoto(uri.toString())
        var photoId: Long
        if (photoId_ == null) {
            photoId = DB.addPhoto(uri.toString())
        } else {
            photoId = photoId_
        }

        // If we just created the entry in the database then a cache file does not exist
        // (Might not exist even if there was an entry in the DB)
        val useCacheFile = photoId_ != null

        // Create image view

        var img: ImageView
        try {
            img = ImagePick.createImage(context!!, uri, useCacheFile, photoId)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image file $uri: $e")
            return
        }

        // Set tap (view) and long tap (delete) handlers

        img.setOnLongClickListener {
            unsavedData = true
            imagesToRemove.add(uri.toString())
            newPhotos.remove(uri.toString())

            img.setOnLongClickListener(null)

            // Make image fade out and then remove
            ObjectAnimator.ofFloat(img, "alpha", 1.0f, 0.0f).apply {
                duration = 250
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        picturePreviewsView.removeView(img)
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                    }

                    override fun onAnimationStart(animation: Animator?) {
                    }

                })
                start()
            }
            true
        }
        imagesToRemove.remove(uri.toString())


        val msg = Message()
        msg.obj = img
        handler.sendMessage(msg)
    }

    fun setGoalSpinner(goal: Spinner, goalId: Long?) {
        goal.setSelection(0)
        if (goalId != null) {
            var i = 0
            goals.forEach {
                if (it.id == goalId) {
                    goal.setSelection(i + 1)
                }
                i += 1
            }
        }
    }

}