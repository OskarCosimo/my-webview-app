package com.my.webviewapplication.mobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.multidex.MultiDexApplication;

import com.google.firebase.FirebaseApp;

public class MyWebviewApp extends MultiDexApplication {
    
    private static final String CHANNEL_ID = "mwa_notifications";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        if (Config.ENABLE_FIREBASE_PUSH) {
            FirebaseApp.initializeApp(this);
        }
        
        // Create notification channel
        createNotificationChannel();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
