#include <WiFiS3.h>
#include <ArduinoMqttClient.h>
#include <ArduinoJson.h>
#include <Servo.h>




int degree = 0;

Servo servo_2;
Servo servo_4;

int counter;
int counter2;
int counter3;
int counter4;
int counter5;
int counter6;
int counter7;
int counter8;
int counter9;
int counter10;
int counter11;
int counter12;

 
// wifiのSSIDとパスワード
const char* SSID     = "";
const char* PASSWORD = "";

// サーバのURL
const char* BROKER = "broker.hivemq.com";
const int   PORT   = 1883;
 
const char* TOPIC_SUB = "emotion/broadcast";  // 受信用
const char* TOPIC_PUB = "kit/test";           // 送信用（←同じでも良い）
 
WiFiClient   wifi;
MqttClient   mqtt(wifi);
 
void setup() {
  Serial.begin(9600);
 
  // --- Wi-Fi ------------------------------------------------------------
  Serial.print("Connecting Wi-Fi");
  WiFi.begin(SSID, PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(500);
  }
  Serial.println("\n✅ Wi-Fi connected");
 
  // --- MQTT ------------------------------------------------------------
  Serial.print("Connecting MQTT");
  while (!mqtt.connect(BROKER, PORT)) {
    Serial.print(".");
    delay(1000);
  }
  Serial.println("\n✅ MQTT connected");
 
  // 購読開始
  mqtt.subscribe(TOPIC_SUB);

  servo_2.attach(2, 500, 2500);
  servo_4.attach(4, 500, 2500);
}
 
void loop() {
  // 受信処理
  int msgSize = mqtt.parseMessage();
  if (msgSize) {
    String response_body = "";
    while (mqtt.available()) {
      // mqtt.read()で読み込んだ文字(char)を文字列の末尾に追加する
      response_body += (char)mqtt.read();
    }
    // このループが終わると、response_bodyに受信した文字列全体が格納されている
    Serial.println(response_body);
  
    //JsonDocument doc;   
    // サーバーから取得したと仮定するJSON文字列
    String json_data = response_body;

    // JSONを解析するためのメモリ領域を準備
    // このJSONデータなら96バイトもあれば十分
    StaticJsonDocument<96> doc;
    
    // JSON文字列を解析する
    DeserializationError error = deserializeJson(doc, json_data);

    // 解析に失敗した場合の処理
    if (error) {
      Serial.print("JSONの解析に失敗しました: ");
      Serial.println(error.c_str());
      return;
    }

    // --- 辞書のようにキーを使って要素を取り出す ---

    // "emotion"というキーで文字列を取得
    String emotion = doc["emotion"]; // "happy" が取り出される

    // "level"というキーで数値を取得
    int level = doc["level"]; // 20 が取り出される

    // 結果を表示
    Serial.println(emotion);
    Serial.println(level);


    int times = 0;
    if (level <= 5) {
      times = (times + 2);
    }
    if (level > 5 && level <= 10) {
      times = (times + 3);
    }
    if (level > 10 && level <= 15) {
      times = (times + 4);
    }
    if (level > 15 && level <= 20) {
      times = (times + 5);
    }
    if (emotion == "happy") {
      for (counter3 = 0; counter3 < times; ++counter3) {
        degree = 0;
        for (counter = 0; counter < 90; ++counter) {
          servo_2.write((90 + degree));
          servo_4.write((90 + degree));
          degree = (degree + 1);
          delay(20); // Wait for 15 millisecond(s)
        }
        degree = 0;
        for (counter2 = 0; counter2 < 90; ++counter2) {
          servo_2.write((180 - degree));
          servo_4.write((180 - degree));
          degree = (degree + 1);
          delay(20); // Wait for 15 millisecond(s)
        }
      }
    }
    if (emotion == "bad") {
      for (counter6 = 0; counter6 < times; ++counter6) {
        degree = 0;
        for (counter4 = 0; counter4 < 90; ++counter4) {
          servo_2.write((90 + degree));
          servo_4.write((90 + degree));
          degree = (degree + 1);
          delay(25); // Wait for 25 millisecond(s)
        }
        degree = 0;
        for (counter5 = 0; counter5 < 90; ++counter5) {
          servo_2.write((180 - degree));
          servo_4.write((180 - degree));
          degree = (degree + 1);
          delay(25); // Wait for 25 millisecond(s)
        }
      }
    }
    if (emotion == "sad") {
      for (counter9 = 0; counter9 < times; ++counter9) {
        degree = 0;
        for (counter7 = 0; counter7 < 90; ++counter7) {
          servo_2.write((90 + degree));
          servo_4.write((90 + degree));
          degree = (degree + 1);
          delay(35); // Wait for 35 millisecond(s)
        }
        degree = 0;
        for (counter8 = 0; counter8 < 90; ++counter8) {
          servo_2.write((180 - degree));
          servo_4.write((180 - degree));
          degree = (degree + 1);
          delay(35); // Wait for 35 millisecond(s)
        }
      }
    }
    if (emotion == "fun") {
      for (counter12 = 0; counter12 < times; ++counter12) {
        degree = 0;
        for (counter10 = 0; counter10 < 90; ++counter10) {
          servo_2.write((90 + degree));
          servo_4.write((90 + degree));
          degree = (degree + 1);
          delay(20); // Wait for 10 millisecond(s)
        }
        degree = 0;
        for (counter11 = 0; counter11 < 90; ++counter11) {
          servo_2.write((180 - degree));
          servo_4.write((180 - degree));
          degree = (degree + 1);
          delay(20); // Wait for 10 millisecond(s)
        }
      }
    }
  }
}
