package gdoge.checklistagenda;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TimePicker;

public class SetTimeDialogFragment extends DialogFragment {

    private TimeDialogListener mListener;
    private int index;
    private int hour;
    private int minute;

    public SetTimeDialogFragment() {
        // Required empty public constructor
    }

    public static SetTimeDialogFragment newInstance(int index, String time) {
        SetTimeDialogFragment f = new SetTimeDialogFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt("index", index);
        if(!time.equals("")) {
            int givenTime = Integer.parseInt(time);
            args.putInt("hour", givenTime / 100);
            args.putInt("minute", givenTime % 100);
        }
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.index = getArguments().getInt("index");
        this.hour = getArguments().getInt("hour", -1);
        this.minute = getArguments().getInt("minute", -1);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.set_time_dialog, null);
        if(hour != -1) {
            TimePicker tp = (TimePicker) view.findViewById(R.id.timePicker);
            tp.setHour(hour);
            tp.setMinute(minute);
        }
        builder.setView(view)
                .setPositiveButton(R.string.dialog_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onPositiveClick(SetTimeDialogFragment.this, index);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        SetTimeDialogFragment.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof TimeDialogListener) {
            mListener = (TimeDialogListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement TimeDialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface TimeDialogListener {
        void onPositiveClick(DialogFragment dialog, int index);
    }
}
