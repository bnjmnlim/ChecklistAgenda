package gdoge.checklistagenda;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import gdoge.checklistagenda.CalendarActivity.DBContract.CalendarEntry;


public class CalendarFragment extends Fragment {

    private OnCalendarInteractionListener mListener;
    private CalendarView calendarView;
    private TextView previewBox;
    private String hoveringDate;
    private SQLiteDatabase db;

    public CalendarFragment() {
        // Required empty public constructor
    }

    public static CalendarFragment newInstance() {
        CalendarFragment fragment = new CalendarFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        db = ((CalendarActivity) getActivity()).getDatabase();

        calendarView = (CalendarView) view.findViewById(R.id.calendarView);
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int day) {
                String selectedDate = year*10000 + (month+1)*100 + day + "";
                while(selectedDate.length() < 8) {
                    selectedDate = "0" + selectedDate;
                }
                if(hoveringDate.equals(selectedDate)) {
                    Log.e("E", "opening: " + selectedDate);
                    mListener.onCalendarInteraction(selectedDate);
                } else {
                    Log.e("E", "showing preview: " + selectedDate);
                    hoveringDate = selectedDate;
                    showPreview(selectedDate);
                }
            }
        });
        previewBox = (TextView) view.findViewById(R.id.previewBox);
        hoveringDate = new SimpleDateFormat("yyyyMMdd").format(new Date(calendarView.getDate()));
        showPreview(hoveringDate);
        return view;
    }

    public void showPreview(String date) {
        Cursor cursor = db.query(
                CalendarEntry.TABLE_NAME,
                new String[] {CalendarEntry.COLUMN_OVERVIEW},
                CalendarEntry.COLUMN_DATE + " = ?",
                new String[] {date},
                null,
                null,
                null);
        if(cursor.moveToNext()) {
            previewBox.setText(cursor.getString(cursor.getColumnIndex(CalendarEntry.COLUMN_OVERVIEW)));
        } else {
            previewBox.setText(R.string.empty_preview);
        }
        cursor.close();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnCalendarInteractionListener) {
            mListener = (OnCalendarInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        db.close();
        mListener = null;
    }

    @Override
    public void onResume() {
        ((CalendarActivity) getActivity()).updateNavHeader();
        ((CalendarActivity) getActivity()).setStateCalendar();
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date(calendarView.getDate()));
        Cursor cursor = db.query(
                CalendarEntry.TABLE_NAME,
                new String[] {CalendarEntry.COLUMN_OVERVIEW},
                CalendarEntry.COLUMN_DATE + " = ?",
                new String[] {date},
                null,
                null,
                null);
        if(cursor.moveToNext()) {
            previewBox.setText(cursor.getString(cursor.getColumnIndex(CalendarEntry.COLUMN_OVERVIEW)));
        }
        cursor.close();
        super.onResume();
    }

    public interface OnCalendarInteractionListener {
        void onCalendarInteraction(String str);
    }
}
