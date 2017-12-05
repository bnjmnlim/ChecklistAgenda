package gdoge.checklistagenda;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import gdoge.checklistagenda.NoteFragment.NoteEntryInfo;

import java.util.List;


public class MyNoteRecyclerViewAdapter extends RecyclerView.Adapter<MyNoteRecyclerViewAdapter.ViewHolder> {

    private NoteFragment fragment;
    private final List<NoteEntryInfo> mValues;
    private Context context;
    private View menuClicked;

    public MyNoteRecyclerViewAdapter(List<NoteEntryInfo> items, Context context, NoteFragment fragment) {
        this.mValues = items;
        this.context = context;
        this.fragment = fragment;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_note, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.checkBox.setChecked(mValues.get(position).checked);
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mValues.get(holder.getAdapterPosition()).checked = b;
            }
        });

        holder.text.setText(mValues.get(position).text);
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

        holder.settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuClicked = view;
                PopupMenu popup = new PopupMenu(context, holder.settingButton);
                popup.getMenuInflater().inflate(R.menu.note_entry_options, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
                    public boolean onMenuItemClick(MenuItem item) {
                        switch(item.getItemId()) {
                            case R.id.delete_entry:
                                fragment.deleteNoteEntry((LinearLayout) menuClicked.getParent());
                                break;
                            case R.id.moveup_entry:
                                fragment.ascendNoteEntry((LinearLayout) menuClicked.getParent());
                                break;
                            case R.id.movedown_entry:
                                fragment.descendNoteEntry((LinearLayout) menuClicked.getParent());
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

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final CheckBox checkBox;
        public final EditText text;
        public final ImageButton settingButton;

        public ViewHolder(View view) {
            super(view);
            this.mView = view;
            this.checkBox = (CheckBox) view.findViewById(R.id.entry_checkbox);
            this.text = (EditText) view.findViewById(R.id.entry_text);
            this.settingButton = (ImageButton) view.findViewById(R.id.entry_setting);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
