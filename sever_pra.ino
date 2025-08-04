#include "WiFiS3.h"

// ▼▼▼ 1. Wi-Fi設定 ▼▼▼
const char ssid[] = "";
const char pass[] = "";
// ▲▲▲ ▲▲▲

// ▼▼▼ 2. アクセスしたいURLを設定 ▼▼▼
const char server[] = "hackitserver-563679032017.asia-east1.run.app";
const char filePath[] = "/";
// ▲▲▲ ▲▲▲

int status = WL_IDLE_STATUS;
WiFiSSLClient client;

void setup() {
  Serial.begin(9600);
  while (!Serial);

  if (WiFi.status() == WL_NO_MODULE) {
    Serial.println("Wi-Fiモジュールとの通信に失敗しました！");
    while (true);
  }

  Serial.print(ssid);
  Serial.println(" に接続中...");
  while (status != WL_CONNECTED) {
    status = WiFi.begin(ssid, pass);
    delay(5000);
  }
  Serial.println("Wi-Fi接続成功！");
  Serial.print("IPアドレス: ");
  Serial.println(WiFi.localIP());

  accessUrl(); 
}

void loop() {
  // 実行は一度だけ
}

void accessUrl() {
  Serial.println("\n--------------------");
  Serial.print(server);
  Serial.println(" に接続します...");

  if (client.connect(server, 443)) {
    Serial.println("サーバーに接続しました。");
    // HTTP GETリクエストを送信
    client.print("GET ");
    client.print(filePath);
    client.println(" HTTP/1.1");
    client.print("Host: ");
    client.println(server);
    client.println("Connection: close");
    client.println();
  } else {
    Serial.println("サーバーへの接続に失敗しました。");
    return;
  }

  // ▼▼▼ ここからが変更点 ▼▼▼
  bool headers_finished = false;
  String header_line_buffer = "";

  // 1. 本文を格納するための変数を準備
  String response_body = "";

  Serial.println("サーバーからの応答を受信中...");

  while (client.connected() || client.available()) {
    if (client.available()) {
      char c = client.read();

      if (headers_finished) {
        // 2. 本文なので、文字を変数に追加していく
        response_body += c;
      } else {
        header_line_buffer += c;
        if (c == '\n') {
          if (header_line_buffer == "\r\n") {
            headers_finished = true;
            Serial.println("ヘッダ終了。本文の格納を開始します。");
          }
          header_line_buffer = "";
        }
      }
    }
  }

  // 3. 格納が完了したら、変数の中身をシリアルモニタに表示
  Serial.println("--------------------");
  Serial.println("本文の格納が完了しました。変数の中身:");
  Serial.println(response_body);
  // ▲▲▲ ここまでが変更点 ▲▲▲

  client.stop();
  Serial.println("\n--------------------");
  Serial.println("処理を終了しました。");
}
