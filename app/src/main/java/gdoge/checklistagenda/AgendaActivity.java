package gdoge.checklistagenda;

import android.app.FragmentManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class AgendaActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
        , DirectoryEntryFragment.OnListFragmentInteractionListener
        , NoteFragment.NoteFragmentInteractionListener
        , RenameDialogFragment.NoticeDialogListener {
    private DirectoryEntryFragment directoryFragment;
    private NoteFragment noteFragment;
    private int fragmentState;

    public static final int STATE_DIRECTORY = 0;
    public static final int STATE_NOTE = 1;

    private NoteDBHelper dbHelper;
    private CalendarDBHelper cdbHelper;
    private FragmentManager fm;
    private DirectoryEntryFragment.DirectoryInfo info;
    private FloatingActionButton fab;
    private Menu actionBarMenu;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agenda);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        onCreateOptionsMenu(toolbar.getMenu());

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fabAction();
            }
        });

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        dbHelper = new NoteDBHelper(this);
        cdbHelper = new CalendarDBHelper(this);

        directoryFragment = new DirectoryEntryFragment();

        fm = getFragmentManager();
        fm.beginTransaction()
                .add(R.id.fragment_frame, directoryFragment)
                .commit();
        fragmentState = STATE_DIRECTORY;
    }

    public void updateNavHeader() {
        ImageView image = (ImageView)  navigationView.getHeaderView(0).findViewById(R.id.header_image);
        TextView weekday = (TextView) navigationView.getHeaderView(0).findViewById(R.id.header_weekday);
        TextView progress = (TextView) navigationView.getHeaderView(0).findViewById(R.id.header_progress);

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

        String date = new SimpleDateFormat("EEEE MMMM dd, yyyy").format(new Date());
        weekday.setText(date);

        date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        SQLiteDatabase db = cdbHelper.getReadableDatabase();
        Cursor cursor = db.query(CalendarActivity.DBContract.CalendarContent.TABLE_NAME,
                new String[] {CalendarActivity.DBContract.CalendarContent.COLUMN_CHECKED},
                CalendarActivity.DBContract.CalendarContent.COLUMN_DATE + " = ?",
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
        getMenuInflater().inflate(R.menu.agenda, menu);
        actionBarMenu = menu;
        actionBarMenu.findItem(R.id.action_rename_title).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_rename_title) {
            if(fragmentState == STATE_NOTE) {
                DialogFragment dialog = new RenameDialogFragment();
                dialog.show(getSupportFragmentManager(), "rename");
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void updateIcons(int state) {
        if(state == STATE_DIRECTORY) {
            fab.setImageResource(R.drawable.ic_new_note);
            actionBarMenu.findItem(R.id.action_rename_title).setVisible(false);
        } else if (state == STATE_NOTE) {
            fab.setImageResource(R.drawable.ic_plus);
            actionBarMenu.findItem(R.id.action_rename_title).setVisible(true);
        }
    }

    //for DirectoryFragment
    @Override
    public void onListFragmentInteraction(NoteArgs args) {
        noteFragment = NoteFragment.newInstance(1, args.eid);
        fm.beginTransaction()
                .replace(R.id.fragment_frame, noteFragment)
                .addToBackStack(null)
                .commit();
        setFragmentState(STATE_NOTE);
        ActionBar ab = getSupportActionBar();
        if(ab != null) {
            ab.setTitle(args.title);
        }
        updateIcons(AgendaActivity.STATE_NOTE);
    }

    @Override
    public void onPositiveClick(DialogFragment dialog) {
        View text = dialog.getDialog().findViewById(R.id.dialog_rename_input);
        if(text != null) {
            getSupportActionBar().setTitle(((EditText) text).getText().toString());
        }
    }

    //for NoteFragment
    @Override
    public void updateDirectoryEntry(DirectoryEntryFragment.DirectoryInfo info) {
        this.info = info;
    }

    public DirectoryEntryFragment.DirectoryInfo retrieveUpdatedDirectoryEntry() {
        return info;
    }

    public void clearUpdatedDirectoryEntry() {
        info = null;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_calendar) {
            /*
            Intent intent = new Intent(this, CalendarActivity.class);
            this.startActivity(intent);
            */
            NavUtils.navigateUpFromSameTask(this);
        } else if (id == R.id.nav_notebook) {
            //already in notebook
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public SQLiteDatabase getDatabase() {
        return dbHelper.getWritableDatabase();
    }

    public void setFragmentState(int state) {
        fragmentState = state;
    }

    public void fabAction() {
        if(fragmentState == STATE_DIRECTORY) {
            directoryFragment.createNewEntry();
        } else if (fragmentState == STATE_NOTE) {
            noteFragment.createNewEntry();
        }
    }

    public void setToggleState(boolean state) {
        if(state) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            toggle.setDrawerIndicatorEnabled(true);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawer.openDrawer(Gravity.LEFT);
                }
            });
        } else {
            toggle.setDrawerIndicatorEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }
        toggle.syncState();
    }

    public static class NoteArgs {
        public int eid;
        public String title;

        public NoteArgs(int eid, String title) {
            this.eid = eid;
            this.title = title;
        }
    }

    public class DBContract {
        private DBContract() {}

        public static final String directory_dbcreate = "CREATE TABLE " + DirectoryEntries.TABLE_NAME +
                " (" + DirectoryEntries._ID + " INTEGER PRIMARY KEY," + DirectoryEntries.COLUMN_NAME
                + " TEXT," + DirectoryEntries.COLUMN_EID + " INTEGER," + DirectoryEntries.COLUMN_PROGRESS
                + " INTEGER," + DirectoryEntries.COLUMN_PREVIEW + " INTEGER)";

        public static final String note_entries_dbcreate = "CREATE TABLE " + NoteEntries.TABLE_NAME +
                " (" + NoteEntries._ID + " INTEGER PRIMARY KEY," + NoteEntries.COLUMN_EID +
                " INTEGER," + NoteEntries.COLUMN_LINE_ID + " INTEGER," + NoteEntries.COLUMN_CHECKED +
                " INTEGER," + NoteEntries.COLUMN_TEXT + " TEXT)";

        public static final String directory_dbdelete = "DROP TABLE IF EXISTS " + DirectoryEntries.TABLE_NAME;

        public static final String note_entries_dbdelete = "DROP TABLE IF EXISTS " + NoteEntries.TABLE_NAME;

        public class DirectoryEntries implements BaseColumns {
            public static final String TABLE_NAME = "otherdirectory";
            public static final String COLUMN_NAME = "name";
            public static final String COLUMN_EID = "entryid";
            public static final String COLUMN_PROGRESS = "progress";
            public static final String COLUMN_PREVIEW = "preview";
        }

        public class NoteEntries implements BaseColumns {
            public static final String TABLE_NAME = "otherentries";
            public static final String COLUMN_EID = "entryid";
            public static final String COLUMN_LINE_ID = "lineid";
            public static final String COLUMN_CHECKED = "checked";
            public static final String COLUMN_TEXT = "text";
        }
    }
}
