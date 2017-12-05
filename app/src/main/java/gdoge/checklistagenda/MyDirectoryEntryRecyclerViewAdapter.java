package gdoge.checklistagenda;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import gdoge.checklistagenda.DirectoryEntryFragment.OnListFragmentInteractionListener;

import java.util.List;

public class MyDirectoryEntryRecyclerViewAdapter extends RecyclerView.Adapter<MyDirectoryEntryRecyclerViewAdapter.ViewHolder> {

    private final List<DirectoryEntryFragment.DirectoryInfo> mValues;
    private final OnListFragmentInteractionListener mListener;

    public MyDirectoryEntryRecyclerViewAdapter(List<DirectoryEntryFragment.DirectoryInfo> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_directoryentry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.title.setText(mValues.get(position).title);
        holder.progress.setText(mValues.get(position).progress);
        holder.preview.setText(mValues.get(position).preview);
        holder.delete.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView title;
        public final TextView progress;
        public final TextView preview;
        public final Button delete;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            title = (TextView) view.findViewById(R.id.directory_title);
            progress = (TextView) view.findViewById(R.id.directory_progress);
            preview = (TextView) view.findViewById(R.id.directory_preview);
            delete = (Button) view.findViewById(R.id.directory_delete);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + title.getText() + "'";
        }
    }
}
