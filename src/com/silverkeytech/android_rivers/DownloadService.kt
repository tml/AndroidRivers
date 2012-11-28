package com.silverkeytech.android_rivers

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.github.kevinsawicki.http.HttpRequest
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException
import android.os.Environment
import java.io.File
import android.app.Activity
import android.os.Messenger
import android.os.Message
import android.os.RemoteException
import android.support.v4.app.NotificationCompat
import android.app.PendingIntent
import android.widget.RemoteViews
import android.content.Context
import android.app.NotificationManager
import android.app.Notification
import java.util.Random


public class DownloadService() : IntentService("DownloadService"){
    class object{
        public val PARAM_DOWNLOAD_URL : String= "downloadUrl"
        public val PARAM_DOWNLOAD_TITLE : String = "downloadTitle"

        public val TAG: String = javaClass<DownloadService>().getSimpleName()!!
    }

    var targetUrl : String? = null
    var targetTitle : String? = null

    fun prepareNotification(inferredName : String) : Notification{
        var notificationIntent = Intent(this, javaClass<TryOutActivity>())
        var contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        var notification = NotificationCompat.Builder(this)
                .setTicker("Podcast starts downloading")
        ?.setSmallIcon(android.R.drawable.gallery_thumb)
        ?.setProgress(100,10, true)
        ?.setWhen(System.currentTimeMillis())
        ?.setContentIntent(contentIntent)
        ?.build()

        notification!!.contentView = RemoteViews(getApplicationContext()!!.getPackageName(), R.layout.download_progress)

        notification!!.contentView!!.setImageViewResource(R.id.download_progress_status_icon, android.R.drawable.btn_star);
        notification!!.contentView!!.setProgressBar(R.id.download_progress_status_progress, 100, 10, false)
        notification!!.contentView!!.setTextViewText(R.id.download_progress_status_text, "Downloading $targetTitle")

        return notification!!
    }

    protected override fun onHandleIntent(p0: Intent?) {
        targetUrl = p0!!.getStringExtra(PARAM_DOWNLOAD_URL)
        targetTitle = p0!!.getStringExtra(PARAM_DOWNLOAD_TITLE)

        Log.d(TAG, "onHandleIntent with ${targetUrl}")

        var inferredName = getFileNameFromUri(targetUrl!!)

        var result = Activity.RESULT_CANCELED
        var filename : String = ""

        var notification : Notification? = null

        var notificationId = Random().nextLong().toInt()
        var notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        try{
            var req = HttpRequest.get(targetUrl)

            if (inferredName == null)
                inferredName = generateThrowawayName() + ".mp3"

            notification = prepareNotification(inferredName!!)

            var directory = Environment.getExternalStorageDirectory()!!.getPath() + "/" + Environment.DIRECTORY_PODCASTS
            filename  = directory + "/" + inferredName

            Log.d(TAG, "Podcast to be stored at ${filename}")

            notificationManager.notify(notificationId, notification)

            var output = File(filename)
            req!!.receive(output)

            notification!!.contentView!!.setTextViewText(R.id.download_progress_status_text, "File successfully download to $filename")
            notificationManager.notify(notificationId, notification)

            result = Activity.RESULT_OK
        }
        catch(e : HttpRequestException){
            Log.d(TAG, "Exception happend at attempt to download ${e.getMessage()}")
            notification!!.contentView!!.setTextViewText(R.id.download_progress_status_text, "File $inferredName download cancelled")
            notificationManager.notify(notificationId, notification)
        }

        var extras = p0.getExtras()
        if (extras != null){
            var messenger = extras!!.get(Params.MESSENGER) as android.os.Messenger
            var msg = Message.obtain()!!
            msg.arg1 = result
            msg.obj = filename
            try{
                messenger.send(msg)
            }
            catch(e : RemoteException){
                Log.d(TAG, "Have problem when try to send a message ")
            }
        }
    }

    public override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "OnStartCommand  with ${targetUrl}")

        return super<IntentService>.onStartCommand(intent, flags, startId)
    }

    public override fun onCreate() {
        super<IntentService>.onCreate()
        Log.d(TAG, "Service created  with ${targetUrl}")
    }

    public override fun onStart(intent: Intent?, startId: Int) {
        super<IntentService>.onStart(intent, startId)
        Log.d(TAG, "Service started  with ${targetUrl}")
    }

    public override fun onDestroy() {
        super<IntentService>.onDestroy()
        Log.d(TAG, "Service destroyed  with ${targetUrl}")
    }
}
