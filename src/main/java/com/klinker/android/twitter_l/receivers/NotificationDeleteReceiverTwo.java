package com.klinker.android.twitter_l.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.klinker.android.twitter_l.services.MarkReadSecondAccService;

/**
 * Created by luke on 7/24/14.
 */
public class NotificationDeleteReceiverTwo extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("notification_deleted_talon", "starting receiver for notification deleted on account 2");
        context.startService(new Intent(context, MarkReadSecondAccService.class));
    }
}