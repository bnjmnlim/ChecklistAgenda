package gdoge.checklistagenda;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import gdoge.checklistagenda.CalendarActivity.DBContract.CalendarContent;
import gdoge.checklistagenda.CalendarActivity.DBContract.AlarmReferences;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        CalendarDBHelper dbHelper = new CalendarDBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        //for startup, to reset alarms
        if (intent.getAction() != null && context != null) {
            Log.d("AlarmReceiver", "booting up");
            if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {

                Cursor cursor = db.query(AlarmReferences.TABLE_NAME,
                        new String[] {AlarmReferences.COLUMN_DATE, AlarmReferences.COLUMN_KEY, AlarmReferences.COLUMN_TIME},
                        null,
                        null,
                        null,
                        null,
                        null);
                int rc_index = cursor.getColumnIndex(AlarmReferences.COLUMN_KEY);
                int date_index = cursor.getColumnIndex(AlarmReferences.COLUMN_DATE);
                int time_index = cursor.getColumnIndex(AlarmReferences.COLUMN_TIME);
                Log.e("AlarmReceiver", "starting up");
                while(cursor.moveToNext()) {
                    Log.e("E", "alarm added");
                    if(!MyDateEntryRecyclerViewAdapter.setAlarm(context, db, cursor.getString(time_index),
                            cursor.getInt(rc_index), cursor.getInt(date_index))) {
                        db.delete(AlarmReferences.TABLE_NAME, AlarmReferences.COLUMN_KEY + " = ?", new String[] {cursor.getInt(rc_index) + ""});
                    }
                }
                cursor.close();
                return;
            }
        }

        //for when an alarm is actually received
        String date = intent.getExtras().getString(DateEntryFragment.DATE_KEY);
        int rc = intent.getExtras().getInt(DateEntryFragment.RC_KEY);

        Cursor cursor = db.query(CalendarContent.TABLE_NAME,
                new String[] {CalendarContent.COLUMN_CONTENT, CalendarContent.COLUMN_ALARMINFO},
                CalendarContent.COLUMN_REQUESTCODE + " = ?",
                new String[] {rc + ""},
                null,
                null,
                null);
        String content = "";
        String alarminfo = "";
        while(cursor.moveToNext()) {
            content = cursor.getString(cursor.getColumnIndex(CalendarContent.COLUMN_CONTENT));
            alarminfo = cursor.getString(cursor.getColumnIndex(CalendarContent.COLUMN_ALARMINFO));
        }
        cursor.close();

        ContentValues cv = new ContentValues();
        cv.put(CalendarContent.COLUMN_ALARMINFO, "0" + alarminfo.substring(1));
        db.update(CalendarContent.TABLE_NAME, cv, CalendarContent.COLUMN_REQUESTCODE + " = ?", new String[] {rc + ""});

        db.delete(AlarmReferences.TABLE_NAME,
                AlarmReferences.COLUMN_KEY + " = ?",
                new String[] {rc + ""});

        showNotification(context, CalendarActivity.class, "Reminder:", content, date, rc);
    }

    public void showNotification(Context context, Class<?> cls, String title, String content, String date, int rc) {
        Intent notificationIntent = new Intent(context, cls);
        notificationIntent.putExtra(CalendarActivity.START_DATE, date);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(cls);
        stackBuilder.addNextIntent(notificationIntent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(rc, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        Notification notification = builder.setContentTitle(title)
                .setContentText(content).setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_icon2_circle)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                .build();

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(rc, notification);
    }
}
