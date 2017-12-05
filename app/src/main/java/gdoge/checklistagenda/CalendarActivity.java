package gdoge.checklistagenda;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CalendarActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
        , DateShortcutFragment.OpenCalendarEntryListener
        , CalendarFragment.OnCalendarInteractionListener
        , SetTimeDialogFragment.TimeDialogListener {
    private FloatingActionButton fab;
    private int state;
    private DateEntryFragment entryFragment;
    private DateShortcutFragment shortcutFragment;
    private CalendarDBHelper dbHelper;
    private Menu toolbarMenu;

    public static final String START_DATE = "initialdate";
    public static final String prefs = "myprefs";
    private static final String requestCodeKey = "requestcode";
    public SharedPreferences preferences;
    private int currentRequestCode;

    private Toolbar toolbar;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private DrawerLayout drawer;

    private final int STATE_ENTRY = 1;
    private final int STATE_CALENDAR = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        onCreateOptionsMenu(toolbar.getMenu());

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(state == STATE_ENTRY) {
                    entryFragment.addNewEntry();
                }
            }
        });

        preferences = getSharedPreferences(prefs, MODE_PRIVATE);
        currentRequestCode = preferences.getInt(requestCodeKey, 1);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        dbHelper = new CalendarDBHelper(this);

        updateNavHeader();

        shortcutFragment = new DateShortcutFragment();
        getFragmentManager()
                .beginTransaction()
                .add(R.id.scrolling_frame, shortcutFragment)
                .commit();

        getFragmentManager()
                .beginTransaction()
                .add(R.id.calendar_frame, new CalendarFragment())
                .commit();

        Intent intent = getIntent();
        String str = intent.getStringExtra(START_DATE);
        if(str == null) {
            Log.e("E", "str is null");
        }
    }

    public void updateNavHeader() {
        ImageView image = (ImageView) navigationView.getHeaderView(0).findViewById(R.id.header_image);
        TextView weekday = (TextView) navigationView.getHeaderView(0).findViewById(R.id.header_weekday);
        TextView progress = (TextView)  navigationView.getHeaderView(0).findViewById(R.id.header_progress);

        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        switch(dayOfWeek) {
            case 1:
                image.setImageResource(R.drawable.ic_sunday);
                break;
            case 2:
                image.setImageResource(R.drawable.ic_monday);
                break;
            case 3:
                image.setImageResource(R.drawable.ic_tuesday);
                break;
            case 4:
                image.setImageResource(R.drawable.ic_wednesday);
                break;
            case 5:
                image.setImageResource(R.drawable.ic_thursday);
                break;
            case 6:
                image.setImageResource(R.drawable.ic_friday);
                break;
            case 7:
                image.setImageResource(R.drawable.ic_saturday);
                break;
            default:
                break;
        }

        String date = new SimpleDateFormat("EEEE MMMM d, yyyy").format(new Date());
        weekday.setText(date);

        date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBContract.CalendarContent.TABLE_NAME,
                new String[] {DBContract.CalendarContent.COLUMN_CHECKED},
                DBContract.CalendarContent.COLUMN_DATE + " = ?",
                new String[] {date},
                null,
                null,
                null);
        int total = 0;
        int complete = 0;

        while(cursor.moveToNext()) {
            if(cursor.getInt(0) == DateEntryFragment.STATUS_CHECKED) {
                complete++;
            }
            total++;
        }
        progress.setText("Today's schedule: " + complete + " of " + total + " completed.");
        cursor.close();
    }

    @Override
    public void onNewIntent(Intent intent) {
        String initialDate = intent.getStringExtra(START_DATE);
        if(initialDate != null) {
            openCalendarEntry(initialDate);
        }
        super.onNewIntent(intent);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.calendar, menu);
        toolbarMenu = menu;
        toolbarMenu.findItem(R.id.action_delete_all).setVisible(false);
        toolbarMenu.findItem(R.id.action_sort).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_sort && state == STATE_ENTRY) {
            entryFragment.sortEntriesAscending();
            return true;
        } else if (id == R.id.action_delete_all && state == STATE_ENTRY) {
            entryFragment.deleteAllEntries();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_calendar) {
            //already in calendar
        } else if (id == R.id.nav_notebook) {
            Intent intent = new Intent(CalendarActivity.this, AgendaActivity.class);
            this.startActivity(intent);
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void openCalendarEntry(String date) { //from shortcut bar
        if(state == STATE_ENTRY) {
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        entryFragment = DateEntryFragment.newInstance(date);
        getFragmentManager().
                beginTransaction()
                .replace(R.id.calendar_frame, entryFragment)
                .addToBackStack(null)
                .commit();
        ((MyDateShortcutRecyclerViewAdapter) shortcutFragment.rView.getAdapter()).setChosenDate(date);
    }

    @Override
    public void onCalendarInteraction(String date) {
        entryFragment = DateEntryFragment.newInstance(date);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.calendar_frame, entryFragment)
                .addToBackStack(null)
                .commit();
        ((MyDateShortcutRecyclerViewAdapter) shortcutFragment.rView.getAdapter()).setChosenDate(date);
    }

    @Override
    public void onPositiveClick(DialogFragment dialog, int index) {
        if(state == STATE_ENTRY) {
            ((MyDateEntryRecyclerViewAdapter) entryFragment.rView.getAdapter()).onPositiveClick(dialog, index);
        }
    }

    public int getNextRequestCode() {
        preferences.edit().putInt(requestCodeKey, currentRequestCode+1).apply();
        return currentRequestCode++;
    }

    public void setStateCalendar() {
        //back button stuff
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        toggle.setDrawerIndicatorEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.openDrawer(Gravity.LEFT);
            }
        });

        //toolbar stuff
        toolbarMenu.findItem(R.id.action_delete_all).setVisible(false);
        toolbarMenu.findItem(R.id.action_sort).setVisible(false);

        fab.setVisibility(View.GONE);
        getSupportActionBar().setTitle(R.string.action_bar_calendar);
        state = STATE_CALENDAR;
    }

    public void setStateEntry(String date) {
        //back button stuff
        toggle.setDrawerIndicatorEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //toolbar stuff
        toolbarMenu.findItem(R.id.action_delete_all).setVisible(true);
        toolbarMenu.findItem(R.id.action_sort).setVisible(true);

        fab.setVisibility(View.VISIBLE);
        getSupportActionBar().setTitle(toPresentableString(Integer.parseInt(date)));
        state = STATE_ENTRY;
    }

    public SQLiteDatabase getDatabase() {
        return dbHelper.getWritableDatabase();
    }

    public String toPresentableString(int date) {
        int day = date%100;
        date = date/100;
        int month = date%100;
        int year = date/100;
        switch(month) {
            case 1:
                return "January " + day + ", " + year;
            case 2:
                return "February " + day + ", " + year;
            case 3:
                return "March " + day + ", " + year;
            case 4:
                return "April " + day + ", " + year;
            case 5:
                return "May " + day + ", " + year;
            case 6:
                return "June " + day + ", " + year;
            case 7:
                return "July " + day + ", " + year;
            case 8:
                return "August " + day + ", " + year;
            case 9:
                return "September " + day + ", " + year;
            case 10:
                return "October " + day + ", " + year;
            case 11:
                return "November " + day + ", " + year;
            case 12:
                return "December " + day + ", " + year;
            default:
                return "";
        }
    }

    public class DBContract {
        private DBContract() {}

        public static final String cEntry_dbcreate = "CREATE TABLE " + CalendarEntry.TABLE_NAME +
                "( " + CalendarEntry._ID + " INTEGER PRIMARY KEY," +  CalendarEntry.COLUMN_DATE +
                " INTEGER," + CalendarEntry.COLUMN_OVERVIEW + " TEXT)";

        public static final String cContent_dbcreate = "CREATE TABLE " + CalendarContent.TABLE_NAME +
                "( " + CalendarContent._ID + " INTEGER PRIMARY KEY," + CalendarContent.COLUMN_ID + " INTEGER," +
                CalendarContent.COLUMN_DATE + " INTEGER," + CalendarContent.COLUMN_ALARMINFO + " TEXT,"
                + CalendarContent.COLUMN_CHECKED + " INTEGER," + CalendarContent.COLUMN_CONTENT + " TEXT," +
                CalendarContent.COLUMN_REQUESTCODE + " INTEGER)";

        public static final String cAlarms_dbcreate = "CREATE TABLE " + AlarmReferences.TABLE_NAME +
                "( " + AlarmReferences._ID + " INTEGER PRIMARY KEY," + AlarmReferences.COLUMN_KEY +
                " INTEGER," + AlarmReferences.COLUMN_DATE + " TEXT," + AlarmReferences.COLUMN_TIME +
                " TEXT)";

        public static final String cEntry_dbdelete = "DROP TABLE IF EXISTS " + CalendarEntry.TABLE_NAME;
        public static final String cContent_dbdelete = "DROP TABLE IF EXISTS " + CalendarContent.TABLE_NAME;
        public static final String cAlarms_dbdelete = "DROP TABLE IF EXISTS " + AlarmReferences.TABLE_NAME;

        public class CalendarEntry implements BaseColumns {
            public static final String TABLE_NAME = "calendarentry";
            public static final String COLUMN_DATE = "date";
            public static final String COLUMN_OVERVIEW = "overview";
        }

        public class CalendarContent implements BaseColumns {
            public static final String TABLE_NAME = "calendarcontent";
            public static final String COLUMN_ID = "orderid";
            //date is YYYYMMDD
            public static final String COLUMN_DATE = "date";
            public static final String COLUMN_ALARMINFO = "time";
            public static final String COLUMN_CHECKED = "checked";
            public static final String COLUMN_CONTENT = "content";
            public static final String COLUMN_REQUESTCODE = "rq";
        }

        public class AlarmReferences implements BaseColumns {
            public static final String TABLE_NAME="alarms";
            public static final String COLUMN_KEY="key";
            public static final String COLUMN_DATE="date";
            public static final String COLUMN_TIME="time";
        }
    }
}
