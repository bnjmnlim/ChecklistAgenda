package gdoge.checklistagenda;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import gdoge.checklistagenda.CalendarActivity.DBContract.CalendarEntry;
import gdoge.checklistagenda.CalendarActivity.DBContract.CalendarContent;
import gdoge.checklistagenda.CalendarActivity.DBContract.AlarmReferences;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DateEntryFragment extends Fragment {

    public static final int STATUS_CHECKED = 1;
    public static final int STATUS_UNCHECKED = 0;
    private static final String ARG_DATE = "date";
    private final int PREVIEW_LENGTH = 5;

    private int mColumnCount = 1;
    private String date;
    private int nextId;

    public RecyclerView rView;
    private List<CalendarEntryInfo> entries;
    private SQLiteDatabase db;

    //for inserting date to intent extras
    public static final String DATE_KEY = "datekey";
    public static final String RC_KEY = "requestkey";

    public DateEntryFragment() {
    }

    @SuppressWarnings("unused")
    public static DateEntryFragment newInstance(String date) {
        DateEntryFragment fragment = new DateEntryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, date);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            date = getArguments().getString(ARG_DATE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dateentry_list, container, false);
        entries = new LinkedList<>();
        db = ((CalendarActivity) getActivity()).getDatabase();

        loadEntriesToList();
        if(!entries.isEmpty()) {
            nextId = entries.get(0).calEntryId + 1;
        }

        // Set the adapter
        if (view instanceof RecyclerView) {
            rView = (RecyclerView) view;
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            recyclerView.setAdapter(new MyDateEntryRecyclerViewAdapter(this, entries, date));
        }
        return view;
    }

    public void loadEntriesToList() {
        Cursor cursor = db.query(CalendarContent.TABLE_NAME,
                new String[] {CalendarContent.COLUMN_ID, CalendarContent.COLUMN_ALARMINFO, CalendarContent.COLUMN_CHECKED, CalendarContent.COLUMN_CONTENT, CalendarContent.COLUMN_REQUESTCODE},
                CalendarContent.COLUMN_DATE + " = ?",
                new String[] {date},
                null,
                null,
                null);

        int id = cursor.getColumnIndex(CalendarContent.COLUMN_ID);
        int alarminfo = cursor.getColumnIndex(CalendarContent.COLUMN_ALARMINFO);
        int checked = cursor.getColumnIndex(CalendarContent.COLUMN_CHECKED);
        int content = cursor.getColumnIndex(CalendarContent.COLUMN_CONTENT);
        int requestcode = cursor.getColumnIndex(CalendarContent.COLUMN_REQUESTCODE);
        while(cursor.moveToNext()) {
            entries.add(new CalendarEntryInfo(cursor.getInt(id), cursor.getString(content),
                    cursor.getInt(checked), cursor.getString(alarminfo), cursor.getInt(requestcode)));
        }

        Collections.sort(entries);
        cursor.close();
    }

    public void saveEntriesToDatabase() {
        db.beginTransaction();
        Iterator<CalendarEntryInfo> iter = entries.iterator();
        int id = entries.size();
        ContentValues cv = new ContentValues();
        while(iter.hasNext()) {
            CalendarEntryInfo info = iter.next();
            cv.clear();
            cv.put(CalendarContent.COLUMN_ID, id--);
            cv.put(CalendarContent.COLUMN_DATE, date);
            cv.put(CalendarContent.COLUMN_CONTENT, info.text);
            cv.put(CalendarContent.COLUMN_CHECKED, ((info.isChecked) ? STATUS_CHECKED : STATUS_UNCHECKED));
            cv.put(CalendarContent.COLUMN_ALARMINFO, ((info.alarmActive) ? '1' : '0') + info.alarmTime);
            cv.put(CalendarContent.COLUMN_REQUESTCODE, info.requestCode);
            db.insert(CalendarContent.TABLE_NAME, null, cv);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @Override
    public void onResume() {
        CalendarActivity activity = (CalendarActivity)getActivity();
        activity.setStateEntry(date);
        activity.updateNavHeader();
        super.onResume();
    }

    @Override
    public void onPause() {
        db.delete(CalendarEntry.TABLE_NAME, CalendarEntry.COLUMN_DATE + " = ?", new String[] {date});
        db.delete(CalendarContent.TABLE_NAME, CalendarContent.COLUMN_DATE + " = ?", new String[] {date});
        ((MyDateEntryRecyclerViewAdapter) rView.getAdapter()).closeDatabase();
        if(!entries.isEmpty()) {
            ContentValues cv = new ContentValues();
            cv.put(CalendarEntry.COLUMN_DATE, date);
            int count = 0;
            String preview = "";
            for (CalendarEntryInfo info : entries) {
                if (!info.isChecked && !info.text.equals("")) {
                    if (++count > PREVIEW_LENGTH) {
                        preview += "...";
                        break;
                    }
                    preview += info.text + "\n";
                }
            }
            cv.put(CalendarEntry.COLUMN_OVERVIEW, preview);
            db.insert(CalendarEntry.TABLE_NAME, null, cv);
            saveEntriesToDatabase();
        }
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void addNewEntry() {
        entries.add(0, new CalendarEntryInfo(nextId++, "", STATUS_UNCHECKED, "0", 0));
        rView.getAdapter().notifyItemInserted(0);
    }

    public void sortEntriesAscending() {
        for(CalendarEntryInfo info : entries) {
            if(!info.alarmTime.equals("")) {
                info.calEntryId = Integer.MAX_VALUE - Integer.parseInt(info.alarmTime);
            } else {
                info.calEntryId = -1;
            }
        }
        Collections.sort(entries);
        rView.getAdapter().notifyDataSetChanged();
    }

    public void deleteAllEntries() {
        if(db == null) { db = ((CalendarActivity) getActivity()).getDatabase(); }
        db.beginTransaction();
        for(CalendarEntryInfo info : entries) {
            if(info.alarmActive &&info.requestCode != 0) {
                MyDateEntryRecyclerViewAdapter.cancelAlarm(getContext(), db, info.requestCode, Integer.parseInt(date));
            }
        }
        entries.clear();
        db.setTransactionSuccessful();
        db.endTransaction();
        rView.getAdapter().notifyDataSetChanged();
    }

    public class CalendarEntryInfo implements Comparable<CalendarEntryInfo> {
        public int calEntryId;
        public String text;
        public boolean isChecked;
        public boolean alarmActive;
        public String alarmTime;
        public int requestCode;

        public CalendarEntryInfo(int id, String text, int isChecked, String alarmInfo, int requestCode) {
            this.calEntryId = id;
            this.text = text;
            this.isChecked = isChecked == STATUS_CHECKED;
            this.alarmActive = alarmInfo.charAt(0) == '1';
            this.alarmTime = alarmInfo.substring(1);
            this.requestCode = requestCode;
        }

        public int compareTo(CalendarEntryInfo other) {
            return other.calEntryId - this.calEntryId;
        }
    }
}
