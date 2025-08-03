// Arduino UNO R4 WiFi用 C++コード

#include "WiFiS3.h"
#include "arduino_secrets.h"

// --- Wi-Fiとサーバーの設定 ---
char ssid[] = SECRET_SSID;
char pass[] = SECRET_PASS;
int status = WL_IDLE_STATUS;
WiFiServer server(80);

WiFiSSLClient client; 
const char externalServer[] = "hackitserver-563679032017.asia-east1.run.app";


// --- センサー & LEDピン設定 ---
const int sensorPin = A0;  
const int redLedPin = 11;
const int blueLedPin = 10;

// --- 判定ロジック用変数 ---
const int threshold_fast = 70;
const int threshold_slow = 40;
const int threshold_deadzone = 5;
long lastChangeTime = 0;
const long detectionDuration = 500;
int currentValue = 0;
int previousValue = 0;

// --- 状態管理 & クールダウン用変数 ---
String currentStatus = "Idle";
bool isNadeNadeState = false;
bool isPonPonState = false;

// ★★★ 追加：クールダウン設定 (45秒) ★★★
const unsigned long cooldownPeriod = 45000; 
unsigned long lastRequestTime = 0;


// ★★★ 変更点：汎用的なPOSTリクエスト送信関数 ★★★
// 引数で受け取ったJSONデータを送信する
bool sendTriggerRequest(String jsonBody) {
  Serial.println("\nChecking cooldown...");
  // クールダウン中か確認
  if (millis() - lastRequestTime < cooldownPeriod) {
    Serial.print("Cooldown active. Please wait ");
    Serial.print((cooldownPeriod - (millis() - lastRequestTime)) / 1000);
    Serial.println(" seconds.");
    return false; // 送信失敗（クールダウン中）
  }

  Serial.println("Connecting for POST request...");
  if (client.connect(externalServer, 443)) {
    Serial.println("Connected to server");
    
    // HTTP POSTリクエストを作成
    client.println("POST /send_trigger HTTP/1.1"); // エンドポイントを/send_triggerに
    client.print("Host: ");
    client.println(externalServer);
    client.println("Content-Type: application/json");
    client.print("Content-Length: ");
    client.println(jsonBody.length());
    client.println("Connection: close");
    client.println();
    client.println(jsonBody); // 引数で受け取ったJSONを送信
    
    Serial.println("POST request sent with body: " + jsonBody);

    // サーバーからの応答を待つ（タイムアウト付き）
    unsigned long timeout = millis();
    while (client.available() == 0) {
      if (millis() - timeout > 5000) {
        Serial.println(">>> Client Timeout !");
        client.stop();
        return false;
      }
    }
    
    Serial.println("--- Server Response ---");
    while(client.available()){
      char c = client.read();
      Serial.write(c);
    }
    Serial.println("\n-----------------------");
    
    client.stop();
    lastRequestTime = millis(); // ★ 成功したので最終リクエスト時刻を更新
    return true; // 送信成功
  } else {
    Serial.println("Connection failed");
    return false; // 送信失敗
  }
}

// (handleWebServer関数は変更なし)
void handleWebServer() {
  WiFiClient client = server.available();
  if (client) {
    String currentLine = "";
    while (client.connected()) {
      if (client.available()) {
        char c = client.read();
        if (c == '\n') {
          if (currentLine.length() == 0) {
            client.println("HTTP/1.1 200 OK");
            client.println("Content-type:text/html");
            client.println("Refresh: 5");
            client.println();
            client.println("<!DOCTYPE html><html><head><title>Arduino Sensor Status</title>");
            client.println("<style>body { font-family: sans-serif; text-align: center; background-color: #282c34; color: white; font-size: 2em; margin-top: 100px;}</style></head>");
            client.println("<body><h1>Sensor Status</h1>");
            client.print("<h2>" + currentStatus + "</h2>");
            client.println("</body></html>");
            break;
          } else {
            currentLine = "";
          }
        } else if (c != '\r') {
          currentLine += c;
        }
      }
    }
    client.stop();
  }
}

// (setup関数は変更なし)
void setup() {
  Serial.begin(9600);
  while (!Serial);

  pinMode(redLedPin, OUTPUT);
  pinMode(blueLedPin, OUTPUT);

  if (WiFi.status() == WL_NO_MODULE) {
    Serial.println("Communication with WiFi module failed!");
    while (true);
  }

  Serial.print("Connecting to WiFi...");
  while (status != WL_CONNECTED) {
    status = WiFi.begin(ssid, pass);
    delay(5000);
    Serial.print(".");
  }
  
  Serial.println("\nWiFi connected!");
  server.begin();
  
  IPAddress ip = WiFi.localIP();
  Serial.print("Web Server is at IP address: ");
  Serial.println(ip);

  previousValue = analogRead(sensorPin);
  lastChangeTime = millis();
  // ★ 起動直後に送信しないように、クールダウンタイマーを初期化
  lastRequestTime = -cooldownPeriod; 
}


void loop() {
  handleWebServer();

  currentValue = analogRead(sensorPin);
  int valueChange = abs(currentValue - previousValue);

  // ポンポンの判定
  if (valueChange > threshold_fast) {
    digitalWrite(redLedPin, LOW);
    digitalWrite(blueLedPin, HIGH);
    if(currentStatus != "PonPon") Serial.println("PonPon");
    currentStatus = "PonPon";
    
    if (!isPonPonState) {
      // ★★★ ポンポン用のJSONでリクエストを送信 ★★★
      sendTriggerRequest("{\"type\": \"ponpon\"}");
    }
    
    isPonPonState = true;
    isNadeNadeState = false;
    lastChangeTime = millis();
  } 
  // なでなでの判定
  else if (valueChange > threshold_deadzone && valueChange <= threshold_slow && (millis() - lastChangeTime) > detectionDuration) {
    digitalWrite(redLedPin, HIGH);
    digitalWrite(blueLedPin, LOW);
    if(currentStatus != "NadeNade") Serial.println("NadeNade");
    currentStatus = "NadeNade";
    
    if (!isNadeNadeState) {
      // ★★★ なでなで用のJSONでリクエストを送信 ★★★
      sendTriggerRequest("{\"type\": \"nadenade\"}");
    }
    
    isNadeNadeState = true;
    isPonPonState = false;
  }
  // どちらでもない場合 (Idle)
  else {
    digitalWrite(redLedPin, LOW);
    digitalWrite(blueLedPin, LOW);
    currentStatus = "Idle";
    
    isNadeNadeState = false;
    isPonPonState = false;
  }

  previousValue = currentValue;
  delay(50);
}
