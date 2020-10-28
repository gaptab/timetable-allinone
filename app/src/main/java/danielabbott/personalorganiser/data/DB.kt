package danielabbott.personalorganiser.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import danielabbott.personalorganiser.Notifications
import java.io.File
import java.io.OutputStream

// All functions can throw exceptions
object DB {

    // ** Database versions **
    // 1: Original database schema
    // 2: Added TBL_NOTES, TBL_TAGS, TBL_NOTE_TAG
    // 3: Added reqCode to TBL_NOTIFICATIONS
    class DBHelper(context: Context) : SQLiteOpenHelper(
        context, "po.db", null,
        3 /*Increment whenever db structure changes*/
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS TBL_TIMETABLE (_id INTEGER PRIMARY KEY, " +
                        "name TEXT NOT NULL)"
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS TBL_TIMETABLE_EVENT (_id INTEGER PRIMARY KEY," +
                        "timetable_id INTEGER NOT NULL," +
                        "startTime INTEGER NOT NULL, duration INTEGER NOT NULL, days INTEGER NOT NULL CHECK(days <> 0)," +
                        "name TEXT NOT NULL, notes TEXT DEFAULT NULL, " +
                        "remind30Mins INT NOT NULL DEFAULT 0, remind1Hr INT NOT NULL DEFAULT 0," +
                        "remind2Hrs INT NOT NULL DEFAULT 0," +
                        "remindMorning INT NOT NULL DEFAULT 0," +
                        "goal_id INT DEFAULT NULL," +
                        "CONSTRAINT timetable_event_timetable" +
                        "    FOREIGN KEY (timetable_id)" +
                        "    REFERENCES TBL_TIMETABLE (_id)" +
                        "    ON DELETE CASCADE," +
                        "CONSTRAINT timetable_event_goal" +
                        "    FOREIGN KEY (goal_id)" +
                        "    REFERENCES TBL_GOAL (_id)" +
                        "    ON DELETE CASCADE)"
            )

            db.execSQL(
                // dateTime can be null
                // dateTime is the number of milliseconds since the epoch
                // Repeat:
                //      0 = Do not repeat
                //      1 = Repeat daily
                //      2 = Repeat every other day
                //      3 = Repeat weekly
                //      4 = Repeat monthly
                "CREATE TABLE IF NOT EXISTS TBL_TODO_LIST_TASK (_id INTEGER PRIMARY KEY," +
                        "dateTime INTEGER, has_time INT DEFAULT 1 NOT NULL, name TEXT NOT NULL, notes TEXT DEFAULT NULL, " +
                        "remind30Mins INT NOT NULL DEFAULT 0, remind1Hr INT NOT NULL DEFAULT 0," +
                        "remind2Hrs INT NOT NULL DEFAULT 0," +
                        "remindMorning INT NOT NULL DEFAULT 0, repeat INT NOT NULL DEFAULT 0," +
                        "goal_id INT DEFAULT NULL," +
                        "CONSTRAINT todo_list_task_goal" +
                        "    FOREIGN KEY (goal_id)" +
                        "    REFERENCES TBL_GOAL (_id)" +
                        "    ON DELETE CASCADE)"
            )


            db.execSQL(
                "CREATE TABLE IF NOT EXISTS TBL_GOAL (_id INTEGER PRIMARY KEY, name TEXT NOT NULL," +
                        "colour INT NOT NULL DEFAULT 16744576," +
                        "notes TEXT DEFAULT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS TBL_MILESTONE (_id INTEGER PRIMARY KEY," +
                        "name TEXT NOT NULL," +
                        "deadline INT," +
                        "goal_id INT NOT NULL," +
                        "CONSTRAINT milestone_goal" +
                        "    FOREIGN KEY (goal_id)" +
                        "    REFERENCES TBL_GOAL (_id)" +
                        "    ON DELETE CASCADE)"
            )


            db.execSQL(
                "CREATE TABLE IF NOT EXISTS TBL_PHOTO (_id INTEGER PRIMARY KEY, url TEXT NOT NULL, UNIQUE (url))"
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS TBL_TIMETABLE_EVENT_PHOTOS (event_id INTEGER, photo_id INT, PRIMARY KEY (event_id, photo_id)," +
                        "CONSTRAINT timetable_event_photos_timetable_event" +
                        "    FOREIGN KEY (event_id)" +
                        "    REFERENCES TBL_TIMETABLE_EVENT (_id)" +
                        "    ON DELETE CASCADE)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS TBL_TODO_LIST_TASK_PHOTOS (task_id INTEGER, photo_id INT, PRIMARY KEY (task_id, photo_id)," +
                        "CONSTRAINT todo_list_task_photos_todo_list_task" +
                        "    FOREIGN KEY (task_id)" +
                        "    REFERENCES TBL_TODO_LIST_TASK (_id)" +
                        "    ON DELETE CASCADE)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS TBL_GOAL_PHOTOS (goal_id INTEGER, photo_id INT, PRIMARY KEY (goal_id, photo_id)," +
                        "CONSTRAINT goal_photos_goal" +
                        "    FOREIGN KEY (goal_id)" +
                        "    REFERENCES TBL_GOAL (_id)" +
                        "    ON DELETE CASCADE)"
            )

            db.execSQL(
                // time_saved = System.currentTimeMillis when timer was last saved in DB
                // time is in seconds. if null then timer is stopped
                // paused = boolean. ignored if time=null
                "CREATE TABLE IF NOT EXISTS TBL_TIMER (_id INTEGER PRIMARY KEY, name TEXT DEFAULT NULL, " +
                        "initial_time INT NOT NULL," +
                        "time_saved INT NOT NULL, time INT DEFAULT NULL, paused INT)"
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS TBL_NOTIFICATIONS (_id INTEGER PRIMARY KEY, " +
                        "content TEXT NOT NULL, channel INT NOT NULL, task_or_event_id LONG NOT NULL," +
                        "time LONG NOT NULL, reqCode INT NOT NULL)"
            )
            db.execSQL("CREATE TABLE IF NOT EXISTS TBL_ALARM_REQ_CODES (_id INTEGER PRIMARY KEY)")

            db.execSQL("CREATE TABLE IF NOT EXISTS TBL_NOTES (_id INTEGER PRIMARY KEY, contents TEXT NOT NULL)")

            db.execSQL("CREATE TABLE IF NOT EXISTS TBL_TAGS (_id INTEGER PRIMARY KEY, tag TEXT NOT NULL COLLATE nocase)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS tag_index ON TBL_TAGS(tag)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS TBL_NOTE_TAG (tag_id INTEGER NOT NULL, note_id INTEGER NOT NULL," +
                        "PRIMARY KEY (tag_id, note_id)," +
                        "CONSTRAINT notes_tag_1 FOREIGN KEY (note_id) REFERENCES TBL_NOTES(_id) ON DELETE CASCADE," +
                        "CONSTRAINT notes_tag_2 FOREIGN KEY (tag_id) REFERENCES TBL_TAGS(_id) ON DELETE CASCADE" +
                        ")"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS note_index ON TBL_NOTE_TAG(note_id)")
        }

        override fun onOpen(db: SQLiteDatabase?) {
            db?.execSQL("PRAGMA foreign_keys = ON")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if(oldVersion < 3) {
                db.execSQL("ALTER TABLE TBL_NOTIFICATIONS ADD COLUMN reqCode INT NOT NULL DEFAULT 0")
            }
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if(newVersion < 3) {
                db.execSQL("DROP TABLE IF EXISTS TBL_NOTIFICATIONS")
                onCreate(db)
            }
        }

    }


    private var initDone = false
    private lateinit var dbHelper: DBHelper
    lateinit var context: Context

    // Opens the database file
    fun init(context_: Context) {
        if (initDone) {
            return
        }
        initDone = true

        context = context_.applicationContext
        dbHelper = DBHelper(context)
    }

    fun close() {
        dbHelper.close()
    }

    fun createNewTimetable(name: String): Long {

        // Open database for writing
        val db = dbHelper.writableDatabase

        // coumn name -> value
        val values = ContentValues().apply {
            put("name", name)
        }

        // returns id of the inserted row or throws an exception
        val newRowId = db?.insert("TBL_TIMETABLE", null, values)

        return newRowId!!
    }

    fun renameTimetable(id: Long, name: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", name)
        }
        db.update("TBL_TIMETABLE", values, "_id=?", arrayOf(id.toString()))
    }

    // Returns list of Pair<id,name>
    fun getTimetables(): List<Pair<Long, String>> {
        val db = dbHelper.readableDatabase

        // Columns to load
        val projection = arrayOf(BaseColumns._ID, "name")

        val cursor = db.query(
            "TBL_TIMETABLE",
            projection,
            null,
            null,
            null,
            null,
            null
        )

        val timetables = ArrayList<Pair<Long, String>>()

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))

            timetables.add(Pair<Long, String>(id, name))
        }
        cursor.close()
        return timetables
    }

    fun isDBEmpty(): Boolean {
        val db = dbHelper.readableDatabase
        arrayOf(
            "TBL_TIMETABLE",
            "TBL_TIMETABLE_EVENT",
            "TBL_TODO_LIST_TASK",
            "TBL_GOAL",
            "TBL_MILESTONE",
            "TBL_NOTES"
        ).forEach {

            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $it",
                arrayOf()
            )

            if (cursor.moveToNext()) {
                val count = cursor.getInt(cursor.getColumnIndexOrThrow("COUNT(*)"))
                if (count > 0) {
                    cursor.close()
                    return false
                }
            }
            cursor.close()
        }
        return true
    }


    // Probably the oldest timetable in the database
    // No guarantee of which timetable will be loaded
    fun getFirstTimetable(): Pair<Long, String> {
        val db = dbHelper.readableDatabase


        val cursor = db.rawQuery(
            "SELECT * FROM TBL_TIMETABLE LIMIT 1",
            null
        )

        if (cursor.moveToNext()) {
            val id =
                cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))

            cursor.close()
            return Pair<Long, String>(id, name)
        } else {
            cursor.close()
            throw Exception("No timetables")
        }

    }

    fun getTimetableName(id: Long): String {
        val db = dbHelper.readableDatabase


        val cursor = db.rawQuery(
            "SELECT name FROM TBL_TIMETABLE WHERE _id=?",
            arrayOf(id.toString())
        )

        if (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))

            cursor.close()
            return name
        } else {
            cursor.close()
            throw Exception("Record not found")
        }
    }

    fun clearTimetable(id: Long) {
        val db = dbHelper.writableDatabase

        db.delete("TBL_TIMETABLE_EVENT", "timetable_id=?", arrayOf(id.toString()))

    }

    fun getTimetableEvents(timetableId: Long): List<TimetableEvent> {
        val list = ArrayList<TimetableEvent>()

        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            "SELECT *,(SELECT colour FROM TBL_GOAL WHERE _id=goal_id) as gcol FROM TBL_TIMETABLE_EVENT WHERE timetable_id=?",
            arrayOf(timetableId.toString())
        )

        while (cursor.moveToNext()) {
            list.add(
                TimetableEvent(
                    cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("timetable_id")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("startTime")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("duration")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("days")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getStringOrNull(cursor.getColumnIndexOrThrow("notes")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("remind30Mins")) != 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("remind1Hr")) != 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("remind2Hrs")) != 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("remindMorning")) != 0,
                    cursor.getLong(cursor.getColumnIndexOrThrow("goal_id")),
                    cursor.getIntOrNull(cursor.getColumnIndexOrThrow("gcol"))
                )
            )
        }

        cursor.close()
        return list
    }

    fun cloneTimetable(oldId: Long, newName: String): Long {
        val newId = createNewTimetable(newName)

        val db = dbHelper.writableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM TBL_TIMETABLE_EVENT WHERE timetable_id=$oldId",
            arrayOf()
        )

        while (cursor.moveToNext()) {
            val values = ContentValues().apply {
                put("timetable_id", newId)
                put("startTime", cursor.getInt(cursor.getColumnIndexOrThrow("startTime")))
                put("duration", cursor.getInt(cursor.getColumnIndexOrThrow("duration")))
                put("days", cursor.getInt(cursor.getColumnIndexOrThrow("days")))
                put("name", cursor.getString(cursor.getColumnIndexOrThrow("name")))
                put("notes", cursor.getStringOrNull(cursor.getColumnIndexOrThrow("notes")))
                put("remind30Mins", cursor.getInt(cursor.getColumnIndexOrThrow("remind30Mins")))
                put("remind1Hr", cursor.getInt(cursor.getColumnIndexOrThrow("remind1Hr")))
                put("remind2Hrs", cursor.getInt(cursor.getColumnIndexOrThrow("remind2Hrs")))
                put("remindMorning", cursor.getInt(cursor.getColumnIndexOrThrow("remindMorning")))
                put("goal_id", cursor.getIntOrNull(cursor.getColumnIndexOrThrow("goal_id")))
            }
            db?.insert("TBL_TIMETABLE_EVENT", null, values)
        }

        cursor.close()
        return newId
    }

    fun getTimetableEvent(id: Long): TimetableEvent {
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM TBL_TIMETABLE_EVENT WHERE _id=?",
            arrayOf(id.toString())
        )

        if (cursor.moveToNext()) {
            val e = TimetableEvent(
                cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("timetable_id")),
                cursor.getInt(cursor.getColumnIndexOrThrow("startTime")),
                cursor.getInt(cursor.getColumnIndexOrThrow("duration")),
                cursor.getInt(cursor.getColumnIndexOrThrow("days")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getStringOrNull(cursor.getColumnIndexOrThrow("notes")),
                cursor.getInt(cursor.getColumnIndexOrThrow("remind30Mins")) != 0,
                cursor.getInt(cursor.getColumnIndexOrThrow("remind1Hr")) != 0,
                cursor.getInt(cursor.getColumnIndexOrThrow("remind2Hrs")) != 0,
                cursor.getInt(cursor.getColumnIndexOrThrow("remindMorning")) != 0,
                cursor.getLongOrNull(cursor.getColumnIndexOrThrow("goal_id"))

            )
            cursor.close()
            return e
        } else {
            cursor.close()
            throw Exception("Record not found")
        }

    }

    fun deleteTimetable(id: Long) {
        val db = dbHelper.writableDatabase

        db.delete("TBL_TIMETABLE", "_id=?", arrayOf(id.toString()))


    }

    fun updateOrCreateTimetableEvent(e: TimetableEvent): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("timetable_id", e.timetable_id)
            put("startTime", e.startTime)
            put("duration", e.duration)
            put("days", e.days)
            put("name", e.name)
            put("notes", e.notes?.ifBlank { null }?.trim())
            put("remind30Mins", if (e.remind30Mins) 1 else 0)
            put("remind1Hr", if (e.remind1Hr) 1 else 0)
            put("remind2Hrs", if (e.remind2Hrs) 1 else 0)
            put("remindMorning", if (e.remindMorning) 1 else 0)
            put("goal_id", e.goal_id)
        }

        return if (e.id < 0) {
            db?.insert("TBL_TIMETABLE_EVENT", null, values)!!
        } else {
            db.update("TBL_TIMETABLE_EVENT", values, "_id=?", arrayOf(e.id.toString()))
            e.id
        }
    }

    fun deleteTimetableEvent(eventId: Long) {
        val db = dbHelper.writableDatabase
        db.execSQL("DELETE FROM TBL_TIMETABLE_EVENT WHERE _id=$eventId")

    }

    fun getPhoto(photoUrl: String): Long? {
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM TBL_PHOTO WHERE url=?",
            arrayOf(photoUrl)
        )

        return if (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
            cursor.close()
            id
        } else {
            cursor.close()
            null
        }
    }

    fun addPhoto(photoUrl: String): Long {
        val values = ContentValues().apply {
            put("url", photoUrl)
        }
        return dbHelper.writableDatabase?.insert("TBL_PHOTO", null, values)!!
    }

    private fun addOrGetPhoto(photoUrl: String): Long {
        val id = getPhoto(photoUrl)
        if (id != null) {
            return id
        }
        return addPhoto(photoUrl)
    }

    fun addTimetableEventPhoto(eventId: Long, photoUrl: String) {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("event_id", eventId)
            put("photo_id", addOrGetPhoto(photoUrl))
        }

        db?.insert("TBL_TIMETABLE_EVENT_PHOTOS", null, values)


    }

    fun removeTimetableEventPhoto(eventId: Long, photoUrl: String) {
        val db = dbHelper.writableDatabase

        db.execSQL(
            "DELETE FROM TBL_TIMETABLE_EVENT_PHOTOS WHERE event_id=${eventId} AND photo_id=" +
                    "(SELECT _id FROM TBL_PHOTO WHERE url=?)", arrayOf(photoUrl)
        )

    }

    fun getTimetableEventPhotos(eventId: Long): List<String> {
        val list = ArrayList<String>()

        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            "SELECT url FROM TBL_PHOTO WHERE _id IN " +
                    "(SELECT photo_id FROM TBL_TIMETABLE_EVENT_PHOTOS WHERE event_id=$eventId)",
            arrayOf()
        )

        while (cursor.moveToNext()) {
            list.add(cursor.getString(cursor.getColumnIndexOrThrow("url")))
        }

        cursor.close()
        return list
    }


    fun getToDoListTasks(): List<ToDoListTaskListData> {
        val list = ArrayList<ToDoListTaskListData>()

        val db = dbHelper.readableDatabase


        val cursor = db.rawQuery(
            "SELECT *,(SELECT colour FROM TBL_GOAL WHERE _id=goal_id) AS gcol, (LENGTH(notes)) AS notesLength " +
                    "FROM TBL_TODO_LIST_TASK ORDER BY dateTime ASC",
            arrayOf()
        )


        while (cursor.moveToNext()) {
            list.add(
                ToDoListTaskListData(
                    cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getLongOrNull(cursor.getColumnIndexOrThrow("dateTime")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("has_time")) != 0,
                    cursor.getIntOrNull(cursor.getColumnIndexOrThrow("gcol")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("notesLength")) > 0
                )
            )
        }


        cursor.close()
        return list
    }


    fun getToDoListTasksNotificationData(): List<ToDoListTaskNotificationData> {
        val list = ArrayList<ToDoListTaskNotificationData>()

        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            "SELECT _id,dateTime,has_time,name,remind30Mins,remind1Hr,remind2Hrs,remindMorning,repeat" +
                    " FROM TBL_TODO_LIST_TASK",
            arrayOf()
        )

        while (cursor.moveToNext()) {
            list.add(
                ToDoListTaskNotificationData(
                    cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                    cursor.getLongOrNull(cursor.getColumnIndexOrThrow("dateTime")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("has_time")) != 0,
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("remind30Mins")) != 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("remind1Hr")) != 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("remind2Hrs")) != 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow("remindMorning")) != 0,
                    Repeat.fromInt(cursor.getInt(cursor.getColumnIndexOrThrow("repeat")))
                )
            )
        }


        cursor.close()
        return list
    }

    fun getToDoListTask(id: Long): ToDoListTask {
        val db = dbHelper.readableDatabase


        val cursor = db.rawQuery(
            "SELECT *, (SELECT colour FROM TBL_GOAL WHERE _id=goal_id) AS gcol FROM TBL_TODO_LIST_TASK WHERE _id=?",
            arrayOf(id.toString())
        )

        if (cursor.moveToNext()) {
            val e = ToDoListTask(
                cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                cursor.getLongOrNull(cursor.getColumnIndexOrThrow("dateTime")),
                cursor.getInt(cursor.getColumnIndexOrThrow("has_time")) != 0,
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getStringOrNull(cursor.getColumnIndexOrThrow("notes")),
                cursor.getInt(cursor.getColumnIndexOrThrow("remind30Mins")) != 0,
                cursor.getInt(cursor.getColumnIndexOrThrow("remind1Hr")) != 0,
                cursor.getInt(cursor.getColumnIndexOrThrow("remind2Hrs")) != 0,
                cursor.getInt(cursor.getColumnIndexOrThrow("remindMorning")) != 0,
                Repeat.fromInt(cursor.getInt(cursor.getColumnIndexOrThrow("repeat"))),
                cursor.getLongOrNull(cursor.getColumnIndexOrThrow("goal_id")),
                cursor.getIntOrNull(cursor.getColumnIndexOrThrow("gcol"))

            )
            cursor.close()
            return e
        } else {
            cursor.close()
            throw Exception("Record not found")
        }

    }

    fun updateOrCreateToDoListTask(e: ToDoListTask): Long {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("dateTime", e.dateTime)
            put("has_time", e.hasTime)
            put("name", e.name)
            put("notes", e.notes?.ifBlank { null }?.trim())
            put("remind30Mins", if (e.remind30Mins) 1 else 0)
            put("remind1Hr", if (e.remind1Hr) 1 else 0)
            put("remind2Hrs", if (e.remind2Hrs) 1 else 0)
            put("remindMorning", if (e.remindMorning) 1 else 0)
            put("repeat", e.repeat.n)
            put("goal_id", e.goal_id)
        }

        return if (e.id < 0) {
            db?.insert("TBL_TODO_LIST_TASK", null, values)!!
        } else {
            db.update("TBL_TODO_LIST_TASK", values, "_id=?", arrayOf(e.id.toString()))
            e.id
        }

    }

    fun deleteToDoListTask(eventId: Long) {
        val db = dbHelper.writableDatabase

        db.execSQL("DELETE FROM TBL_TODO_LIST_TASK WHERE _id=$eventId")

    }

    fun addToDoListTaskPhoto(eventId: Long, photoUrl: String) {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("task_id", eventId)
            put("photo_id", addOrGetPhoto(photoUrl))
        }

        db?.insert("TBL_TODO_LIST_TASK_PHOTOS", null, values)


    }

    fun removeToDoListTaskPhoto(eventId: Long, photoUrl: String) {
        val db = dbHelper.writableDatabase

        db.execSQL(
            "DELETE FROM TBL_TODO_LIST_TASK_PHOTOS WHERE task_id=${eventId} AND photo_id=" +
                    "(SELECT _id FROM TBL_PHOTO WHERE url=?)", arrayOf(photoUrl)
        )

    }

    fun getToDoListTaskPhotos(eventId: Long): List<String> {
        val list = ArrayList<String>()

        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            "SELECT url FROM TBL_PHOTO WHERE _id IN " +
                    "(SELECT photo_id FROM TBL_TODO_LIST_TASK_PHOTOS WHERE task_id=$eventId)",
            arrayOf()
        )

        while (cursor.moveToNext()) {
            list.add(cursor.getString(cursor.getColumnIndexOrThrow("url")))
        }

        cursor.close()
        return list
    }

    fun numberOfToDoListTasks(): Int {
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM TBL_TODO_LIST_TASK",
            arrayOf()
        )

        return if (cursor.moveToNext()) {
            val n = cursor.getInt(cursor.getColumnIndexOrThrow("COUNT(*)"))
            cursor.close()
            n
        } else {
            cursor.close()
            0
        }


    }

    fun getGoals(): List<GoalListData> {
        val list = ArrayList<GoalListData>()

        val db = dbHelper.readableDatabase


        val cursor = db.rawQuery(
            "SELECT _id,name,colour FROM TBL_GOAL",
            arrayOf()
        )

        while (cursor.moveToNext()) {
            list.add(
                GoalListData(
                    cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("colour"))
                )
            )
        }


        cursor.close()
        return list
    }

    fun getGoal(goalId: Long): Goal {
        val db = dbHelper.readableDatabase


        val cursor = db.rawQuery(
            "SELECT * FROM TBL_GOAL WHERE _id=?",
            arrayOf(goalId.toString())
        )

        if (cursor.moveToNext()) {
            val g = Goal(
                cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getInt(cursor.getColumnIndexOrThrow("colour")),
                cursor.getStringOrNull(cursor.getColumnIndexOrThrow("notes"))
            )

            cursor.close()
            return g
        } else {
            cursor.close()
            throw Exception("Record not found")
        }

    }

    fun getMilestones(goalId: Long): List<Milestone> {
        val list = ArrayList<Milestone>()

        val db = dbHelper.readableDatabase


        val cursor = db.rawQuery(
            "SELECT * FROM TBL_MILESTONE WHERE goal_id=? ORDER BY deadline",
            arrayOf(goalId.toString())
        )

        while (cursor.moveToNext()) {
            list.add(
                Milestone(
                    cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getLongOrNull(cursor.getColumnIndexOrThrow("deadline")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("goal_id"))
                )
            )
        }


        cursor.close()
        return list
    }

    fun updateOrCreateGoal(e: Goal): Long {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("name", e.name)
            put("colour", e.colour)
            put("notes", e.notes?.ifBlank { null }?.trim())
        }

        return if (e.id < 0) {
            db?.insert("TBL_GOAL", null, values)!!
        } else {
            db.update("TBL_GOAL", values, "_id=?", arrayOf(e.id.toString()))
            e.id
        }

    }


    fun changeGoalColour(id: Long, colour: Int) {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("colour", colour)
        }

        db.update("TBL_GOAL", values, "_id=?", arrayOf(id.toString()))
    }

    // If deleteEvents then all associated events/tasks get deleted
    // If false then they are assigned to no goal
    fun deleteGoal(goalId: Long, deleteEvents: Boolean) {
        val db = dbHelper.writableDatabase

        if (!deleteEvents) {
            val newGoalId: Long? = null
            val values = ContentValues().apply {
                put("goal_id", newGoalId)
            }
            try {
                db.update("TBL_TIMETABLE_EVENT", values, "goal_id=?", arrayOf(goalId.toString()))
            } catch (_: Exception) {
            }
            try {
                db.update("TBL_TODO_LIST_TASK", values, "goal_id=?", arrayOf(goalId.toString()))
            } catch (_: Exception) {
            }
        }

        db.execSQL("DELETE FROM TBL_GOAL WHERE _id=$goalId")

    }

    fun addGoalPhoto(goalId: Long, photoUrl: String) {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("goal_id", goalId)
            put("photo_id", addOrGetPhoto(photoUrl))
        }

        db?.insert("TBL_GOAL_PHOTOS", null, values)
    }

    fun removeGoalPhoto(goalId: Long, photoUrl: String) {
        val db = dbHelper.writableDatabase

        db.execSQL(
            "DELETE FROM TBL_GOAL_PHOTOS WHERE goal_id=${goalId} AND photo_id=" +
                    "(SELECT _id FROM TBL_PHOTO WHERE url=?)", arrayOf(photoUrl)
        )
    }

    fun getGoalPhotos(goalId: Long): List<String> {
        val list = ArrayList<String>()

        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            "SELECT url FROM TBL_PHOTO WHERE _id IN " +
                    "(SELECT photo_id FROM TBL_GOAL_PHOTOS WHERE goal_id=$goalId)",
            arrayOf()
        )

        while (cursor.moveToNext()) {
            list.add(cursor.getString(cursor.getColumnIndexOrThrow("url")))
        }

        cursor.close()
        return list
    }

    fun addMilestone(m: Milestone) {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("name", m.name)
            put("deadline", m.deadline)
            put("goal_id", m.goal_id)
        }

        db?.insert("TBL_MILESTONE", null, values)

    }

    fun removeMilestone(m: Milestone) {
        val db = dbHelper.writableDatabase

        db.execSQL("DELETE FROM TBL_MILESTONE WHERE _id=${m.id}")

    }

    fun updateMilestone(m: Milestone) {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("name", m.name)
            put("deadline", m.deadline)
        }

        db.update("TBL_MILESTONE", values, "_id=?", arrayOf(m.id.toString()))

    }

    // Optimise database
    private fun vacuum() {
        val db = dbHelper.writableDatabase
        db.execSQL("VACUUM")
    }


    fun goalHasAssociatedEventsOrTasks(goalId: Long): Boolean {
        val db = dbHelper.readableDatabase

        try {
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM TBL_TODO_LIST_TASK WHERE goal_id=$goalId",
                arrayOf()
            )

            if (cursor.moveToNext()) {
                if (cursor.getInt(cursor.getColumnIndexOrThrow("COUNT(*)")) > 0) {
                    cursor.close()
                    return true
                }
            }
            cursor.close()
        } finally {
        }

        val cursor2 = db.rawQuery(
            "SELECT COUNT(*) FROM TBL_TIMETABLE_EVENT WHERE goal_id=$goalId",
            arrayOf()
        )

        if (cursor2.moveToNext()) {
            if (cursor2.getInt(cursor2.getColumnIndexOrThrow("COUNT(*)")) > 0) {
                cursor2.close()
                return true
            }
        }

        cursor2.close()
        return false
    }

    fun allUsedGoalColours(): List<Int> {
        val db = dbHelper.readableDatabase
        val colours = ArrayList<Int>()

        val cursor = db.rawQuery(
            "SELECT colour FROM TBL_GOAL",
            arrayOf()
        )

        while (cursor.moveToNext()) {
            colours.add(cursor.getInt(cursor.getColumnIndexOrThrow("colour")) and 0xffffff)
        }

        cursor.close()
        return colours
    }

    fun optimise() {
        val db = dbHelper.writableDatabase

        // Get IDs of all unused photos
        val cursor = db.rawQuery(
            "SELECT _id FROM TBL_PHOTO WHERE _id " +
                    "NOT IN (SELECT photo_id FROM  TBL_TIMETABLE_EVENT_PHOTOS) " +
                    "AND _id NOT IN (SELECT photo_id FROM  TBL_TODO_LIST_TASK_PHOTOS) " +
                    "AND _id NOT IN (SELECT photo_id FROM  TBL_GOAL_PHOTOS)",
            arrayOf()
        )

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
            try {
                val file = File("${context.applicationContext.cacheDir}/img$id")
                file.delete()
            } finally {
            }
        }
        cursor.close()

        db.execSQL(
            "DELETE FROM TBL_PHOTO WHERE _id " +
                    "NOT IN (SELECT photo_id FROM  TBL_TIMETABLE_EVENT_PHOTOS) " +
                    "AND _id NOT IN (SELECT photo_id FROM  TBL_TODO_LIST_TASK_PHOTOS) " +
                    "AND _id NOT IN (SELECT photo_id FROM  TBL_GOAL_PHOTOS)",
            arrayOf()
        )


        vacuum()
    }

    fun getDBFileBytes(): ByteArray {
        close()
        val dbFile = context.getDatabasePath("po.db")

        val stream = dbFile.inputStream()
        val bytes = ByteArray(dbFile.length().toInt())
        stream.read(bytes)
        stream.close()
        return bytes
    }

    fun getOutputStream(): OutputStream {
        close()

        val dbFile = context.getDatabasePath("po.db")
        return dbFile.outputStream()
    }

    fun saveOrUpdateTimer(t: Timer): Long {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("name", t.name)
            put("initial_time", t.initialTime)
            put("time_saved", t.time_saved)
            put("time", t.time)
            put("paused", if (t.isPaused) 1 else 0)
        }

        return if (t.id == null) {
            db?.insert("TBL_TIMER", null, values)!!
        } else {
            db.update("TBL_TIMER", values, "_id=?", arrayOf(t.id.toString()))
            t.id
        }
    }

    fun getTimers(): List<Timer> {
        val db = dbHelper.readableDatabase

        // Columns to load
        val projection =
            arrayOf(BaseColumns._ID, "name", "initial_time", "time_saved", "time", "paused")

        val cursor = db.query(
            "TBL_TIMER",
            projection,
            null,
            null,
            null,
            null,
            null
        )

        val timers = ArrayList<Timer>()

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val initialTime = cursor.getInt(cursor.getColumnIndexOrThrow("initial_time"))
            val timeSaved = cursor.getLong(cursor.getColumnIndexOrThrow("time_saved"))
            val time = cursor.getIntOrNull(cursor.getColumnIndexOrThrow("time"))
            val paused = cursor.getInt(cursor.getColumnIndexOrThrow("paused")) != 0

            timers.add(Timer(id, name, time, initialTime, timeSaved, paused))
        }
        cursor.close()
        return timers
    }

    fun deleteTimer(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete("TBL_TIMER", "_id=?", arrayOf(id.toString()))
    }

    fun addNotification(n: NotificationData) {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("content", n.content)
            put("channel", n.channel.id_int)
            put("task_or_event_id", n.taskOrEventId)
            put("time", n.time)
            put("reqCode", n.reqCode)
        }

        db?.insert("TBL_NOTIFICATIONS", null, values)!!
    }

    fun getNotificationsToShow(reqCodeToDelete: Int? = null): List<NotificationData> {
        val db = dbHelper.writableDatabase

        val projection =
            arrayOf("content", "channel", "task_or_event_id", "time", "reqCode")

        val maxTime = (System.currentTimeMillis() + 30000).toString()

        val cursor = db.query(
            "TBL_NOTIFICATIONS",
            projection,
            "time <= ?",
            arrayOf(maxTime),
            null,
            null,
            null
        )

        val notifications = ArrayList<NotificationData>()

        while (cursor.moveToNext()) {
            val content = cursor.getString(cursor.getColumnIndexOrThrow("content"))
            val channel = cursor.getInt(cursor.getColumnIndexOrThrow("channel"))
            val task_or_event_id = cursor.getLong(cursor.getColumnIndexOrThrow("task_or_event_id"))
            val time = cursor.getLong(cursor.getColumnIndexOrThrow("time"))
            val reqCode = cursor.getInt(cursor.getColumnIndexOrThrow("reqCode"))

            notifications.add(
                NotificationData(
                    content,
                    Notifications.Channel.fromInt(channel),
                    task_or_event_id,
                    time,
                    reqCode
                )
            )
        }
        cursor.close()

        if(reqCodeToDelete != null) {
            db.delete("TBL_NOTIFICATIONS", "time <= ? OR reqCode=?", arrayOf(maxTime, reqCodeToDelete.toString()))
        }
        else {
            db.delete("TBL_NOTIFICATIONS", "time <= ?", arrayOf(maxTime))
        }

        return notifications
    }

    fun getActiveAlarmReqCodesAndRemove(channel: Int, id: Long) : List<Int> {
        val db = dbHelper.writableDatabase

        val projection =
            arrayOf("reqCode")

        val cursor = db.query(
            "TBL_NOTIFICATIONS",
            projection,
            "channel=? AND task_or_event_id=?",
            arrayOf(channel.toString(), id.toString()),
            null,
            null,
            null
        )

        val codes = ArrayList<Int>()

        while (cursor.moveToNext()) {
            codes.add(cursor.getInt(cursor.getColumnIndexOrThrow("reqCode")))
        }
        cursor.close()

        db.delete("TBL_NOTIFICATIONS", "channel=? AND task_or_event_id=?", arrayOf(channel.toString(), id.toString()))


        return codes
    }

    fun getActiveAlarmReqCodesForTaskAndRemove(id: Long) : List<Int> {
        return getActiveAlarmReqCodesAndRemove(1, id)
    }

    fun getActiveAlarmReqCodesForTTEventAndRemove(id: Long) : List<Int> {
        return getActiveAlarmReqCodesAndRemove(0, id)
    }

    fun getActiveAlarmReqCodes(): List<Int> {
        val db = dbHelper.readableDatabase

        val projection =
            arrayOf("reqCode")

        val cursor = db.query(
            "TBL_NOTIFICATIONS",
            projection,
            null,
            null,
            null,
            null,
            null
        )

        val codes = ArrayList<Int>()

        while (cursor.moveToNext()) {
            codes.add(cursor.getInt(cursor.getColumnIndexOrThrow("reqCode")))
        }
        cursor.close()

        return codes
    }

    fun clearNotifications() {
        val db = dbHelper.writableDatabase
        db.delete("TBL_NOTIFICATIONS", null, null)
    }

    fun disableAllReminders() {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("remind30Mins", 0)
            put("remind1Hr", 0)
            put("remind2Hrs", 0)
            put("remindMorning", 0)
        }
        db.update("TBL_TIMETABLE_EVENT", values, null, null)
        db.update("TBL_TODO_LIST_TASK", values, null, null)
    }

    fun getNotesPreviews(tag: Long?): List<NotePreview> {
        val list = ArrayList<NotePreview>()

        val db = dbHelper.readableDatabase


        val cursor = if (tag == null) {
            db.rawQuery(
                "SELECT _id,SUBSTR(contents,0,100) as contents_preview , length(contents) as contents_length FROM TBL_NOTES ORDER BY _id DESC",
                arrayOf()
            )
        } else {
            db.rawQuery(
                "SELECT _id,SUBSTR(contents,0,100) as contents_preview, length(contents) as contents_length FROM TBL_NOTES WHERE _id IN (SELECT note_id FROM TBL_NOTE_TAG WHERE tag_id = ?) ORDER BY _id DESC",
                arrayOf(tag.toString())
            )
        }


        while (cursor.moveToNext()) {
            list.add(
                NotePreview(
                    cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("contents_preview")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("contents_length"))
                )
            )
        }


        cursor.close()
        return list
    }

    fun getNotesPreviewsUntagged(): List<NotePreview> {
        val list = ArrayList<NotePreview>()

        val db = dbHelper.readableDatabase


        val cursor = db.rawQuery(
            "SELECT _id,SUBSTR(contents,0,100) as contents_preview, length(contents) as contents_length FROM TBL_NOTES WHERE _id NOT IN (SELECT note_id FROM TBL_NOTE_TAG) ORDER BY _id DESC",
            arrayOf()
        )


        while (cursor.moveToNext()) {
            list.add(
                NotePreview(
                    cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("contents_preview")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("contents_length"))
                )
            )
        }
        cursor.close()
        return list
    }

    fun getTags(): List<Tag> {
        val list = ArrayList<Tag>()

        val db = dbHelper.readableDatabase


        val cursor = db.rawQuery(
            "SELECT _id,tag FROM TBL_TAGS",
            arrayOf()
        )


        while (cursor.moveToNext()) {
            list.add(
                Tag(
                    cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("tag"))
                )
            )
        }

        cursor.close()

        return list
    }

    fun getNote(id: Long): Note {
        val db = dbHelper.readableDatabase


        val cursor = db.rawQuery(
            "SELECT contents FROM TBL_NOTES WHERE _id=?",
            arrayOf(id.toString())
        )

        if (!cursor.moveToNext()) {
            throw Exception("Record not found")
        }

        val contents = cursor.getString(cursor.getColumnIndexOrThrow("contents"))

        cursor.close()

        val cursor2 = db.rawQuery(
            "SELECT _id,tag FROM TBL_TAGS WHERE _id IN (SELECT tag_id FROM TBL_NOTE_TAG WHERE note_id=?)",
            arrayOf(id.toString())
        )

        val tags = ArrayList<Tag>()

        while (cursor2.moveToNext()) {
            tags.add(
                Tag(
                    cursor2.getLong(cursor2.getColumnIndexOrThrow("_id")),
                    cursor2.getString(cursor2.getColumnIndexOrThrow("tag"))
                )
            )
        }

        cursor2.close()

        return Note(id, contents, tags)
    }

    fun getNoteTagIDs(noteId: Long): List<Long> {
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            "SELECT tag_id FROM TBL_NOTE_TAG WHERE note_id=?",
            arrayOf(noteId.toString())
        )

        val tags = ArrayList<Long>()

        while (cursor.moveToNext()) {
            tags.add(
                cursor.getLong(cursor.getColumnIndexOrThrow("tag_id"))
            )
        }

        cursor.close()

        return tags
    }

    fun updateOrCreateNote(e: Note): Long {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put("contents", e.contents.trim())
        }

        return if (e.id < 0) {
            db?.insert("TBL_NOTES", null, values)!!
        } else {
            db.update("TBL_NOTES", values, "_id=?", arrayOf(e.id.toString()))
            e.id
        }

    }

    fun createOrGetTag(tag: String): Long {
        val db = dbHelper.writableDatabase

        val cursor = db.rawQuery(
            "SELECT _id FROM TBL_TAGS WHERE tag=?",
            arrayOf(tag.trim())
        )

        if (!cursor.moveToNext()) {
            // Add new tag

            return db?.insert("TBL_TAGS", null, ContentValues().apply {
                put("tag", tag.trim())
            })!!
        }

        val id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
        cursor.close()
        return id


    }

    fun renameTag(id: Long, newTagName: String) {
        val db = dbHelper.writableDatabase
        db.update("TBL_TAGS", ContentValues().apply {
            put("tag", newTagName.trim())
        }, "_id=?", arrayOf(id.toString()))
    }

    fun addTagToNote(noteId: Long, tagId: Long) {
        val db = dbHelper.writableDatabase
        db?.insert("TBL_NOTE_TAG", null, ContentValues().apply {
            put("note_id", noteId)
            put("tag_id", tagId)
        })!!
    }

    private fun removeTagIfUnused(tagId: Long) {
        val db = dbHelper.writableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) as n FROM TBL_NOTE_TAG WHERE tag_id=?",
            arrayOf(tagId.toString())
        )

        if (cursor.moveToNext() && cursor.getInt(cursor.getColumnIndexOrThrow("n")) < 1) {
            // Tag is unused

            db.execSQL("DELETE FROM TBL_TAGS WHERE _id=?", arrayOf(tagId.toString()))
        }

        cursor.close()
    }

    fun removeTagFromNote(noteId: Long, tagId: Long) {
        val db = dbHelper.writableDatabase
        db?.delete(
            "TBL_NOTE_TAG",
            "note_id=? AND tag_id=?",
            arrayOf(noteId.toString(), tagId.toString())
        )

        val cursor = db.rawQuery(
            "SELECT COUNT(*) as n FROM TBL_NOTE_TAG WHERE tag_id=?",
            arrayOf(tagId.toString())
        )

        if (cursor.moveToNext() && cursor.getInt(cursor.getColumnIndexOrThrow("n")) < 1) {
            // Tag is unused

            db.execSQL("DELETE FROM TBL_TAGS WHERE _id=?", arrayOf(tagId.toString()))
        }

        cursor.close()

    }

    fun deleteNote(noteId: Long) {
        val tags = getNoteTagIDs(noteId)

        val db = dbHelper.writableDatabase

        db?.delete("TBL_NOTE_TAG", "note_id=?", arrayOf(noteId.toString()))

        tags.forEach {
            removeTagIfUnused(it)
        }

        db?.delete("TBL_NOTES", "_id=?", arrayOf(noteId.toString()))

    }
}