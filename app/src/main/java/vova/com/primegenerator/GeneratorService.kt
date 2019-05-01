package vova.com.primegenerator

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import vova.com.primegenerator.MainActivity.Companion.KEY_LIMIT
import vova.com.primegenerator.MainActivity.Companion.KEY_MESSAGE
import vova.com.primegenerator.MainActivity.Companion.KEY_PROGRESS
import vova.com.primegenerator.MainActivity.Companion.KEY_VALUES
import vova.com.primegenerator.data.Number
import vova.com.primegenerator.data.NumbersDatabase
import vova.com.primegenerator.data.PrimeNumbersDao


class GeneratorService : Service() {

    lateinit var mNotificationManager: NotificationManager
    lateinit var primeNumbersDao: PrimeNumbersDao
    lateinit var generator: PrimeGenerator
    lateinit var notificationBuilder: NotificationCompat.Builder
    var minRange = 2
    val accumulatedPrimes = ArrayList<Int>()


    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        primeNumbersDao = NumbersDatabase.getInstance(this).primeNumbersDao()
        generator = PrimeGenerator()
    }

    var isGenerating = false
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.apply {
            when (action) {
                ACTION_START_FOREGROUND_SERVICE -> {
                    val limit = intent.getIntExtra(KEY_LIMIT, 0)
                    Log.i("FFFFx", " limit = " + limit)
                    startForeground(NOTIF_ID, buildNotification(limit.toString()))
                    if (accumulatedPrimes.isEmpty() && !isGenerating) {
                        getCachedData(limit)
                    } else {
                        startComputation(limit)
                    }
                }
                ACTION_STOP_FOREGROUND_SERVICE -> {
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun buildNotification(text: String): Notification {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_NONE
        )
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)


        val stopSelf = Intent(this, GeneratorService::class.java)
        stopSelf.action = ACTION_STOP_FOREGROUND_SERVICE
        val pStopSelf = PendingIntent.getService(this, 0, stopSelf, 0)


        val intent = Intent(applicationContext, MainActivity::class.java)
        val pIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            0
        )

        notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        return notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(getString(R.string.service_generating_numbers_until, text))
            .setContentText(getString(R.string.service_primes_found))
            .setPriority(NotificationManager.IMPORTANCE_LOW)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pIntent)
            .addAction(R.drawable.ic_action_name, getString(R.string.service_cancel), pStopSelf)
            .build()
    }

    private fun stopForegroundService() {
        // Stop foreground service and remove the notification.
        stopForeground(true)
        // Stop the foreground service.
        stopSelf()
    }

    private fun splitNumberToSegments(number: Int): List<Int> {
        val breakPoint = 10000
        val segments = mutableListOf<Int>()
        var cumulativeBreakpoint = breakPoint
        if (number > breakPoint) {
            val segmentsNum = kotlin.math.ceil(number.toDouble() / breakPoint).toInt()
            for (i in 1 until segmentsNum) {
                segments.add(cumulativeBreakpoint)
                cumulativeBreakpoint += breakPoint
            }
        }
        segments.add(number)
        return segments
    }

    private fun startComputation(limit: Int) {
        isGenerating = true
        if (accumulatedPrimes.isNotEmpty() && limit <= accumulatedPrimes.last()) {
            val primes = mutableListOf<Int>()
            accumulatedPrimes.forEach {
                if (it <= limit) {
                    primes.add(it)
                }
            }
            updateView(primes.size, primes)
            updateNotification(
                primes.size.toString(),
                getString(R.string.service_generating_numbers_until, limit.toString())
            )
            toggleProgressBar(false)
            return
        }

            //todo: implement proper async cancellation
         GlobalScope.launch {
            val work1 = async(Dispatchers.Default) { generate(limit) }
            val result = work1.await()

            //if we already have cached results - write only the new ones
            if (result.last() > biggestCachedValue) {
                val index = result.indexOf(biggestCachedValue)
                val subList = result.subList(index, result.size - 1)
                 launch { writeToDb(subList) }
            }
            isGenerating = false
        }
    }

    private fun generate(limit: Int): List<Int> {
        minRange = 2
        accumulatedPrimes.clear()
        splitNumberToSegments(limit).forEach {
            generator.generate(minRange, it).apply {
                updateView(accumulatedPrimes.size + size, this)
                forEach { prime -> accumulatedPrimes.add(prime) }
                //update UI
                val size = accumulatedPrimes.size
                updateNotification(
                    size.toString(),
                    getString(R.string.service_generating_numbers_until, limit.toString())
                )
                minRange = it
            }
        }
        toggleProgressBar(false)
        return accumulatedPrimes
    }

    var biggestCachedValue = 2
    private fun getCachedData(limit: Int) {
        toggleProgressBar(true)
        GlobalScope.launch {
            val work1 = async { primeNumbersDao.getAll() }

            val numbers = work1.await()
            if (numbers.isNotEmpty()) {
                numbers.forEach { accumulatedPrimes.add(it.primeNumber) }
                biggestCachedValue = accumulatedPrimes.last()
            }
            startComputation(limit)
            toggleProgressBar(false)
        }
    }

    private fun writeToDb(list: List<Int>) {
        toggleProgressBar(true)
        val numbers = mutableListOf<Number>()
        list.forEach {
            numbers.add(Number(null, it))
        }
        primeNumbersDao.insert(numbers)
        toggleProgressBar(false)
    }

    private fun toggleProgressBar(isShown: Boolean) {
        val intent = Intent(MainActivity.ACTION_UPDATE_PROGRESS_BAR)
        intent.putExtra(KEY_PROGRESS, isShown)
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(intent)
    }

    private fun updateNotification(primes: String, limitText: String) {
        val notification = notificationBuilder
            .setContentText(primes)
            .setContentTitle(limitText)
            .build()
        mNotificationManager.notify(NOTIF_ID, notification)
    }

    private fun updateView(primesCount: Int, primesList: List<Int>) {
        val intent = Intent(MainActivity.ACTION_UPDATE_PRIMES_COUNT)
        intent.putExtra(KEY_MESSAGE, primesCount)
        intent.putExtra(KEY_VALUES, primesList.toIntArray())


        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
            .sendBroadcast(intent)
    }

    companion object {
        const val ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE"
        const val ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE"

        const val NOTIF_ID = 2
        const val NOTIFICATION_CHANNEL_ID = "vova.com.primegenerator"
        const val CHANNEL_NAME = "My Background Service"


    }
}