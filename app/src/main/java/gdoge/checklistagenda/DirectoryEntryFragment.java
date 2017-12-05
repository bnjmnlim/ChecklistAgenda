package gdoge.checklistagenda;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.ActionBar;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;

import gdoge.checklistagenda.AgendaActivity.NoteArgs;
import gdoge.checklistagenda.AgendaActivity.DBContract.DirectoryEntries;
import gdoge.checklistagenda.AgendaActivity.DBContract.NoteEntries;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class DirectoryEntryFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private static final int TAG_DELETE = 0;
    private static final int TAG_CHANGE = 1;
    private static final int TAG_NEW = 2;

    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;
    private List<DirectoryInfo> directoryInfoList;
    private HashMap<Integer, changeLogEntry> changeLog;
    private RecyclerView rView;
    private View showingDelete;
    private View childToAffect;
    private SQLiteDatabase database;
    private int nextEid;

    public DirectoryEntryFragment() {
    }

    @SuppressWarnings("unused")
    public static DirectoryEntryFragment newInstance(int columnCount) {
        DirectoryEntryFragment fragment = new DirectoryEntryFragment();
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
        View view = inflater.inflate(R.layout.fragment_directoryentry_list, container, false);

        directoryInfoList = new LinkedList<DirectoryInfo>();
        changeLog =  new HashMap<Integer, changeLogEntry>();

        loadDirectoryFromDatabase();
        if(directoryInfoList.isEmpty()) {
            nextEid = 1;
        } else {
            nextEid = directoryInfoList.get(0).entryid + 1;
        }

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            recyclerView.setAdapter(new MyDirectoryEntryRecyclerViewAdapter(directoryInfoList, mListener));
            rView = recyclerView;
        }

        rView.addOnItemTouchListener(new ItemClickListener(getActivity()));
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
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

    @Override
    public void onPause() {
        DirectoryInfo info = ((AgendaActivity) getActivity()).retrieveUpdatedDirectoryEntry();
        if(info != null) {
            changeLog.put(info.entryid, new changeLogEntry(TAG_CHANGE, info));
        }
        saveDirectoryToDatabase();
        super.onPause();
    }

    @Override
    public void onResume() {
        ((AgendaActivity) getActivity()).updateNavHeader();
        AgendaActivity activity = (AgendaActivity) getActivity();
        activity.setToggleState(true);

        activity.updateIcons(AgendaActivity.STATE_DIRECTORY);
        activity.setFragmentState(AgendaActivity.STATE_DIRECTORY);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle(R.string.action_bar_directory);
        }

        DirectoryInfo info = activity.retrieveUpdatedDirectoryEntry();
        activity.clearUpdatedDirectoryEntry();
        if(info != null) {
            int index = -1;
            for(DirectoryInfo temp : directoryInfoList) {
                if(temp.entryid == info.entryid) {
                    index = directoryInfoList.indexOf(temp);
                }
            }
            directoryInfoList.set(index, info);
            rView.getAdapter().notifyItemChanged(index);
        }

        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        database.close();
    }

    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(NoteArgs args);
    }

    public void loadDirectoryFromDatabase() {
        database = ((AgendaActivity) getActivity()).getDatabase();
        String[] projection = {DirectoryEntries.COLUMN_NAME, DirectoryEntries.COLUMN_PROGRESS,
                DirectoryEntries.COLUMN_PREVIEW, DirectoryEntries.COLUMN_EID};
        Cursor cursor = database.query(DirectoryEntries.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null);
        int eidIndex = cursor.getColumnIndex(DirectoryEntries.COLUMN_EID);
        int nameIndex= cursor.getColumnIndex(DirectoryEntries.COLUMN_NAME);
        int progressIndex= cursor.getColumnIndex(DirectoryEntries.COLUMN_PROGRESS);
        int previewIndex = cursor.getColumnIndex(DirectoryEntries.COLUMN_PREVIEW);
        while(cursor.moveToNext()) {
            directoryInfoList.add(0, new DirectoryInfo(cursor.getInt(eidIndex), cursor.getString(nameIndex),
                    cursor.getString(progressIndex), cursor.getString(previewIndex)));

        }
        cursor.close();
        Collections.sort(directoryInfoList);
    }

    public void saveDirectoryToDatabase() {
        if(database == null) { database = ((AgendaActivity) getActivity()).getDatabase(); }
        ContentValues cv = new ContentValues();
        database.beginTransaction();
        for(Integer eid : changeLog.keySet()) {
            changeLogEntry toReplace = changeLog.get(eid);
            if(toReplace.tag == TAG_NEW) {
                insertIntoDirectoryDb(cv, toReplace.info);
            } else {
                database.delete(DirectoryEntries.TABLE_NAME, DirectoryEntries.COLUMN_EID + " = ?",
                        new String[]{Integer.toString(eid)});
                if (toReplace.tag == TAG_CHANGE) {
                    insertIntoDirectoryDb(cv, toReplace.info);
                } else if (toReplace.tag == TAG_DELETE) {
                    database.delete(
                            NoteEntries.TABLE_NAME,
                            NoteEntries.COLUMN_EID + " = ?",
                            new String[] { Integer.toString(toReplace.info.entryid) }
                    );
                }
            }
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        changeLog.clear();
    }

    public void insertIntoDirectoryDb(ContentValues cv, DirectoryInfo info) {
        cv.clear();
        cv.put(DirectoryEntries.COLUMN_EID, info.entryid);
        cv.put(DirectoryEntries.COLUMN_NAME, info.title);
        cv.put(DirectoryEntries.COLUMN_PROGRESS, info.progress);
        cv.put(DirectoryEntries.COLUMN_PREVIEW, info.preview);
        database.insert(DirectoryEntries.TABLE_NAME, null, cv);
    }

    public void onSwipeRightToLeft() {
        if(childToAffect != null) {
            LinearLayout layout = (LinearLayout) childToAffect;
            View deleteToShow = layout.getChildAt(layout.getChildCount()-1);
            if(deleteToShow.getVisibility() == View.GONE) {
                if(showingDelete != null) {
                    LinearLayoutManager manager = (LinearLayoutManager) rView.getLayoutManager();
                    int position = rView.indexOfChild((View) showingDelete.getParent());
                    if(manager.findFirstVisibleItemPosition() < position && manager.findLastVisibleItemPosition() < position) {
                        TranslateAnimation animate = new TranslateAnimation(0, showingDelete.getWidth(), 0, 0);
                        animate.setDuration(200);
                        showingDelete.startAnimation(animate);
                    }
                    showingDelete.setVisibility(View.GONE);
                }
                showingDelete = deleteToShow;
                TranslateAnimation animate = new TranslateAnimation(showingDelete.getWidth(), 0, 0, 0);
                animate.setDuration(200);
                showingDelete.startAnimation(animate);
                showingDelete.setVisibility(View.VISIBLE);
            }
        }
    }

    public void createNewEntry() {
        DirectoryInfo newEntry = new DirectoryInfo(nextEid, "New Note", "0 of 0 completed", "");
        directoryInfoList.add(0, newEntry);
        changeLog.put(newEntry.entryid, new changeLogEntry(TAG_NEW, newEntry));
        nextEid++;
        rView.getAdapter().notifyItemInserted(0);
    }

    public void onSwipeLeftToRight() {
        if(childToAffect != null && showingDelete != null) {
            if(childToAffect instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) childToAffect;
                if(layout.getChildAt(layout.getChildCount()-1) == showingDelete) {
                    TranslateAnimation animate = new TranslateAnimation(0, showingDelete.getWidth(), 0, 0);
                    animate.setDuration(200);
                    showingDelete.startAnimation(animate);
                    showingDelete.setVisibility(View.GONE);
                    showingDelete = null;
                }
            }
        }
    }

    public void onItemTapped(View v, MotionEvent e) {
        if(v instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) v;
            int realpos = rView.getChildAdapterPosition(v);
            View delete = layout.getChildAt(layout.getChildCount()-1);
            int[] loc = new int[2];
            delete.getLocationOnScreen(loc);
            int x = (int) e.getRawX();
            int y = (int) e.getRawY();
            if(delete.getId() == R.id.directory_delete &&
                    x > loc[0] && x < loc[0] + delete.getWidth()
                    && y > loc[1] && y < loc[1] + delete.getHeight()) {
                DirectoryInfo toRemove = directoryInfoList.get(realpos);
                changeLog.put(toRemove.entryid, new changeLogEntry(TAG_DELETE, toRemove));
                directoryInfoList.remove(realpos);
                rView.getAdapter().notifyItemRemoved(realpos);
            } else {
                DirectoryInfo args = directoryInfoList.get(realpos);
                mListener.onListFragmentInteraction(
                        new NoteArgs(args.entryid, args.title));
            }
        }
    }

    private class changeLogEntry {
        public int tag;
        public DirectoryInfo info;

        public changeLogEntry(int tag, DirectoryInfo info) {
            this.tag = tag;
            this.info = info;
        }
    }

    public static class DirectoryInfo implements Comparable<DirectoryInfo> {
        public int entryid;
        public String title;
        public String progress;
        public String preview;

        public DirectoryInfo(int eid, String title, String progress, String preview) {
            this.entryid = eid;
            this.title = title;
            this.progress = progress;
            this.preview = preview;
        }

        //to sort lowest entry id last
        @Override
        public int compareTo(DirectoryInfo other) {
            return other.entryid - this.entryid;
        }
    }


    private class ItemClickListener implements RecyclerView.OnItemTouchListener {
        private GestureDetector gd;

        private ItemClickListener(Context context) {
            gd = new GestureDetector(context, new directoryGestureDetector());
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            View view = rv.findChildViewUnder(e.getX(), e.getY());
            if(e.getAction() == MotionEvent.ACTION_DOWN) {
                childToAffect = view;
            }
            return view != null && gd.onTouchEvent(e);
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallow) {

        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }
    }

    private class directoryGestureDetector extends GestureDetector.SimpleOnGestureListener {
        private final int threshold = 50;

        @Override
        public boolean onDown(MotionEvent me) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent me1, MotionEvent me2, float dX, float dy) {
            if(me1 == null || me2 == null) {
                return false;
            }
            float xdist = Math.abs(me1.getX() - me2.getX());
            float ydist = Math.abs(me1.getY() - me2.getY());
            if(xdist > ydist && xdist > threshold) {
                if(me1.getX() > me2.getX()) {
                    onSwipeRightToLeft();
                } else {
                    onSwipeLeftToRight();
                }
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            View v = rView.findChildViewUnder(e.getX(), e.getY());
            onItemTapped(v, e);
            return true;
        }
    }
}
