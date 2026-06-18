package com.sunny.networktrafficwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.SystemClock;
import android.widget.RemoteViews;

public class NetworkTrafficWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        TrafficUpdateService.start(context);
        TrafficUpdateService.updateWidgets(context, TrafficUpdateService.snapshot(context, SystemClock.elapsedRealtime()));
    }

    @Override
    public void onEnabled(Context context) {
        TrafficUpdateService.start(context);
    }

    @Override
    public void onDisabled(Context context) {
        TrafficUpdateService.stop(context);
    }
}
