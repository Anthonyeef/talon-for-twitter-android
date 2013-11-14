package com.klinker.android.talon.SQLite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import twitter4j.DirectMessage;
import twitter4j.MediaEntity;
import twitter4j.Status;

import java.util.ArrayList;
import java.util.List;

public class DMDataSource {

    // Database fields
    private SQLiteDatabase database;
    private DMSQLiteHelper dbHelper;
    public String[] allColumns = {DMSQLiteHelper.COLUMN_ID, DMSQLiteHelper.COLUMN_TYPE,
            DMSQLiteHelper.COLUMN_TEXT, DMSQLiteHelper.COLUMN_NAME, DMSQLiteHelper.COLUMN_PRO_PIC,
            DMSQLiteHelper.COLUMN_SCREEN_NAME, DMSQLiteHelper.COLUMN_TIME, DMSQLiteHelper.COLUMN_PIC_URL, DMSQLiteHelper.COLUMN_RETWEETER };

    public DMDataSource(Context context) {
        dbHelper = new DMSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void createDirectMessage(DirectMessage status) {
        ContentValues values = new ContentValues();
        long time = status.getCreatedAt().getTime();

        values.put(DMSQLiteHelper.COLUMN_TEXT, status.getText());
        values.put(DMSQLiteHelper.COLUMN_ID, status.getId());
        values.put(DMSQLiteHelper.COLUMN_NAME, status.getSender().getName());
        values.put(DMSQLiteHelper.COLUMN_PRO_PIC, status.getSender().getBiggerProfileImageURL());
        values.put(DMSQLiteHelper.COLUMN_SCREEN_NAME, status.getSender().getScreenName());
        values.put(DMSQLiteHelper.COLUMN_TIME, time);
        values.put(DMSQLiteHelper.COLUMN_RETWEETER, status.getRecipientScreenName());

        MediaEntity[] entities = status.getMediaEntities();

        if (entities.length > 0) {
            values.put(DMSQLiteHelper.COLUMN_PIC_URL, entities[0].getMediaURL());
        }
        database.insert(DMSQLiteHelper.TABLE_DM, null, values);
    }

    public void deleteTweet(Tweet tweet) {
        long id = tweet.getId();
        System.out.println("Comment deleted with id: " + id);
        database.delete(DMSQLiteHelper.TABLE_DM, DMSQLiteHelper.COLUMN_ID
                + " = " + id, null);
    }

    public List<Tweet> getAllTweets() {
        List<Tweet> tweets = new ArrayList<Tweet>();

        Cursor cursor = getCursor();

        cursor.moveToLast();
        while (!cursor.isBeforeFirst()) {
            Tweet tweet = cursorToTweet(cursor);
            tweets.add(tweet);
            cursor.moveToPrevious();
        }
        // make sure to close the cursor
        cursor.close();
        return tweets;
    }

    public void deleteAllTweets() {
        database.delete(DMSQLiteHelper.TABLE_DM, null, null);
    }

    public Cursor getCursor() {
        Cursor cursor = database.query(DMSQLiteHelper.TABLE_DM,
                allColumns, null, null, null, null, null);

        return cursor;
    }

    private Tweet cursorToTweet(Cursor cursor) {
        Tweet tweet = new Tweet();
        tweet.setId(cursor.getLong(0));
        tweet.setTweet(cursor.getString(1));
        tweet.setName(cursor.getString(2));
        return tweet;
    }
}
