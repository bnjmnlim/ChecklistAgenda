package gdoge.checklistagenda;

import android.app.ActionBar;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import gdoge.checklistagenda.AgendaActivity.DBContract.DirectoryEntries;
import gdoge.checklistagenda.AgendaActivity.DBContract.NoteEntries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NoteFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private static final String ARG_EID = "entry-id";

    private static final int STATUS_CHECKED = 1;
    private static final int STATUS_UNCHECKED = 0;

    private int mColumnCount = 1;
    private NoteFragmentInteractionListener mListener;
    private int eid;
    private List<NoteEntryInfo> entryInfoList;
    private SQLiteDatabase database;
    private int nextLineId;
    private RecyclerView rView;

    public NoteFragment() {
    }

    @SuppressWarnings("unused")
    public static NoteFragment newInstance(int columnCount, int eid) {
        NoteFragment fragment = new NoteFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        args.putInt(ARG_EID, eid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            this.mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
            this.eid = getArguments().getInt(ARG_EID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        entryInfoList = new ArrayList<NoteEntryInfo>();
        loadEntriesFromDatabase();
        if(!entryInfoList.isEmpty()) {
            nextLineId = entryInfoList.get(0).lineId + 1;
        } else {
            nextLineId = 1;
        }

        View view = inflater.inflate(R.layout.fragment_note_list, container, false);
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            rView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                rView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                rView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            rView.setAdapter(new MyNoteRecyclerViewAdapter(entryInfoList, getActivity(), this));
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof NoteFragmentInteractionListener) {
            mListener = (NoteFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        AgendaActivity activity = (AgendaActivity) getActivity();
        activity.updateNavHeader();
        activity.setToggleState(false);
        super.onResume();
    }

    @Override
    public void onPause() {
        String preview = "";
        int previewCount = 0;
        int numChecked = 0;
        for(NoteEntryInfo info : entryInfoList) {
            if(info.checked) {
                numChecked++;
            } else {
                if(previewCount < 3) {
                    preview = preview + info.text;
                    if(previewCount++ != 2) {
                        preview = preview + "\n";
                    }
                }
            }
        }
        String progress = numChecked + " of " + entryInfoList.size() + " completed";
        String title = ((AgendaActivity) getActivity()).getSupportActionBar().getTitle().toString();

        ContentValues cv = new ContentValues();
        cv.put(DirectoryEntries.COLUMN_EID, this.eid);
        cv.put(DirectoryEntries.COLUMN_NAME, title);
        cv.put(DirectoryEntries.COLUMN_PROGRESS, progress);
        cv.put(DirectoryEntries.COLUMN_PREVIEW, preview);
        if(database == null) { database = ((AgendaActivity) getActivity()).getDatabase(); }
        database.delete(DirectoryEntries.TABLE_NAME, DirectoryEntries.COLUMN_EID + " = ?",
                new String[] { Integer.toString(this.eid)});
        database.insert(DirectoryEntries.TABLE_NAME, null, cv);

        DirectoryEntryFragment.DirectoryInfo toSend = new DirectoryEntryFragment.DirectoryInfo(
                this.eid,
                title,
                progress,
                preview);
        mListener.updateDirectoryEntry(toSend);
        saveEntriesToDatabase();
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        database.close();
    }

    public void loadEntriesFromDatabase() {
        if(database == null) { database = ((AgendaActivity) getActivity()).getDatabase(); }
        String[] projection = { NoteEntries.COLUMN_LINE_ID, NoteEntries.COLUMN_CHECKED, NoteEntries.COLUMN_TEXT };
        String where = NoteEntries.COLUMN_EID + " = ?";
        String[] whereArgs = { Integer.toString(this.eid) };
        Cursor cursor = database.query(NoteEntries.TABLE_NAME,
                projection,
                where,
                whereArgs,
                null,
                null,
                null);

        int lineIdIndex = cursor.getColumnIndex(NoteEntries.COLUMN_LINE_ID);
        int checkIndex = cursor.getColumnIndex(NoteEntries.COLUMN_CHECKED);
        int textIndex = cursor.getColumnIndex(NoteEntries.COLUMN_TEXT);

        while(cursor.moveToNext()) {
            entryInfoList.add(new NoteEntryInfo(cursor.getInt(lineIdIndex),
                    cursor.getInt(checkIndex), cursor.getString(textIndex)));
        }
        cursor.close();
        Collections.sort(entryInfoList);
    }

    public void saveEntriesToDatabase() {
        if(database == null) { database = ((AgendaActivity) getActivity()).getDatabase(); }
        database.delete(NoteEntries.TABLE_NAME, NoteEntries.COLUMN_EID + " = ?", new String[] { Integer.toString(this.eid) });
        database.beginTransaction();
        ContentValues cv = new ContentValues();
        for(NoteEntryInfo info : entryInfoList) {
            cv.clear();
            cv.put(NoteEntries.COLUMN_EID, this.eid);
            cv.put(NoteEntries.COLUMN_LINE_ID, info.lineId);
            cv.put(NoteEntries.COLUMN_CHECKED, ((info.checked) ? STATUS_CHECKED : STATUS_UNCHECKED));
            cv.put(NoteEntries.COLUMN_TEXT, info.text);
            database.insert(NoteEntries.TABLE_NAME, null, cv);
        }
        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public void deleteNoteEntry(LinearLayout layout) {
        int index = rView.getChildAdapterPosition(layout);
        entryInfoList.remove(index);
        rView.getAdapter().notifyItemRemoved(index);
    }

    public void ascendNoteEntry(LinearLayout layout) {
        int index = rView.getChildAdapterPosition(layout);
        NoteEntryInfo e1 = entryInfoList.get(index);
        NoteEntryInfo e2 = entryInfoList.get(index-1);
        int temp = e1.lineId;
        e2.lineId = e1.lineId;
        e1.lineId = temp;
        entryInfoList.add(index-1, entryInfoList.remove(index));
        rView.getAdapter().notifyItemChanged(index-1);
        rView.getAdapter().notifyItemChanged(index);
    }

    public void descendNoteEntry(LinearLayout layout) {
        int index = rView.getChildAdapterPosition(layout);
        NoteEntryInfo e1 = entryInfoList.get(index);
        NoteEntryInfo e2 = entryInfoList.get(index+1);
        int temp = e1.lineId;
        e2.lineId = e1.lineId;
        e1.lineId = temp;
        entryInfoList.add(index, entryInfoList.remove(index+1));
        rView.getAdapter().notifyItemChanged(index);
        rView.getAdapter().notifyItemChanged(index+1);
    }

    public void createNewEntry() {
        NoteEntryInfo toCreate = new NoteEntryInfo(nextLineId, STATUS_UNCHECKED, "");
        nextLineId++;
        entryInfoList.add(0, toCreate);
        rView.getAdapter().notifyItemInserted(0);
    }

    public interface NoteFragmentInteractionListener {
        void updateDirectoryEntry(DirectoryEntryFragment.DirectoryInfo info);
    }

    public class NoteEntryInfo implements Comparable<NoteEntryInfo> {
        public int lineId;
        public boolean checked;
        public String text;

        public NoteEntryInfo(int lineId, int checked, String text) {
            this.lineId = lineId;
            this.checked = checked == STATUS_CHECKED;
            this.text = text;
        }

        public int compareTo(NoteEntryInfo info) {
            return info.lineId - this.lineId;
        }
    }
}
