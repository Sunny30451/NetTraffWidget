package com.sunny.networktrafficwidget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrafficUpdateService extends Service {
    private static final String CHANNEL_ID = "traffic_widget_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long INTERVAL_MS = 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastRx = -1;
    private long lastTx = -1;
    private long lastTime = -1;

    private final Runnable updater = new Runnable() {
        @Override
        public void run() {
            TrafficSnapshot current = calculateSnapshot();
            updateWidgets(TrafficUpdateService.this, current);
            handler.postDelayed(this, INTERVAL_MS);
        }
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, TrafficUpdateService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, TrafficUpdateService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        lastRx = safeBytes(TrafficStats.getTotalRxBytes());
        lastTx = safeBytes(TrafficStats.getTotalTxBytes());
        lastTime = SystemClock.elapsedRealtime();
        handler.post(updater);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(updater);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private TrafficSnapshot calculateSnapshot() {
        long now = SystemClock.elapsedRealtime();
        long rx = safeBytes(TrafficStats.getTotalRxBytes());
        long tx = safeBytes(TrafficStats.getTotalTxBytes());
        long elapsed = Math.max(1L, now - lastTime);

        long rxPerSec = Math.max(0L, (rx - lastRx) * 1000L / elapsed);
        long txPerSec = Math.max(0L, (tx - lastTx) * 1000L / elapsed);

        lastRx = rx;
        lastTx = tx;
        lastTime = now;

        return new TrafficSnapshot(rxPerSec, txPerSec);
    }

    public static TrafficSnapshot snapshot(Context context, long now) {
        return new TrafficSnapshot(0, 0);
    }

    public static void updateWidgets(Context context, TrafficSnapshot snapshot) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, NetworkTrafficWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(provider);
        if (ids == null || ids.length == 0) return;

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.network_widget);
        views.setTextViewText(R.id.download, "↓ " + formatBytes(snapshot.rxBytesPerSec) + "/s");
        views.setTextViewText(R.id.upload, "↑ " + formatBytes(snapshot.txBytesPerSec) + "/s");
        views.setTextViewText(R.id.updated, "Aktualisiert: " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
        manager.updateAppWidget(ids, views);
    }

    private static long safeBytes(long value) {
        return value == TrafficStats.UNSUPPORTED ? 0L : Math.max(0L, value);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.getDefault(), "%.2f MB", mb);
        return String.format(Locale.getDefault(), "%.2f GB", mb / 1024.0);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Network Traffic Widget", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Sekündliche Widget-Aktualisierung");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return builder
                .setContentTitle(getString(R.string.service_notification_title))
                .setContentText(getString(R.string.service_notification_text))
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    public static class TrafficSnapshot {
        public final long rxBytesPerSec;
        public final long txBytesPerSec;

        public TrafficSnapshot(long rxBytesPerSec, long txBytesPerSec) {
            this.rxBytesPerSec = rxBytesPerSec;
            this.txBytesPerSec = txBytesPerSec;
        }
    }
}
