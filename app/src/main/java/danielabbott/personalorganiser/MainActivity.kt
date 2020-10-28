package danielabbott.personalorganiser

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import danielabbott.personalorganiser.data.*
import danielabbott.personalorganiser.ui.OnBackPressed
import danielabbott.personalorganiser.ui.SettingsFragment
import danielabbott.personalorganiser.ui.goals.GoalsFragment
import danielabbott.personalorganiser.ui.notes.EditNoteFragment
import danielabbott.personalorganiser.ui.notes.NotesFragment
import danielabbott.personalorganiser.ui.timers.TimersFragment
import danielabbott.personalorganiser.ui.timetable.TimetableEditEventFragment
import danielabbott.personalorganiser.ui.timetable.TimetableFragment
import danielabbott.personalorganiser.ui.todolist.EditToDoListTaskFragment
import danielabbott.personalorganiser.ui.todolist.ToDoListFragment
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    companion object {
        const val CREATE_FILE_REQUEST_CODE = 357
        const val OPEN_FILE_REQUEST_CODE = 3572
    }

    private var menuVisible = false
    private lateinit var menu: ScrollView
    private lateinit var menuContainer: LinearLayout
    private lateinit var menuBlack: View

    private var animatorMenuTranslation: ObjectAnimator? = null
    private var animatorMenuBlackAlpha: ObjectAnimator? = null

    // d = time in milliseconds of animation
    private fun hideMenu() {
        // Animate window sliding to left
        val startPos = 0.0f
        val currentPos = menuContainer.translationX
        val endPos = -menu.layoutParams.width.toFloat()

        if (currentPos != endPos) {
            val t = (currentPos - startPos) / (endPos - startPos)
            val currentAlpha = 0.7f * (1.0f - t)
            val endAlpha = 0.0f
            val time = (1.0f - t) * 250.0f

            if (animatorMenuTranslation != null) {
                animatorMenuTranslation!!.cancel()
            }
            if (animatorMenuBlackAlpha != null) {
                animatorMenuBlackAlpha!!.cancel()
            }

            animatorMenuTranslation =
                ObjectAnimator.ofFloat(menuContainer, "translationX", currentPos, endPos).apply {
                    duration = time.toLong()
                    start()
                }
            // Make the black rectangle fade away
            animatorMenuBlackAlpha =
                ObjectAnimator.ofFloat(menuBlack, "alpha", currentAlpha, endAlpha).apply {
                    duration = time.toLong()
                    start()
                }
        }
        menuVisible = false
    }

    // Opposite animations of hideMenu
    private fun showMenu() {
        val startPos = -menu.layoutParams.width.toFloat()
        val currentPos = menuContainer.translationX
        val endPos = 0.0f
        if (currentPos != endPos) {
            val t = (currentPos - startPos) / (endPos - startPos)
            val currentAlpha = 0.7f * t
            val endAlpha = 0.7f
            val time = (1.0f - t) * 300.0f

            if (animatorMenuTranslation != null) {
                animatorMenuTranslation!!.cancel()
            }
            if (animatorMenuBlackAlpha != null) {
                animatorMenuBlackAlpha!!.cancel()
            }

            animatorMenuTranslation =
                ObjectAnimator.ofFloat(menuContainer, "translationX", currentPos, endPos).apply {
                    duration = time.toLong()
                    start()
                }
            animatorMenuBlackAlpha =
                ObjectAnimator.ofFloat(menuBlack, "alpha", currentAlpha, endAlpha).apply {
                    duration = time.toLong()
                    start()
                }
        }
        menuVisible = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        DB.init(applicationContext)


        /*val testttid = DB.createNewTimetable("chungus")
        for(i in 1..50) {
            var e = TimetableEvent(
                -1,
                testttid,
                (Math.random() * 23*60).toInt(),
                60,
                    (Math.random() * 127).toInt(),
                "a"+Math.random().toFloat(),
                null,
                Math.random() < 0.5,
                Math.random() < 0.5,
                Math.random() < 0.5,
                Math.random() < 0.5,
                null,
                null
            )
            DB.updateOrCreateTimetableEvent(e)
        }
        Notifications.scheduleAllNotifications(this)*/

        /*for(i in 1..50) {
            var e = ToDoListTask(
                -1,
                System.currentTimeMillis()+(Math.random() * 10000000).toLong(),
                true,
                "uwu" + Math.random().toFloat(),
                null,
                Math.random() < 0.5,
                Math.random() < 0.5,
                Math.random() < 0.5,
                Math.random() < 0.5,
                Repeat.fromInt((Math.random() * 4.9).toInt()),
                null,
                null
            )
            DB.updateOrCreateToDoListTask(e)
        }
        Notifications.scheduleAllNotifications(DB.context)*/




        menu = findViewById<ScrollView>(R.id.menu)
        menuContainer = findViewById<LinearLayout>(R.id.menuContainer)
        menuBlack = findViewById<View>(R.id.menuBlack)

        menuContainer.translationX = -menu.layoutParams.width.toFloat()

        findViewById<ImageButton>(R.id.bHamburgerIcon).setOnClickListener {
            // If an EditText is selected this stops the blob cursor thingy showing above the menu
            currentFocus?.clearFocus()

            hideKeyboard()
            if (menuVisible) {
                hideMenu()
            } else {
                showMenu()
            }
        }

        // Menu buttons
        findViewById<LinearLayout>(R.id.bMenuToDoList).setOnClickListener {
            menuButtonPressed {
                Settings.setLastPage(this, 0)
                switchToFragment(ToDoListFragment())
            }
        }
        findViewById<LinearLayout>(R.id.bMenuTimetable).setOnClickListener {
            menuButtonPressed {
                Settings.setLastPage(this, 1)
                switchToFragment(TimetableFragment())
            }
        }
        findViewById<LinearLayout>(R.id.bMenuGoals).setOnClickListener {
            menuButtonPressed {
                Settings.setLastPage(this, 2)
                switchToFragment(GoalsFragment())
            }
        }
        findViewById<LinearLayout>(R.id.bMenuTimers).setOnClickListener {
            menuButtonPressed {
                Settings.setLastPage(this, 3)
                switchToFragment(TimersFragment())
            }
        }
        findViewById<LinearLayout>(R.id.bMenuNotes).setOnClickListener {
            menuButtonPressed {
                Settings.setLastPage(this, 4)
                switchToFragment(NotesFragment())
            }
        }
        findViewById<LinearLayout>(R.id.bMenuSettings).setOnClickListener {
            hideMenu()
            goToSettingsPage()
        }

        fun loadLastPage() {
            when (Settings.getLastPage(this)) {
                0 -> switchToFragment(ToDoListFragment())
                1 -> switchToFragment(TimetableFragment())
                2 -> switchToFragment(GoalsFragment())
                3 -> switchToFragment(TimersFragment())
                else -> switchToFragment(NotesFragment())
            }
        }



        Notifications.createChannels(this)

        val lastAppStartTime = Settings.appStarted(this)
        if(System.currentTimeMillis() - lastAppStartTime > 6*24*60*60*1000) {
            Notifications.scheduleAllNotifications(this)
            enableWeeklyNotificationReschedule()
        }


        if (intent == null || intent.extras == null) {
            // Activity started normally

            loadLastPage()

        } else {
            if(intent.action == Intent.ACTION_SEND) {
                if(intent.type == "text/plain") {
                    // Launched from a different app to create a new note

                    // Go to notes page
                    Settings.setLastPage(this, 4)
                    switchToFragment(NotesFragment())

                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)


                    // Go to edit notes page
                    if(text == null) {
                        switchToFragment(EditNoteFragment(null), true)
                    }
                    else {
                        switchToFragment(EditNoteFragment(null, text), true)
                    }

                }
                else {
                    loadLastPage()
                }
            }
            else {

                val type = intent.extras!!.getString("WHAT_LOAD")
                val id = intent.extras!!.getLong("TASK_EVENT_ID")

                if (type == null) {
                    // Activity started normally
                    loadLastPage()
                } else {
                    // Activity started from the user tapping a notification

                    hideKeyboard()

                    if (type == "TODO") {
                        Settings.setLastPage(this, 0)
                        switchToFragment(ToDoListFragment())
                        switchToFragment(EditToDoListTaskFragment(id), true)
                    } else {
                        // Go to timetable page then to edit page so back/save buttons take the user back to the timetable
                        Settings.setLastPage(this, 1)
                        switchToFragment(TimetableFragment())
                        switchToFragment(
                            TimetableEditEventFragment(
                                Settings.getActiveTimetable(this),
                                id
                            ),
                            true
                        )
                    }
                }
            }
        }




    }

    private fun menuButtonPressed(onPress: () -> Unit) {
        hideMenu()

        runFragmentBackHandler(onPress)
    }

    // * not actually weekly
    // Once every 6 days all notifications are reloaded
    // Without this, if the app was not started for a long period of time then notifications
    // would never get scheduled
    private fun enableWeeklyNotificationReschedule() {
        val intent = Intent(applicationContext, NotifResched::class.java)
        val intent2 = PendingIntent.getBroadcast(
            applicationContext,
            -1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val t = (6 * 24 * 60 * 60 * 1000).toLong()
        alarmMgr.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + t,
            t,
            intent2
        )
    }

    private lateinit var settingsMenuOption: MenuItem

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        if (menu != null) {
            settingsMenuOption = menu.add("Settings")!!
        }
        return true
    }

    private fun goToSettingsPage() {
        hideKeyboard()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentView, SettingsFragment()).addToBackStack(null)
        fragmentTransaction.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        if (item == settingsMenuOption) {
            goToSettingsPage()
        }
        return false
    }

    private fun switchToFragment(f: Fragment, addToBackStack: Boolean = false) {
        if (!addToBackStack) {
            // Wipe history
            for (i in 1..supportFragmentManager.backStackEntryCount) {
                supportFragmentManager.popBackStack()
            }
        }

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentView, f)
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null)
        }
        fragmentTransaction.commit()
    }

    fun setToolbarTitle(s: String) {
        (findViewById<TextView>(R.id.tvTitle)).text = s
    }

    // If menu is open and somewhere not on the menu is tapped then hide the menu
    // and stop the touch event from reaching the UI behind the menu
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event != null && menuVisible) {
            val x = event.x
            var menuPos = IntArray(2)
            menu.getLocationInWindow(menuPos)
            if (x >= menuPos[0] && x < menuPos[0] + menu.layoutParams.width
            ) {
                // Menu pressed
            } else {
                hideMenu()
                return true
            }
        }
        return super.dispatchTouchEvent(event)
    }

    // Calls the onBackPressed function in the active fragment (if it has one)
    // If there are no unsaved changes or the user chooses to discard them
    // then the onNoChangesOrDiscardChanges lambda is called
    private fun runFragmentBackHandler(onNoChangesOrDiscardChanges: () -> Unit) {
        if (supportFragmentManager.fragments.count() < 1) {
            onNoChangesOrDiscardChanges()
            return
        }
        val fragment = supportFragmentManager.fragments[0]
        if (fragment != null && fragment is OnBackPressed) {
            (fragment as OnBackPressed).onBackPressed(onNoChangesOrDiscardChanges)
        } else {
            onNoChangesOrDiscardChanges()
        }
    }

    override fun onBackPressed() {
        runFragmentBackHandler { super.onBackPressed() }
    }

    // https://stackoverflow.com/a/26911627/11498001
    fun hideKeyboard() {
        val inputManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // check if no view has focus:
        val currentFocusedView = currentFocus
        if (currentFocusedView != null) {
            inputManager.hideSoftInputFromWindow(
                currentFocusedView.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }

    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?
    ) {
        if (resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            if (requestCode == CREATE_FILE_REQUEST_CODE) {
                val uri = resultData.data!!

                try {
                    contentResolver.openFileDescriptor(uri, "w").use {
                        if (it?.fileDescriptor != null) {
                            FileOutputStream(it.fileDescriptor).use { fos ->
                                fos.write(
                                    DB.getDBFileBytes()
                                )
                            }
                        }
                    }
                } finally {

                }
            } else if (requestCode == OPEN_FILE_REQUEST_CODE) {
                val uri = resultData.data!!

                DB.getOutputStream().use { out ->
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        while (inputStream.available() > 0) {
                            val bytes = ByteArray(inputStream.available())
                            inputStream.read(bytes)
                            out.write(bytes)
                        }
                    }
                }

                recreate()
            } else {
                super.onActivityResult(requestCode, resultCode, resultData)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, resultData)
        }
    }

}
