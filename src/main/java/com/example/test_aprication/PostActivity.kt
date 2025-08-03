package com.example.test_aprication

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.test_aprication.model.Post
import com.example.test_aprication.model.Emotion
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.IOException
import java.util.*

class PostActivity : AppCompatActivity() {

    // UI要素
    private lateinit var viewBox: View
    private lateinit var buttonPost: Button
    private lateinit var buttonSee: Button
    private lateinit var editPost: EditText
    private lateinit var statusTextView: TextView

    // MQTTクライアント
    private lateinit var mqttClient: MqttClient

    // HTTPクライアント
    private val okHttpClient = OkHttpClient()

    // MQTT設定
    private val BROKER_URL = "tcp://broker.hivemq.com:1883"
    private val TOPIC = "emotion/broadcast"
    private val CLIENT_ID = MqttClient.generateClientId()

    // EmotionデータとPostデータ
    private var nowEmotion = Emotion(emotion = "default", level = 0)
    private var newPost = Post(user = "", text = "")

    // リアクションデータを受信するまで待機するための変数
    private var emotionDeferred: CompletableDeferred<Emotion>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_post)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.post)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // UI要素の初期化
        viewBox = findViewById(R.id.view_box)
        buttonPost = findViewById(R.id.button_post)
        buttonSee = findViewById(R.id.button_see)
        editPost = findViewById(R.id.edit_post)
        statusTextView = findViewById(R.id.statusTextView)

        // MQTTクライアントの初期化
        initMqttClient()

        // 投稿ボタンのクリックリスナーを設定
        buttonPost.setOnClickListener {
            if (mqttClient.isConnected) {
                buttonPost.isEnabled = false
                editPost.isEnabled = false

                val postText = editPost.text.toString()
                newPost.text = postText
                val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                newPost.user = prefs.getString("user_id", "") ?: ""

                // MQTTで投稿を送信する代わりに、HTTP POSTリクエストを送信
                sendPostToHttpServer(newPost)
                editPost.setText("")

                CoroutineScope(Dispatchers.Main).launch {
                    Log.d("MQTT", "リアクション受信を待機中...")
                    val receivedEmotion = awaitEmotion()

                    if (receivedEmotion != null) {
                        nowEmotion = receivedEmotion
                        updateUiColors()
                        Log.d("MQTT", "📩 Topic: $TOPIC")
                        Log.d("MQTT", "Payload: $receivedEmotion")
                        Toast.makeText(this@PostActivity, "リアクションを受信しました", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("MQTT", "リアクションの受信に失敗しました")
                        Toast.makeText(this@PostActivity, "リアクションの受信に失敗しました", Toast.LENGTH_SHORT).show()
                    }
                    buttonPost.isEnabled = true
                    editPost.isEnabled = true
                }
            } else {
                Toast.makeText(this, "MQTTブローカーに接続していません", Toast.LENGTH_SHORT).show()
            }
        }

        buttonSee.setOnClickListener {
            val intent = Intent(this, FeedActivity::class.java)
            startActivity(intent)
        }

        connectMqtt()
        updateUiColors()
    }

    private fun initMqttClient() {
        try {
            mqttClient = MqttClient(BROKER_URL, CLIENT_ID, MemoryPersistence())
            mqttClient.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e("MQTT", "接続切断: $cause")
                    runOnUiThread {
                        statusTextView.text = "接続状態: 未接続"
                        Toast.makeText(this@PostActivity, "接続が切断されました", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = String(message?.payload ?: ByteArray(0))
                    Log.d("MQTT", "📩 Topic: $topic")
                    Log.d("MQTT", "Payload: $payload")
                    runOnUiThread {
                        if (topic == TOPIC) {
                            val receivedEmotion = Gson().fromJson(payload, Emotion::class.java)
                            if (receivedEmotion != null) {
                                emotionDeferred?.complete(receivedEmotion)
                                nowEmotion = receivedEmotion
                                updateUiColors()
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
            statusTextView.text = "接続状態: 接続中..."
            mqttClient.connect(options)
            mqttClient.subscribe(TOPIC, 1)
            Log.d("MQTT", "接続成功とトピック購読: $TOPIC")
            runOnUiThread {
                statusTextView.text = "接続状態: 接続済み"
                Toast.makeText(this, "接続しました。リアクションを待機中...", Toast.LENGTH_SHORT).show()
            }
        } catch (e: MqttException) {
            Log.e("MQTT", "接続失敗: ${e.message}")
            runOnUiThread {
                statusTextView.text = "接続状態: 接続失敗"
                Toast.makeText(this, "接続失敗: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun awaitEmotion(): Emotion? {
        emotionDeferred = CompletableDeferred()
        val result = emotionDeferred?.await()
        emotionDeferred = null
        return result
    }

    // 新しい関数: HTTP POSTリクエストを送信
    private fun sendPostToHttpServer(postData: Post) {
        val jsonPayload = Gson().toJson(postData)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonPayload.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://hackitserver-563679032017.asia-east1.run.app/send_chat")
            .post(body)
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HTTP", "HTTPリクエスト失敗: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@PostActivity, "投稿送信失敗: HTTPリクエストエラー", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("HTTP", "HTTPリクエスト成功: ${response.code}")
                    // HTTPリクエストが成功したことをToastで通知
                    runOnUiThread {
                        Toast.makeText(this@PostActivity, "投稿がサーバーに送信されました", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("HTTP", "HTTPリクエスト失敗: ${response.code} ${response.message}")
                    runOnUiThread {
                        Toast.makeText(this@PostActivity, "投稿送信失敗: HTTPエラー", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // 元の publishPost 関数は今回は使用しません
    /*
    private fun publishPost(postData: Post) {
        // ... MQTTでのパブリッシュ処理
    }
    */

    private fun updateUiColors() {
        val happyColor = ContextCompat.getColor(this, R.color.happy)
        val sadColor = ContextCompat.getColor(this, R.color.sad)
        val funColor = ContextCompat.getColor(this, R.color.enjoy)
        val badColor = ContextCompat.getColor(this, R.color.bad)

        fun levelToAlpha(level: Int): Int {
            return ((level / 20.0) * 205 + 50).toInt()
        }

        val emotionString = nowEmotion.emotion?.lowercase() ?: ""

        val color = when (emotionString) {
            "happy" -> Color.argb(levelToAlpha(nowEmotion.level), Color.red(happyColor), Color.green(happyColor), Color.blue(happyColor))
            "sad"   -> Color.argb(levelToAlpha(nowEmotion.level), Color.red(sadColor), Color.green(sadColor), Color.blue(sadColor))
            "fun"   -> Color.argb(levelToAlpha(nowEmotion.level), Color.red(funColor), Color.green(funColor), Color.blue(funColor))
            "bad"   -> Color.argb(levelToAlpha(nowEmotion.level), Color.red(badColor), Color.green(badColor), Color.blue(badColor))
            else    -> Color.GRAY
        }

        viewBox.setBackgroundColor(color)
        buttonPost.setBackgroundColor(color)
        buttonSee.setBackgroundColor(color)
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
