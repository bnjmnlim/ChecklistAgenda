package gdoge.checklistagenda;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import gdoge.checklistagenda.DateShortcutFragment.OpenCalendarEntryListener;
import gdoge.checklistagenda.DateShortcutFragment.DateShortcut;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class MyDateShortcutRecyclerViewAdapter extends RecyclerView.Adapter<MyDateShortcutRecyclerViewAdapter.ViewHolder> {
    private RecyclerView rv;
    private final List<DateShortcut> mValues;
    private final OpenCalendarEntryListener mListener;
    private final int scrollThreshold = 7;
    private int dateid;
    private int scrollNum;

    private String dateToday;
    private String dateChosen;

    private int colorToday;
    private int colorStandard;
    private int colorChosen;

    public MyDateShortcutRecyclerViewAdapter(RecyclerView rv, List<DateShortcut> items, int dateid,
                                             OpenCalendarEntryListener listener) {
        this.rv = rv;
        mValues = items;
        this.dateid = dateid;
        mListener = listener;
        scrollNum = 0;
        dateToday = new SimpleDateFormat("yyyyMMdd").format(new Date());
        dateChosen = dateToday;
        colorChosen = rv.getContext().getResources().getColor(R.color.colorToday);
        colorToday = rv.getContext().getResources().getColor(R.color.colorPrimary);
        colorStandard = rv.getContext().getResources().getColor(R.color.standard);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_dateshortcut, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mMoYrView.setText(holder.mItem.toMonthYear());
        holder.mDayView.setText("" + holder.mItem.day);
        if (dateChosen.equals(holder.mItem.toString())) {
            holder.mDayView.setTextColor(colorChosen);
            holder.mMoYrView.setTextColor(colorChosen);
        } else if(dateToday.equals(holder.mItem.toString())) {
            holder.mDayView.setTextColor(colorToday);
            holder.mMoYrView.setTextColor(colorToday);
        } else {
            holder.mDayView.setTextColor(colorStandard);
            holder.mMoYrView.setTextColor(colorStandard);
        }

        /*
        if(++scrollNum > scrollThreshold) {
            scrollNum = 0;
            if ((holder.mItem.year * 100) + holder.mItem.month != dateid) {
                if ((holder.mItem.year) * 100 + holder.mItem.month > dateid) {
                    int[] date = backNMonths(holder.mItem.year, holder.mItem.month, 2);
                    final int index = DateShortcutFragment.numDaysInMonth(date[0], date[1]);
                    mValues.subList(0, index).clear();

                    date = holder.mItem.nextMonth();
                    List<DateShortcut> list = new LinkedList<>();
                    final int numDays = DateShortcutFragment.numDaysInMonth(date[0], date[1]);
                    for (int i = 1; i <= numDays; i++) {
                        list.add(new DateShortcut(date[0], date[1], i));
                    }
                    mValues.addAll(list);

                    Handler handler = new Handler();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyItemRangeRemoved(0, index);
                            notifyItemRangeInserted(mValues.size(), numDays);
                        }
                    });
                } else {
                    int[] date = forwardNMonths(holder.mItem.year, holder.mItem.month, 2);
                    final int index = DateShortcutFragment.numDaysInMonth(date[0], date[1]);
                    final int size = mValues.size();
                    mValues.subList(mValues.size() - index, mValues.size()).clear();

                    date = holder.mItem.prevMonth();
                    List<DateShortcut> list = new LinkedList<>();
                    final int numDays = DateShortcutFragment.numDaysInMonth(date[0], date[1]);
                    for (int i = 1; i <= numDays; i++) {
                        mValues.add(-1 + i, new DateShortcut(date[0], date[1], i));
                    }
                    //mValues.addAll(0, list);

                    Handler handler = new Handler();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyItemRangeRemoved(size - index, size);
                            notifyItemRangeInserted(0, numDays);
                            rv.scrollToPosition(numDays + holder.mItem.day+1);
                        }
                    });
                }
                dateid = (holder.mItem.year*100) + holder.mItem.month;
            }
        }
        */

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.openCalendarEntry(holder.mItem.toString());
                }
            }
        });
    }

    /*
    //n is less than 12
    public int[] backNMonths(int year, int month, int n) {
        if(n > month-1) {
            return new int[] {year-1, 12-(n - month)};
        }
        return new int[] {year, month-n};
    }

    public int[] forwardNMonths(int year, int month, int n) {
        if(n + month > 12) {
            return new int[] {year+1, n - (12-month)};
        }
        return new int[] {year, month + n};
    }
    */

    public void setChosenDate(String date) {
        dateChosen = date;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mMoYrView;
        public final TextView mDayView;
        public DateShortcut mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mMoYrView = (TextView) view.findViewById(R.id.moyr);
            mDayView = (TextView) view.findViewById(R.id.day);
        }
    }
}
