package gdoge.checklistagenda;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CalendarDBHelper extends SQLiteOpenHelper {
    public static final int DB_VERSION = 1;
    public static final String DB_NAME = "calendar.db";

    public CalendarDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CalendarActivity.DBContract.cEntry_dbcreate);
        db.execSQL(CalendarActivity.DBContract.cContent_dbcreate);
        db.execSQL(CalendarActivity.DBContract.cAlarms_dbcreate);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(CalendarActivity.DBContract.cContent_dbdelete);
        db.execSQL(CalendarActivity.DBContract.cEntry_dbdelete);
        db.execSQL(CalendarActivity.DBContract.cAlarms_dbdelete);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
