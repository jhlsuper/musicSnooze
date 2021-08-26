package com.cando.music_snoozer

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.NumberPicker
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.music_snoozer.R
import com.google.android.gms.ads.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.Date.from
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var state: State = State.BEFORE_PRESSING
    private var currentCountDownTimer: CountDownTimer? = null
    private var notificationState: Boolean = false
    private var minToLong: Long = 0
    private var hourToLong: Long = 0
    private var realLong: Long = 0
    private val notificationId = 1
    private lateinit var mAdView: AdView


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //광고 초기화
        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        val hour: NumberPicker = findViewById(R.id.numberPicker_hour)
        val min: NumberPicker = findViewById(R.id.numberPicker_min)

        hour.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        min.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS

        hour.minValue = 0
        min.minValue = 0

        hour.maxValue = 6
        min.maxValue = 59
        Thread(Runnable {
            mAdView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Log.d("ads", "ad loaded")

                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    Log.d("ads", "$p0")

                }
            }
        }).start()

        btn_start.setOnClickListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelNotification()
    }


    private fun stopMusic() {


        val mAudioManager: AudioManager =
            applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mAudioManager.requestAudioFocus(
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build()
            ).setAcceptsDelayedFocusGain(true).setOnAudioFocusChangeListener { }.build()
        )
    }

//    private fun stopInSec(sec: Long) {
//        //5000 = 5sec
//        createCountDownTimer(sec)
//        Timer().schedule(sec) {
//            stopMusic()
//            state = State.BEFORE_PRESSING
////            finish()
//        }
//    }

    private fun createCountDownTimer(initialMillis: Long): CountDownTimer =
        object : CountDownTimer(initialMillis, 1000L) {
            override fun onTick(initialMillis: Long) {
                updateRemainTime(initialMillis)
                Log.d("realLong", "$initialMillis")

            }

            override fun onFinish() {
                val timeSetView = findViewById<ConstraintLayout>(R.id.layout_setTime)
                val countDownView = findViewById<ConstraintLayout>(R.id.layout_showRemainTime)
                state = State.BEFORE_PRESSING
                stopMusic()
                timeSetView.visibility = View.VISIBLE
                countDownView.visibility = View.INVISIBLE
//                Toast.makeText(applicationContext, "음악을 성공적으로 멈추었습니다", Toast.LENGTH_SHORT).show()
                updateRemainTime(0)
                btn_start.text = "시작하기"

            }
        }

    @SuppressLint("SetTextI18n")
    private fun updateRemainTime(remainMills: Long) {
        val remainSeconds = remainMills / 1000
        thread(start = true) {
            runOnUiThread {
                txt_remainSeconds.text = "%02d초".format(remainSeconds % 60)
                txt_remainHours.text = "%02d시간".format(remainSeconds / 3600)
                txt_remainMinutes.text = "%02d분".format((remainSeconds / 60) % 60)
            }
            Thread.sleep(remainMills)
        }
    }

    private fun createNotificationChannel(channelId: String) {
        val name = "음악멈춰"
        val channelDescription = "description"
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        val channel = NotificationChannel(channelId, name, importance)
        channel.apply {
            description = channelDescription

        }

        // Finally register the channel with system
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }


    private fun showNotification(initialMillis: Long) {

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        createNotificationChannel("stop")
        var builder = NotificationCompat.Builder(this, "stop").apply {
            setSmallIcon(R.drawable.ic_launcher_background)
            setContentTitle("미디어 종료 시각")
            setContentText("oh..")
            setContentIntent(pendingIntent) //어플로 복귀 가아니라 액티비티가 시작됨
            setPriority(NotificationCompat.PRIORITY_LOW)
        }
        val max = initialMillis.toInt()
        Log.d("progress", "$max")
        var progress = 0
        var percentage = 0
        val handler = Handler()

        with(NotificationManagerCompat.from(this)) {
            if(notificationState){
            builder.setProgress(max, progress, false)
            notify(notificationId, builder.build())
            }
            Thread(Runnable {
                while (progress < max) {
                    progress += 1000
                    try {
                        //1초마다 갱신
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    handler.post(Runnable {
                        if (progress == max) {
                            builder.setContentText("끝")
                            builder.setProgress(0, 0, false)
                        } else {
                            percentage = (progress * 100) / max
                            builder.setContentText("${(max - progress) / 1000 / 60} 분 후 종료")
                            builder.setProgress(max, progress, false)
                            Log.d("progress", "$progress")
                        }
                        notify(notificationId, builder.build())
                        cancel(notificationId)
                    })
                }
            }).start()
        }

    }

    private fun cancelNotification() {

        notificationState = false
        NotificationManagerCompat.from(this).apply {
            cancelAll()

        }
    }


    override fun onClick(v: View?) {
        val hour: NumberPicker = findViewById(R.id.numberPicker_hour)
        val min: NumberPicker = findViewById(R.id.numberPicker_min)

        val timeSetView = findViewById<ConstraintLayout>(R.id.layout_setTime)
        val countDownView = findViewById<ConstraintLayout>(R.id.layout_showRemainTime)



        when (v?.id) {
            R.id.btn_start -> {
                minToLong = (min.value * 1000 * 60).toLong()
                hourToLong = (hour.value * 60 * 1000 * 60).toLong()
                realLong = minToLong + hourToLong



                when (state) {
                    State.BEFORE_PRESSING -> {

                        notificationState = true
                        currentCountDownTimer = createCountDownTimer(realLong)
                        btn_start.text = "중지"
                        timeSetView.visibility = View.INVISIBLE
                        countDownView.visibility = View.VISIBLE
                        state = State.ON_PRESSED
                        Log.d("realLong", "$realLong")
                        currentCountDownTimer?.start()
                        showNotification(realLong)

                    }
                    State.ON_PRESSED -> {
                        cancelNotification()
                        currentCountDownTimer?.cancel()
                        btn_start.text = "시작하기"
                        state = State.BEFORE_PRESSING
                        timeSetView.visibility = View.VISIBLE
                        countDownView.visibility = View.INVISIBLE

                    }


                }
//            Toast.makeText(this, "${hour.value}시간 ${min.value}분", Toast.LENGTH_SHORT).show()

            }
        }
    }
}

