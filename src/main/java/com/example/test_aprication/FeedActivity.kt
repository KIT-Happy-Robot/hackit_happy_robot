package com.example.test_aprication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.test_aprication.model.Emotion
import com.example.test_aprication.model.Post
import com.google.gson.Gson
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class FeedActivity : AppCompatActivity() {

    private lateinit var viewBox: View
    private lateinit var viewPost: TextView
    private lateinit var buttonWrite: Button
    private lateinit var buttonHappy: Button
    private lateinit var buttonSad: Button
    private lateinit var buttonFun: Button
    private lateinit var buttonBad: Button
    private lateinit var mqttClient: MqttClient
    private val BROKER_URL = "tcp://broker.hivemq.com:1883"
    private val POST_TOPIC = "chat/broadcast"
    private val EMOTION_TOPIC = "emotion/broadcast"
    private val CLIENT_ID = MqttClient.generateClientId()
    private var nowEmotion = Emotion(emotion = "default", level = 0)
    private var currentPostText: String? = null
    private var hasReceivedFirstPost = false
    private var isWaitingReaction = false
    private var pendingPost: Post? = null
    private var pendingEmotion: Emotion? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_feed)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.feed)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewBox = findViewById(R.id.view_box)
        viewPost = findViewById(R.id.view_post)
        buttonWrite = findViewById(R.id.button_write)
        buttonHappy = findViewById(R.id.button_happy_top_left)
        buttonSad = findViewById(R.id.button_sad_bottom_left)
        buttonFun = findViewById(R.id.button_fun_top_right)
        buttonBad = findViewById(R.id.button_bad_bottom_right)

        initMqttClient()
        connectMqtt()

        buttonWrite.setOnClickListener {
            val intent = Intent(this, PostActivity::class.java)
            startActivity(intent)
        }

        buttonHappy.setOnClickListener {
            publishReaction("happy", 10)
        }
        buttonSad.setOnClickListener {
            publishReaction("sad", 10)
        }
        buttonFun.setOnClickListener {
            publishReaction("fun", 10)
        }
        buttonBad.setOnClickListener {
            publishReaction("bad", 10)
        }

        viewPost.text = "新しい投稿を待機中..."
        updateUiColors("default", 0)
    }

    private fun initMqttClient() {
        try {
            mqttClient = MqttClient(BROKER_URL, CLIENT_ID, MemoryPersistence())
            mqttClient.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e("MQTT", "接続切断: $cause")
                    runOnUiThread {
                        Toast.makeText(this@FeedActivity, "接続が切断されました", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = String(message?.payload ?: ByteArray(0))
                    Log.d("MQTT", "📩 Topic: $topic")
                    Log.d("MQTT", "Payload: $payload")
                    runOnUiThread {
                        when (topic) {
                            POST_TOPIC -> {
                                val receivedPost = Gson().fromJson(payload, Post::class.java)
                                if (receivedPost != null) {
                                    if (!hasReceivedFirstPost) {
                                        pendingPost = receivedPost
                                        checkAndApplyFirstPost()
                                    } else if (isWaitingReaction) {
                                        pendingPost = receivedPost
                                        Log.d("MQTT", "保留中")
                                        applyPendingUpdate()
                                    } else {
                                        Log.d("MQTT", "リアクション待ち")
                                    }
                                }
                            }

                            EMOTION_TOPIC -> {
                                val receivedEmotion = Gson().fromJson(payload, Emotion::class.java)
                                if (receivedEmotion != null && receivedEmotion.senderId != CLIENT_ID) {
                                    if (!hasReceivedFirstPost) {
                                        pendingEmotion = receivedEmotion
                                        checkAndApplyFirstPost()
                                    } else if (isWaitingReaction) {
                                        pendingEmotion = receivedEmotion
                                        Log.d("MQTT", "保留中")
                                        applyPendingUpdate()
                                    } else {
                                        Log.d("MQTT", "リアクション待ち")
                                    }
                                    Toast.makeText(this@FeedActivity, "リアクションを受信しました: ${receivedEmotion.emotion}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("MQTT", "配信完了")
                }
            })
        } catch (e: MqttException) {
            Log.e("MQTT", "クライアント初期化エラー: ${e.message}")
            Toast.makeText(this, "MQTTクライアント初期化エラー", Toast.LENGTH_LONG).show()
        }
    }

    private fun connectMqtt() {
        val options = MqttConnectOptions()
        options.isCleanSession = true
        try {
            mqttClient.connect(options)
            mqttClient.subscribe(POST_TOPIC, 1)
            mqttClient.subscribe(EMOTION_TOPIC, 1)
            Log.d("MQTT", "接続成功とトピック購読: $POST_TOPIC と $EMOTION_TOPIC")
            runOnUiThread {
                Toast.makeText(this, "接続しました。投稿を待機中...", Toast.LENGTH_SHORT).show()
            }
        } catch (e: MqttException) {
            Log.e("MQTT", "接続失敗: ${e.message}")
            runOnUiThread {
                Toast.makeText(this, "接続失敗: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun publishReaction(emotion: String, level: Int) {
        isWaitingReaction = true
        viewPost.text = "リアクションを送信しました。新しい投稿を待機中..."
        updateUiColors(emotion, level)

        val reaction = Emotion(emotion = emotion, level = level, senderId = CLIENT_ID)
        val jsonPayload = Gson().toJson(reaction)
        val mqttMessage = MqttMessage(jsonPayload.toByteArray(Charsets.UTF_8))
        mqttMessage.qos = 1
        mqttMessage.isRetained = false

        try {
            mqttClient.publish(EMOTION_TOPIC, mqttMessage)
            Log.d("MQTT", "リアクション送信: $jsonPayload")
            Toast.makeText(this, "リアクションが送信されました", Toast.LENGTH_SHORT).show()
        } catch (e: MqttException) {
            Log.e("MQTT", "リアクション送信失敗: ${e.message}")
            Toast.makeText(this, "リアクション送信失敗: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndApplyFirstPost() {
        if (pendingPost != null && pendingEmotion != null && !hasReceivedFirstPost) {
            currentPostText = pendingPost!!.text
            viewPost.text = currentPostText
            nowEmotion = pendingEmotion!!
            updateUiColors(nowEmotion.emotion, nowEmotion.level)

            hasReceivedFirstPost = true
            pendingPost = null
            pendingEmotion = null

            Log.d("MQTT", "🎉 初回投稿と感情を反映")
        }
    }

    private fun applyPendingUpdate() {
        if (pendingPost != null && pendingEmotion != null) {
            currentPostText = pendingPost!!.text
            viewPost.text = currentPostText
            nowEmotion = pendingEmotion!!
            updateUiColors(nowEmotion.emotion, nowEmotion.level)

            pendingPost = null
            pendingEmotion = null
            isWaitingReaction = false
            Log.d("MQTT", "🆕 保留中の投稿と感情を反映")
        }
    }

    private fun updateUiColors(emotion: String, level: Int) {
        val happyColor = ContextCompat.getColor(this, R.color.happy)
        val sadColor = ContextCompat.getColor(this, R.color.sad)
        val funColor = ContextCompat.getColor(this, R.color.enjoy)
        val badColor = ContextCompat.getColor(this, R.color.bad)

        fun levelToAlpha(level: Int): Int {
            return ((level / 20.0) * 205 + 50).toInt()
        }

        val color = when (emotion.lowercase()) {
            "happy" -> Color.argb(levelToAlpha(level), Color.red(happyColor), Color.green(happyColor), Color.blue(happyColor))
            "sad" -> Color.argb(levelToAlpha(level), Color.red(sadColor), Color.green(sadColor), Color.blue(sadColor))
            "fun" -> Color.argb(levelToAlpha(level), Color.red(funColor), Color.green(funColor), Color.blue(funColor))
            "bad" -> Color.argb(levelToAlpha(level), Color.red(badColor), Color.green(badColor), Color.blue(badColor))
            else -> Color.GRAY
        }

        val viewHeader = findViewById<View>(R.id.view_header)
        val viewFooter = findViewById<View>(R.id.view_footer)

        viewBox.setBackgroundColor(color)
        buttonWrite.setBackgroundColor(color)
        viewHeader.setBackgroundColor(color)
        viewFooter.setBackgroundColor(color)

    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mqttClient.isInitialized && mqttClient.isConnected) {
            try {
                mqttClient.disconnect()
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }
}