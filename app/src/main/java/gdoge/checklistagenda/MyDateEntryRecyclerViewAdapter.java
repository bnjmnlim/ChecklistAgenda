package gdoge.checklistagenda;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import gdoge.checklistagenda.DateEntryFragment.CalendarEntryInfo;
import gdoge.checklistagenda.CalendarActivity.DBContract.AlarmReferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MyDateEntryRecyclerViewAdapter extends RecyclerView.Adapter<MyDateEntryRecyclerViewAdapter.ViewHolder> {

    private final List<CalendarEntryInfo> mValues;
    private DateEntryFragment fragment;
    private int date;
    private SQLiteDatabase alarmdb;

    public MyDateEntryRecyclerViewAdapter(DateEntryFragment fragment, List<CalendarEntryInfo> items, String date) {
        this.fragment = fragment;
        mValues = items;
        this.date = Integer.parseInt(date);
        this.alarmdb = (new CalendarDBHelper(fragment.getContext())).getWritableDatabase();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_dateentry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        CalendarEntryInfo info = mValues.get(position);
        holder.mItem = info;
        holder.text.setText(info.text);
        holder.text.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                mValues.get(holder.getAdapterPosition()).text = s.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        holder.checkBox.setChecked(info.isChecked);
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mValues.get(holder.getAdapterPosition()).isChecked = b;
            }
        });
        if(!info.alarmTime.equals("")) {
            int time = Integer.parseInt(info.alarmTime);
            int hour = time / 100;
            String ampm = " AM";
            if (hour >= 12) {
                ampm = " PM";
                if(hour > 12) {
                    hour -= 12;
                }
            } else if (hour == 0) {
                hour = 12;
            }
            int minute = time % 100;
            if (minute < 10) {
                holder.time.setText(hour + ":0" + minute + ampm);
            } else {
                holder.time.setText(hour + ":" + minute + ampm);
            }
        } else {
            holder.time.setText(R.string.empty_time);
        }
        holder.time.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                DialogFragment dialog = SetTimeDialogFragment.newInstance(holder.getAdapterPosition(), holder.mItem.alarmTime);
                dialog.show(((CalendarActivity) fragment.getActivity()).getSupportFragmentManager(), "setTime");
                return true;
            }
        });
        holder.alarmActive.setOnCheckedChangeListener(null);
        holder.alarmActive.setChecked(info.alarmActive);
        holder.alarmActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                CalendarEntryInfo info =  mValues.get(holder.getAdapterPosition());
                info.alarmActive = b;
                if(b) {
                    if(info.alarmTime.equals("")) {
                        Toast.makeText(fragment.getContext(), "Set time to enable alarm", Toast.LENGTH_SHORT).show();
                        holder.alarmActive.setChecked(false);
                        info.alarmActive = false;
                    } else {
                        if(info.requestCode == 0) {
                            info.requestCode = ((CalendarActivity) fragment.getActivity()).getNextRequestCode();
                        }
                        if(alarmdb == null) { alarmdb = (new CalendarDBHelper(fragment.getContext())).getWritableDatabase(); }
                        if(!setAlarm(fragment.getContext(), alarmdb, info.alarmTime, info.requestCode, date)) {
                            Toast.makeText(fragment.getContext(), "Alarm cannot be set for the past", Toast.LENGTH_SHORT).show();
                            holder.alarmActive.setChecked(false);
                            info.alarmActive = false;
                        }
                    }
                } else {
                    int requestCode = info.requestCode;
                    if(alarmdb == null) { alarmdb = (new CalendarDBHelper(fragment.getContext())).getWritableDatabase(); }
                    cancelAlarm(fragment.getContext(), alarmdb, requestCode, date);
                    Toast.makeText(fragment.getContext(), "Alarm canceled", Toast.LENGTH_SHORT).show();
                }
            }
        });
        holder.settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(fragment.getContext(), holder.settings);
                popup.getMenuInflater().inflate(R.menu.calendar_entry_options, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
                    public boolean onMenuItemClick(MenuItem item) {
                        int index = holder.getAdapterPosition();
                        switch(item.getItemId()) {
                            case R.id.delete_entry:
                                if(alarmdb == null) { alarmdb = (new CalendarDBHelper(fragment.getContext())).getWritableDatabase(); }
                                CalendarEntryInfo info = mValues.get(index);
                                cancelAlarm(fragment.getContext(), alarmdb, info.requestCode, date);
                                mValues.remove(index);
                                notifyItemRemoved(index);
                                break;
                            case R.id.moveup_entry:
                                if(index > 0) {
                                    mValues.add(index - 1, mValues.remove(index));
                                    notifyItemRangeChanged(index - 1, 2);
                                }
                                break;
                            case R.id.movedown_entry:
                                if(index < mValues.size()-1) {
                                    mValues.add(index + 1, mValues.remove(index));
                                    notifyItemRangeChanged(index, 2);
                                }
                                break;
                            case R.id.change_time:
                                DialogFragment dialog = SetTimeDialogFragment.newInstance(holder.getAdapterPosition(), holder.mItem.alarmTime);
                                dialog.show(((CalendarActivity) fragment.getActivity()).getSupportFragmentManager(), "setTime");
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                });

                popup.show();
            }
        });
    }

    public static boolean setAlarm(Context context, SQLiteDatabase db, String time, int rc,int alarmdate) {
        int timetoset = Integer.parseInt(time);
        Calendar now = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();
        calendar.set(alarmdate/10000, ((alarmdate%10000)/100) -1, alarmdate%100, timetoset/100, timetoset%100);
        calendar.set(Calendar.SECOND, 0);

        cancelAlarm(context, db, rc, alarmdate);    //cancel existing alarm

        if(calendar.before(now)) {
            return false;
        }

        Toast toast = Toast.makeText(context,
                "Alarm set for: " + new SimpleDateFormat("EEEE MMMM dd, yyyy h:mm a")
                        .format(new Date(calendar.getTimeInMillis())),
                Toast.LENGTH_LONG);
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        if( v != null) { v.setGravity(Gravity.CENTER); }
        toast.show();

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(DateEntryFragment.DATE_KEY, alarmdate);
        intent.putExtra(DateEntryFragment.RC_KEY, rc);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                rc,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

        ContentValues cv = new ContentValues();
        cv.put(AlarmReferences.COLUMN_KEY, rc);
        cv.put(AlarmReferences.COLUMN_DATE, alarmdate);
        cv.put(AlarmReferences.COLUMN_TIME, time);
        db.insert(AlarmReferences.TABLE_NAME, null, cv);
        Log.e("E", "new alarm put into db");

        Cursor cursor = db.query(AlarmReferences.TABLE_NAME,
                new String[] {AlarmReferences.COLUMN_DATE, AlarmReferences.COLUMN_KEY, AlarmReferences.COLUMN_TIME},
                null,
                null,
                null,
                null,
                null);
        Log.e("E", "num alarms: " + cursor.getCount());
        cursor.close();
        return true;
    }

    public static void cancelAlarm(Context context, SQLiteDatabase db, int rc, int alarmdate) {

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(DateEntryFragment.DATE_KEY, alarmdate);
        intent.putExtra(DateEntryFragment.RC_KEY, rc);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                rc,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent);
        pendingIntent.cancel();

        int x = db.delete(AlarmReferences.TABLE_NAME,
                AlarmReferences.COLUMN_KEY + " = ?",
                new String[] {rc + ""});
        Log.e("E", x + " entries deleted from db");
    }

    public void onPositiveClick(DialogFragment dialog, int index) {
        TimePicker tp = (TimePicker) dialog.getDialog().findViewById(R.id.timePicker);
        String time = tp.getHour()*100 + tp.getMinute() + "";
        CalendarEntryInfo info = mValues.get(index);
        info.alarmTime = time;
        if(info.alarmActive) {
            if(alarmdb == null) { alarmdb = (new CalendarDBHelper(fragment.getContext())).getWritableDatabase(); }
            if(!setAlarm(fragment.getContext(), alarmdb, time, info.requestCode, date)) {
                info.alarmActive = false;
                notifyItemChanged(index);
                Toast.makeText(fragment.getContext(), "Alarm cancelled", Toast.LENGTH_SHORT).show();
            }
        }
        notifyItemChanged(index);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public void closeDatabase() {
        if(alarmdb != null) {
            alarmdb.close();
            alarmdb = null;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final EditText text;
        public final CheckBox checkBox;
        public final ImageButton settings;
        public final TextView time;
        public final CheckBox alarmActive;
        public CalendarEntryInfo mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            text = (EditText) view.findViewById(R.id.entry_text);
            checkBox = (CheckBox) view.findViewById(R.id.entry_checkbox);
            settings = (ImageButton) view.findViewById(R.id.entry_settings);
            time = (TextView) view.findViewById(R.id.entry_time);
            alarmActive = (CheckBox) view.findViewById(R.id.entry_alarm_active);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
