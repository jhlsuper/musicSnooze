package com.cando.music_snoozer

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.NumberPicker
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import com.example.music_snoozer.R
import com.google.android.gms.ads.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var state: State = State.BEFORE_PRESSING
    private var currentCountDownTimer: CountDownTimer? = null

    private var minToLong: Long = 0
    private var hourToLong: Long = 0
    private var realLong: Long = 0
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

        mAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                Log.d("ads", "ad loaded")
//                Toast.makeText(applicationContext, "ad Loaded", Toast.LENGTH_SHORT).show()
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                Log.d("ads", "$p0")

            }
        }

        btn_start.setOnClickListener(this)
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
        txt_remainSeconds.text = "%02d초".format(remainSeconds % 60)
        txt_remainHours.text = "%02d시간".format(remainSeconds / 3600)
        txt_remainMinutes.text = "%02d분".format((remainSeconds / 60) % 60)
    }

    override fun onClick(v: View?) {
        val hour: NumberPicker = findViewById(R.id.numberPicker_hour)
        val min: NumberPicker = findViewById(R.id.numberPicker_min)

        val timeSetView = findViewById<ConstraintLayout>(R.id.layout_setTime)
        val countDownView = findViewById<ConstraintLayout>(R.id.layout_showRemainTime)

        var builder = NotificationCompat.Builder(this, "MY_channel")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("미디어 종료 시각")
            .setContentText("oh..")
        val channelId = "MY_channel"
        val channelName = "음악멈춰!"
        val descriptionText = "descriptionText어쩌고쩌저고"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        when (v?.id) {
            R.id.btn_start -> {
                minToLong = (min.value * 1000 * 60).toLong()
                hourToLong = (hour.value * 60 * 1000 * 60).toLong()
                realLong = minToLong + hourToLong



                when (state) {
                    State.BEFORE_PRESSING -> {


                        currentCountDownTimer = createCountDownTimer(realLong)
                        btn_start.text = "중지"
                        timeSetView.visibility = View.INVISIBLE
                        countDownView.visibility = View.VISIBLE
                        state = State.ON_PRESSED
                        Log.d("realLong", "$realLong")
                        currentCountDownTimer?.start()

                        notificationManager.createNotificationChannel(channel)

                        notificationManager.notify(1000, builder.build())

                    }
                    State.ON_PRESSED -> {
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

