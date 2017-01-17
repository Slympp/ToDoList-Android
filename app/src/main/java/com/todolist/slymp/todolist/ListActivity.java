package com.todolist.slymp.todolist;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.todolist.slymp.todolist.db.TaskContract;
import com.todolist.slymp.todolist.db.TaskDbHelper;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static java.lang.System.currentTimeMillis;

public class ListActivity extends AppCompatActivity {

    private static final String     TAG = "ListActivity";
    private TaskDbHelper            mHelper;
    private ListView                mTaskListView;
    private MyAdapter               mAdapter;
    private boolean                 isAlphabetical;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        isAlphabetical = true;
        TimeZone.setDefault(TimeZone.getTimeZone("Paris"));
        mHelper = new TaskDbHelper(this);
        mTaskListView = (ListView) findViewById(R.id.task_list);

        try {
            updateList();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            updateList();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Item> generateData() throws ParseException {
        ArrayList<Item> items = new ArrayList<Item>();
        SQLiteDatabase db = mHelper.getReadableDatabase();
        String query = "SELECT * FROM " + TaskContract.TaskEntry.TABLE + " ORDER BY status DESC, ";
        Log.d(TAG, "Alphabetic: " + isAlphabetical);
        if (isAlphabetical) {
            query +=  "title COLLATE NOCASE";
        } else {
            query +=  "due_time";
        }
        Cursor cursor = db.rawQuery(query, null);
        while(cursor.moveToNext()) {
            Log.d(TAG, "A la création de l'item: " + Integer.toString(cursor.getInt(cursor.getColumnIndex(
                    TaskContract.TaskEntry.COL_TASK_DUE_TIME))));
            Item _item = new Item(cursor.getString(cursor.getColumnIndex(TaskContract.TaskEntry.COL_TASK_TITLE)),
                            cursor.getString(cursor.getColumnIndex(TaskContract.TaskEntry.COL_TASK_DESC)),
                            Integer.toString(cursor.getInt(cursor.getColumnIndex(
                            TaskContract.TaskEntry.COL_TASK_DUE_TIME))),
                            cursor.getInt(cursor.getColumnIndex(TaskContract.TaskEntry._ID)),
                            cursor.getString(cursor.getColumnIndex(TaskContract.TaskEntry.COL_TASK_STATUS)));

            // Format and fill date from Unix timeStamp
            DateFormat sdfInit;
            DateFormat sdfRender;

            long timeStamp = Long.parseLong(cursor.getString(cursor.getColumnIndex(TaskContract.TaskEntry.COL_TASK_DUE_TIME)));
            Date newDate = new Date(timeStamp*1000L);

            long realtimeTimeStamp = System.currentTimeMillis() / 1000L;
            Date realtimeDate = new Date(realtimeTimeStamp*1000L);

            sdfInit = new SimpleDateFormat("dd/MM/yyyy");
            String initFormat = sdfInit.format(realtimeDate);
            String renderFormat;

            if (timeStamp > getStartOfDayInMillis(initFormat) && timeStamp < getEndOfDayInMillis(initFormat)) {
                if (realtimeTimeStamp < timeStamp) {
                    sdfRender = new SimpleDateFormat("H:mm");
                    renderFormat = sdfRender.format(newDate);
                } else
                    renderFormat = "TooLate!";
            } else {
                sdfRender = new SimpleDateFormat("dd/MM/yyyy");
                renderFormat = sdfRender.format(newDate);
            }


            _item.setDue_time(renderFormat);
            _item.setTimestamp_dt(Integer.toString(cursor.getInt(cursor.getColumnIndex(
                    TaskContract.TaskEntry.COL_TASK_DUE_TIME))));

            items.add(_item);
        }

        cursor.close();
        db.close();
        return items;
    }

    protected void updateList() throws ParseException {

        mAdapter = new MyAdapter(this, generateData());
        mTaskListView.setAdapter(mAdapter);
    }

    // TODO : Implementer filtering bar (http://envyandroid.com/align-tabhost-at-bottom/)

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_list_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_task:
                Intent intent = new Intent(ListActivity.this, CreateActivity.class);
                startActivity(intent);
                return true;

                // TODO : Implement swap order (alphabetical <-> due-time)
                case R.id.action_swap_order:
                isAlphabetical = !isAlphabetical;
                    try {
                        updateList();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public long getStartOfDayInMillis(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(format.parse(date));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis() / 1000;
    }
    
    public long getEndOfDayInMillis(String date) throws ParseException {
        // Add one day's time to the beginning of the day.
        // 24 hours * 60 minutes * 60 seconds * 1000 milliseconds = 1 day
        return getStartOfDayInMillis(date) + (24 * 60 * 60);
    }
}


