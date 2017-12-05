package gdoge.checklistagenda;

import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class DateShortcutFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;
    private OpenCalendarEntryListener mListener;
    private List<DateShortcut> dateList;
    public RecyclerView rView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DateShortcutFragment() {
    }

    @SuppressWarnings("unused")
    public static DateShortcutFragment newInstance(int columnCount) {
        DateShortcutFragment fragment = new DateShortcutFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dateshortcut_list, container, false);
        dateList = new LinkedList<>();

        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        int currYear = Integer.parseInt(today.substring(0,4));
        int currMonth = Integer.parseInt(today.substring(4,6));
        int currDay = Integer.parseInt(today.substring(6));
        populateDateList(currYear, currMonth, currDay);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            rView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                rView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
            } else {
                rView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            rView.setAdapter(new MyDateShortcutRecyclerViewAdapter(rView, dateList, (currYear*100)+currMonth, mListener));
        }
        return view;
    }

    public void populateDateList(int year, int month, int day) {
        int daysInMonth = numDaysInMonth(year, month);
        if(day < 3) {
            int[] prevMonth = prevMonth(year, month);
            int daysInPrevMonth = numDaysInMonth(prevMonth[0], prevMonth[1]);
            dateList.add(new DateShortcut(prevMonth[0], prevMonth[1], daysInPrevMonth));
            if(day == 1) {
                dateList.add(0, new DateShortcut(prevMonth[0], prevMonth[1], daysInPrevMonth-1));
            } else {
                dateList.add(new DateShortcut(year, month, day-1));
            }
        } else {
            dateList.add(new DateShortcut(year, month, day-2));
            dateList.add(new DateShortcut(year, month, day-1));
        }
        dateList.add(new DateShortcut(year, month, day));
        int count = 3;
        int lastDay = day+1;
        int[] currDate = {year, month};
        while(count++ <= 10) {
            if(lastDay > daysInMonth) {
                lastDay = 1;
                currDate = nextMonth(currDate[0], currDate[1]);
            }
            dateList.add(new DateShortcut(currDate[0], currDate[1], lastDay++));
        }
    }

    public static int numDaysInMonth(int year, int month) {
        switch(month) {
            case 2:
                if(year%400 == 0 || year%100 != 0 && year%4 == 0) {
                    return 29;
                } else {
                    return 28;
                }
            case 1:case 3:case 5:case 7: case 8: case 10:case 12:
                return 31;
            default:
                return 30;
        }
    }

    //returns { year, month } of next month
    public int[] nextMonth(int year, int month) {
        if(month == 12) {
            return new int[] {year+1, 1};
        } else {
            return new int[] {year, month+1};
        }
    }

    public int[] prevMonth(int year, int month) {
        if(month == 1) {
            return new int[] {year-1, 12};
        } else {
            return new int[] {year, month-1};
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OpenCalendarEntryListener) {
            mListener = (OpenCalendarEntryListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OpenCalendarEntryListener {
        void openCalendarEntry(String date);
    }

    public static class DateShortcut {
        public int year;
        public int month;
        public int day;

        public DateShortcut(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        public String toString() {
            return "" + year + ((month < 10) ? "0"+month : month) + ((day < 10) ? "0"+day : day);
        }

        public String toMonthYear() {
            switch(month) {
                case 1:
                    return "Jan " + year;
                case 2:
                    return "Feb " + year;
                case 3:
                    return "Mar " + year;
                case 4:
                    return "Apr " + year;
                case 5:
                    return "May " + year;
                case 6:
                    return "Jun " + year;
                case 7:
                    return "Jul " + year;
                case 8:
                    return "Aug " + year;
                case 9:
                    return "Sep " + year;
                case 10:
                    return "Oct " + year;
                case 11:
                    return "Nov " + year;
                case 12:
                    return "Dec " + year;
                default:
                    return "";
            }
        }
    }
}
