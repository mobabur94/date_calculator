package com.mobabur94.datecalc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements ViewPager.OnPageChangeListener {

    SectionsPagerAdapter sectionsPagerAdapter;
    ViewPager viewPager;
    CompareModeFragment compareModeFragment;
    AddModeFragment addModeFragment;
    private boolean businessMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the layout
        setContentView(R.layout.activity_main);

        // set up the pager adapter
        sectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOnPageChangeListener(this);

        // handle runtime configuration change and restore the fragments
        if (savedInstanceState == null) {
            compareModeFragment = (CompareModeFragment) sectionsPagerAdapter.getItem(0);
            addModeFragment = (AddModeFragment) sectionsPagerAdapter.getItem(1);
        } else {
            compareModeFragment = (CompareModeFragment) getFragmentManager().getFragment(savedInstanceState, "compareModeFragment");
            addModeFragment = (AddModeFragment) getFragmentManager().getFragment(savedInstanceState, "addModeFragment");
        }

        // set the title based on what page is open
        setTitle((viewPager.getCurrentItem() == 1) ? R.string.title_add : R.string.title_compare);

        // restore the business mode, with a default of false
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        businessMode = settings.getBoolean("business_mode", false);

        // propagate the business mode
        compareModeFragment.businessMode = businessMode;
        addModeFragment.businessMode = businessMode;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // save the fragments
        getFragmentManager().putFragment(outState, "compareModeFragment", compareModeFragment);
        getFragmentManager().putFragment(outState, "addModeFragment", addModeFragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // set the menu up
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // initialize the business mode checkbox
        menu.findItem(R.id.action_business_mode).setChecked(businessMode);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_business_mode) {
            // toggle the business mode since it is a checkbox
            businessMode = !businessMode;
            item.setChecked(businessMode);

            // show a toast
            Toast.makeText(getApplicationContext(), (businessMode) ? "Strictly business from here on out" : "Everyday normal mode", Toast.LENGTH_SHORT).show();

            // update the preferences so that the business mode can be restored
            SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
            editor.putBoolean("business_mode", businessMode);
            editor.apply();

            // propagate the new business mode and recalculate the difference
            compareModeFragment.businessMode = businessMode;
            compareModeFragment.calculateDifference();

            // propagate the new business mode and recalculate the addition
            addModeFragment.businessMode = businessMode;
            addModeFragment.updateNumberPickers();
            addModeFragment.calculateAddition();

            return true;
        } else if (id == R.id.action_help) {
            // create a help message
            String message = "There are two modes you can swipe left/right between:<br><br><b>1. Compare Mode</b><br><b>2. Add Mode</b><br><br>You can also change the type of calculation (business days or regular days) from the context menu.<br><br><br><br><b>Compare Mode:</b><br>Tap the buttons to change the date range.<br><br><b>Add Mode:</b><br>Tap the first button to change the base date. Swipe the number pickers up/down to change the amount of years, months, and days to add. You can subtract by specifying a negative number to add.";

            // create and show a dialog with the message
            DialogFragment alert = InfoDialogFragment.newInstance(message);
            alert.show(getFragmentManager(), "about_dialog");

            return true;
        } else if (id == R.id.action_about) {
            // create an about message
            String message = "When using this date calculator, keep in mind that the calculations are based on there being 97 leap years every 400 years. That is, there are 365.2425 days in a year. This may result in a small error.";

            // create and show a dialog with the message
            DialogFragment alert = InfoDialogFragment.newInstance(message);
            alert.show(getFragmentManager(), "about_dialog");

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) { }

    @Override
    public void onPageScrollStateChanged(int i) { }

    @Override
    public void onPageSelected(int i) {
        // update the title when the page is changed
        setTitle((i == 1) ? R.string.title_add : R.string.title_compare);
    }

    // simple informational dialog class
    public static class InfoDialogFragment extends DialogFragment {

        public static InfoDialogFragment newInstance(String message) {
            // create a new instance
            InfoDialogFragment dialog = new InfoDialogFragment();

            // create and attach a bundle with the message the dialog is supposed to display
            Bundle args = new Bundle();
            args.putString("message", message);
            dialog.setArguments(args);

            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // get the message this dialog is supposed to show
            String message = getArguments().getString("message", "");

            // apply the message to a text view with some padding
            TextView view = new TextView(getActivity());
            view.setText(Html.fromHtml(message));
            view.setPadding(24, 24, 24, 24);

            // build a dialog with the text view and a close button
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(view);
            builder.setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) { }
            });

            return builder.create();
        }

    }

    // tiny date calculation helper
    public static class DateCalcHelper {

        public static long daysUntilSunday(Calendar calendar) {
            long days = 0;
            while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                days++;
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            return days;
        }

        public static long daysFromSunday(Calendar calendar) {
            long days = 0;
            while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                days++;
                calendar.add(Calendar.DAY_OF_YEAR, -1);
            }
            return days;
        }

    }

    // container for the fragments in the view pager
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private AddModeFragment addModeFragment = null;
        private CompareModeFragment compareModeFragment = null;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 1) {
                if (addModeFragment == null) {
                    addModeFragment = new AddModeFragment();
                }
                return addModeFragment;
            } else {
                if (compareModeFragment == null) {
                    compareModeFragment = new CompareModeFragment();
                }
                return compareModeFragment;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 1:
                    return getString(R.string.title_add);
                default:
                    return getString(R.string.title_compare);
            }
        }
    }

    // interface for handling the response of a date picker dialog
    public interface DatePickerDialogListener {
        public void onDatePickerDialogDone(int id, int year, int month, int day);
    }

    // add mode fragment to handle adding amounts to dates
    public static class AddModeFragment extends Fragment implements View.OnClickListener, DatePickerDialogListener, NumberPicker.OnValueChangeListener {

        private SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd [EEE]", Locale.US);
        private Button addBaseDate;
        LinearLayout addAmountLayoutYears;
        LinearLayout addAmountLayoutMonths;
        LinearLayout addAmountLayoutDays;
        private NumberPicker addAmountYears;
        private NumberPicker addAmountMonths;
        private NumberPicker addAmountDays;
        private TextView addResults;
        public boolean businessMode;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            // set the fragment layout
            View rootView = inflater.inflate(R.layout.fragment_add, container, false);

            // get all the views from the layout
            addBaseDate = (Button) rootView.findViewById(R.id.add_base_date);
            addAmountLayoutYears = (LinearLayout) rootView.findViewById(R.id.add_amount_layout_years);
            addAmountLayoutMonths = (LinearLayout) rootView.findViewById(R.id.add_amount_layout_months);
            addAmountLayoutDays = (LinearLayout) rootView.findViewById(R.id.add_amount_layout_days);
            addAmountYears = (NumberPicker) rootView.findViewById(R.id.add_amount_years);
            addAmountMonths = (NumberPicker) rootView.findViewById(R.id.add_amount_months);
            addAmountDays = (NumberPicker) rootView.findViewById(R.id.add_amount_days);
            addResults = (TextView) rootView.findViewById(R.id.add_results);

            // set the button click listener
            addBaseDate.setOnClickListener(this);

            // create an array of numbers ranging from -250 to +250
            String[] numbers = new String[501];
            for (int i = 0; i <= 500; i++) {
                numbers[i] = Integer.toString(i - 250);
            }

            // set up the year amount picker
            addAmountYears.setMaxValue(500);
            addAmountYears.setMinValue(0);
            addAmountYears.setWrapSelectorWheel(true);
            addAmountYears.setDisplayedValues(numbers);
            addAmountYears.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            addAmountYears.setOnValueChangedListener(this);

            // set up the month amount picker
            addAmountMonths.setMaxValue(500);
            addAmountMonths.setMinValue(0);
            addAmountMonths.setWrapSelectorWheel(true);
            addAmountMonths.setDisplayedValues(numbers);
            addAmountMonths.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            addAmountMonths.setOnValueChangedListener(this);

            // set up the days amount picker
            addAmountDays.setMaxValue(500);
            addAmountDays.setMinValue(0);
            addAmountDays.setWrapSelectorWheel(true);
            addAmountDays.setDisplayedValues(numbers);
            addAmountDays.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            addAmountDays.setOnValueChangedListener(this);

            // handle runtime configuration change
            if (savedInstanceState == null) {
                // get a calendar and get rid of the time of day information
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);

                // set the default date to today
                String today = sdf.format(calendar.getTime());
                addBaseDate.setText(today);

                // set the default amounts to none
                addAmountYears.setValue(250);
                addAmountMonths.setValue(250);
                addAmountDays.setValue(250);
            } else {
                // restore the date
                addBaseDate.setText(savedInstanceState.getString("addBaseDate"));

                // restore the amounts
                addAmountYears.setValue(savedInstanceState.getInt("addAmountYears"));
                addAmountMonths.setValue(savedInstanceState.getInt("addAmountMonths"));
                addAmountDays.setValue(savedInstanceState.getInt("addAmountDays"));
            }

            // update the number pickers according to the business mode
            updateNumberPickers();

            // calculate the addition
            calculateAddition();

            return rootView;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);

            // save the date and the amounts
            outState.putString("addBaseDate", (String) addBaseDate.getText());
            outState.putInt("addAmountYears", addAmountYears.getValue());
            outState.putInt("addAmountMonths", addAmountMonths.getValue());
            outState.putInt("addAmountDays", addAmountDays.getValue());
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();

            if (id == R.id.add_base_date) {
                // get a calendar and get rid of the time of day information
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);

                // get the button's current date
                String source = (String) addBaseDate.getText();

                // parse the button's text into the calendar as a date
                try {
                    calendar.setTime(sdf.parse(source));
                } catch (ParseException e) {
                    Log.d("onClick", "parse exception");
                }

                // get the year month and day of the calendar with the parsed date
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                // create and show a date picker fragment using the date information from the button
                DatePickerFragment fragment = DatePickerFragment.newInstance(id, year, month, day);
                fragment.setTargetFragment(this, 0);
                fragment.show(getFragmentManager(), "add_date_picker");
            }
        }

        @Override
        public void onDatePickerDialogDone(int id, int year, int month, int day) {
            // get a calendar and get rid of the time of day information
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            // set the year month and day to the chosen date
            calendar.set(year, month, day);

            // get a string for the chosen date
            String newDateText = sdf.format(calendar.getTime());

            // update the button's text to be the new chosen date
            if (id == R.id.add_base_date) {
                addBaseDate.setText(newDateText);
            }

            // calculate the addition
            calculateAddition();
        }

        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            // calculate the addition
            calculateAddition();
        }

        public void updateNumberPickers() {
            // toggle the year and month pickers based on the business mode
            addAmountYears.setEnabled(!businessMode);
            addAmountMonths.setEnabled(!businessMode);

            // toggle the visibility of the year and month pickers based on the business mode
            if (businessMode) {
                addAmountLayoutYears.setVisibility(View.GONE);
                addAmountLayoutMonths.setVisibility(View.GONE);
            } else {
                addAmountLayoutYears.setVisibility(View.VISIBLE);
                addAmountLayoutMonths.setVisibility(View.VISIBLE);
            }
        }

        public void calculateAddition() {
            // get a calendar and get rid of the time of day information
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            // try to set the calendar to the date from the button's text
            try {
                Date date = sdf.parse((String) addBaseDate.getText());
                calendar.setTime(date);
            } catch (ParseException e) {
                Log.d("calculateAddition", "parse exception");
            }

            // get the amount of years months and days to add
            int amountYears = addAmountYears.getValue();
            int amountMonths = addAmountMonths.getValue();
            int amountDays = addAmountDays.getValue();

            if (businessMode) {
                // adjust the amount of days
                int amount = amountDays - 250;

                // keep adding or subtracting days, skipping weekends until the amount is satisfied
                if (amount > 0) {
                    while (amount > 0) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1);
                        if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY && calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                            amount--;
                        }
                    }
                } else if (amount < 0) {
                    while (amount < 0) {
                        calendar.add(Calendar.DAY_OF_YEAR, -1);
                        if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY && calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                            amount++;
                        }
                    }
                }

                // update the result text view with the results
                addResults.setText(sdf.format(calendar.getTime()));
            } else {
                // add the adjusted amounts
                calendar.add(Calendar.YEAR, amountYears - 250);
                calendar.add(Calendar.MONTH, amountMonths - 250);
                calendar.add(Calendar.DAY_OF_YEAR, amountDays - 250);

                // update the result text view with the results
                addResults.setText(sdf.format(calendar.getTime()));
            }
        }
    }

    // compare mode fragment to handle comparing dates
    public static class CompareModeFragment extends Fragment implements View.OnClickListener, DatePickerDialogListener {

        private SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd [EEE]", Locale.US);
        private Button compareDate1;
        private Button compareDate2;
        private TextView compareResults;
        public boolean businessMode;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            // set the fragment layout
            View rootView = inflater.inflate(R.layout.fragment_compare, container, false);

            // get all the views from the layout
            compareDate1 = (Button) rootView.findViewById(R.id.compare_date_1);
            compareDate2 = (Button) rootView.findViewById(R.id.compare_date_2);
            compareResults = (TextView) rootView.findViewById(R.id.compare_results);

            // set the button click listeners
            compareDate1.setOnClickListener(this);
            compareDate2.setOnClickListener(this);

            // handle runtime configuration change
            if (savedInstanceState == null) {
                // get a calendar and get rid of the time of day information
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);

                // set the default dates to today
                String today = sdf.format(calendar.getTime());
                compareDate1.setText(today);
                compareDate2.setText(today);
            } else {
                // restore the dates
                compareDate1.setText(savedInstanceState.getString("compareDate1"));
                compareDate2.setText(savedInstanceState.getString("compareDate2"));
            }

            // calculate the difference
            calculateDifference();

            return rootView;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);

            // save the dates
            outState.putString("compareDate1", (String) compareDate1.getText());
            outState.putString("compareDate2", (String) compareDate2.getText());
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();

            if (id == R.id.compare_date_1 || id == R.id.compare_date_2) {
                // get a calendar and get rid of the time of day information
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);

                // get the source date from the button text
                String source;
                if (id == R.id.compare_date_1) {
                    source = (String) compareDate1.getText();
                } else {
                    source = (String) compareDate2.getText();
                }

                // parse the date into the calendar from the button text
                try {
                    calendar.setTime(sdf.parse(source));
                } catch (ParseException e) {
                    Log.d("onClick", "parse exception");
                }

                // get the year month and day of the calendar with the parsed date
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                // create and show a date picker fragment using the date information from the button
                DatePickerFragment fragment = DatePickerFragment.newInstance(id, year, month, day);
                fragment.setTargetFragment(this, 0);
                fragment.show(getFragmentManager(), "compare_date_picker");
            }
        }

        @Override
        public void onDatePickerDialogDone(int id, int year, int month, int day) {
            // get a calendar and get rid of the time of day information
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            // set the year month and day to the chosen date
            calendar.set(year, month, day);

            // get a string for the chosen date
            String newDateText = sdf.format(calendar.getTime());

            // update the button's text to the new chosen date
            if (id == R.id.compare_date_1) {
                compareDate1.setText(newDateText);
            } else if (id == R.id.compare_date_2) {
                compareDate2.setText(newDateText);
            }

            // calculate the difference
            calculateDifference();
        }

        public void calculateDifference() {
            // get two calendars
            Calendar calendar1 = Calendar.getInstance();
            Calendar calendar2 = Calendar.getInstance();

            // get rid of the first calendar's time of day information
            calendar1.set(Calendar.HOUR_OF_DAY, 0);
            calendar1.set(Calendar.MINUTE, 0);
            calendar1.set(Calendar.SECOND, 0);

            // get rid of the second calendar's time of day information
            calendar2.set(Calendar.HOUR_OF_DAY, 0);
            calendar2.set(Calendar.MINUTE, 0);
            calendar2.set(Calendar.SECOND, 0);

            // parse the dates into the calendars from the buttons' text
            try {
                Date date1 = sdf.parse((String) compareDate1.getText());
                Date date2 = sdf.parse((String) compareDate2.getText());

                calendar1.setTime(date1);
                calendar2.setTime(date2);

                if (calendar2.getTimeInMillis() < calendar1.getTimeInMillis()) {
                    calendar1.setTime(date2);
                    calendar2.setTime(date1);
                }
            } catch (ParseException e) {
                Log.d("calculateDifference", "parse exception");
            }

            if (businessMode) {
                // get the two times
                long time1 = calendar1.getTimeInMillis();
                long time2 = calendar2.getTimeInMillis();

                // get their total difference in days
                long difference = Math.abs(time2 - time1);
                long totalDays = difference / (1000 * 60 * 60 * 24);

                // get the days until the first sunday, and the days from the last sunday
                long preDays = DateCalcHelper.daysUntilSunday(calendar1);
                long postDays = DateCalcHelper.daysFromSunday(calendar2);

                // get the number of weeks between the first and last sundays
                long weeks = (totalDays - preDays - postDays) / 7;

                // calculate the days based on the number of weeks and the excess pre and post days
                long days = Math.max(0, preDays - 2) + postDays + (weeks * 5);

                // update the result text view with the results
                compareResults.setText(days + " business days");
            } else {
                // get the two times
                long time1 = calendar1.getTimeInMillis();
                long time2 = calendar2.getTimeInMillis();

                // get their total difference in days based on an assumed amount of days in a year
                long difference = Math.abs(time1 - time2);
                double yearInDays = 365.2425;
                double daysInMonth = yearInDays / 12;
                long totalDays = difference / (1000 * 60 * 60 * 24);

                // get assumed amount of years, months, and days
                long years = (long) Math.floor(totalDays / yearInDays);
                long months = (long) Math.floor((totalDays - (years * yearInDays)) / daysInMonth);
                long days = (long) Math.floor(totalDays - (years * yearInDays) - (months * daysInMonth));

                // update the result text view with the results
                compareResults.setText(years + " years\n" + months + " months\n" + days + " days\n\n[" + totalDays + " total days]");
            }
        }
    }

    // date picker fragment used as a dialog to pick a date
    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        public static DatePickerFragment newInstance(int id, int year, int month, int day) {
            // create a new instance
            DatePickerFragment datePickerFragment = new DatePickerFragment();

            // create and attach a bundle with the id of the button that initiated the dialog, and the date information
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putInt("year", year);
            args.putInt("month", month);
            args.putInt("day", day);
            datePickerFragment.setArguments(args);

            return datePickerFragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // get the date information
            int year = getArguments().getInt("year");
            int month = getArguments().getInt("month");
            int day = getArguments().getInt("day");

            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            // use the handler to handle the chosen date
            ((DatePickerDialogListener) getTargetFragment()).onDatePickerDialogDone(getArguments().getInt("id"), year, month, day);
        }
    }

}
