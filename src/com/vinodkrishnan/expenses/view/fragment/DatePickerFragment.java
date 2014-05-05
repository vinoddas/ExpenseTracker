package com.vinodkrishnan.expenses.view.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.TextView;

import java.util.Calendar;

/**
 *
 */
public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener, DatePickerDialog.OnDismissListener {

    private TextView mDateTextView;
    private int mSelectedYear;
    private int mSelectedMonth;
    private int mSelectedDay;

    public DatePickerFragment() {
    }

    public DatePickerFragment(TextView dateTextView) {
        mDateTextView = dateTextView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        mSelectedYear = c.get(Calendar.YEAR);
        mSelectedMonth = c.get(Calendar.MONTH);
        mSelectedDay = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, mSelectedYear, mSelectedMonth, mSelectedDay);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        mSelectedYear = year;
        mSelectedMonth = monthOfYear;
        mSelectedDay = dayOfMonth;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mDateTextView != null) {
            mDateTextView.setText(mSelectedMonth + 1 + "/" + mSelectedDay + "/" + mSelectedYear);
        }
    }
}
