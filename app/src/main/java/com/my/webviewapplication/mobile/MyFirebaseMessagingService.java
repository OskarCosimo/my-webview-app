package com.my.webviewapplication.mobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Firebase Cloud Messaging service to handle incoming push notifications and token refreshes.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "mywebview_notifications";
    private static final AtomicInteger NOTIFICATION_ID_GENERATOR =
            new AtomicInteger((int) (System.currentTimeMillis() & 0x7FFFFFFF));

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (BuildConfig.IS_DEBUG_MODE) {
            Log.d(TAG, "Message received from: " + remoteMessage.getFrom());
        }

        Map<String, String> data = remoteMessage.getData();
        String title = null;
        String body = null;
        String clickUrl = null;

        if (data != null && !data.isEmpty()) {
            if (BuildConfig.IS_DEBUG_MODE) {
                Log.d(TAG, "Message data payload: " + data);
            }
            title = data.get("title");
            body = data.get("body");
            if (TextUtils.isEmpty(body)) {
                body = data.get("message");
            }
            clickUrl = data.get("click_action");
            if (TextUtils.isEmpty(clickUrl)) {
                clickUrl = data.get("url");
            }
        }

        if (remoteMessage.getNotification() != null) {
            if (!TextUtils.isEmpty(remoteMessage.getNotification().getTitle())) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (!TextUtils.isEmpty(remoteMessage.getNotification().getBody())) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        if (!TextUtils.isEmpty(title) || !TextUtils.isEmpty(body)) {
            if (TextUtils.isEmpty(title)) {
                title = getString(R.string.app_name);
            }
            showNotification(title, body, sanitizeNotificationUrl(clickUrl));
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        if (BuildConfig.IS_DEBUG_MODE) {
            Log.d(TAG, "New FCM token generated: " + token);
        }
        sendTokenToServer(token);
    }

    private void sendTokenToServer(@NonNull String token) {
        // Implement backend token synchronization here if required
    }

    @Nullable
    private String sanitizeNotificationUrl(@Nullable String rawUrl) {
        if (TextUtils.isEmpty(rawUrl)) {
            return null;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (!Config.LOAD_ONLY_HTTPS && trimmed.startsWith("http://")) {
            return trimmed;
        }
        Log.w(TAG, "Blocked unsafe notification URL scheme");
        return null;
    }

    private void showNotification(@NonNull String title,
                                  @Nullable String message,
                                  @Nullable String url) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        ensureNotificationChannel(notificationManager);

        int notificationId = NOTIFICATION_ID_GENERATOR.incrementAndGet();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (!TextUtils.isEmpty(url)) {
            intent.putExtra("notification_url", url);
        }

        int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationId, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message != null ? message : "")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message != null ? message : ""))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        notificationManager.notify(notificationId, builder.build());
    }

    private void ensureNotificationChannel(@NonNull NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                CharSequence name = getString(R.string.channel_name);
                String description = getString(R.string.channel_description);
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        name,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription(description);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
