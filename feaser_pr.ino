// C++ code
//
#include <Servo.h>

int feel = 0;

int degree = 0;

int unnamed = 0;

int i = 0;

int j = 0;

int k = 0;

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

void setup()
{
  servo_2.attach(2, 500, 2500);
  servo_4.attach(4, 500, 2500);
}

void loop()
{
  feel = 120;
  degree = 0;
  servo_2.write(90);
  servo_4.write(90);
  delay(1000); // Wait for 1000 millisecond(s)
  if (feel == 113) {
    for (counter3 = 0; counter3 < 5; ++counter3) {
      degree = 0;
      for (counter = 0; counter < 90; ++counter) {
        servo_2.write((90 - degree));
        servo_4.write((90 + degree));
        degree = (degree + 1);
        delay(10); // Wait for 10 millisecond(s)
      }
      degree = 0;
      for (counter2 = 0; counter2 < 90; ++counter2) {
        servo_2.write((0 + degree));
        servo_4.write((180 - degree));
        degree = (degree + 1);
        delay(10); // Wait for 10 millisecond(s)
      }
    }
  }
  if (feel == 115) {
    for (counter6 = 0; counter6 < 1; ++counter6) {
      degree = 0;
      for (counter4 = 0; counter4 < 90; ++counter4) {
        servo_2.write((90 - degree));
        servo_4.write((90 + degree));
        degree = (degree + 1);
        delay(25); // Wait for 25 millisecond(s)
      }
      degree = 0;
      for (counter5 = 0; counter5 < 90; ++counter5) {
        servo_2.write((0 + degree));
        servo_4.write((180 - degree));
        degree = (degree + 1);
        delay(25); // Wait for 25 millisecond(s)
      }
    }
  }
  if (feel == 117) {
    for (counter9 = 0; counter9 < 3; ++counter9) {
      degree = 0;
      for (counter7 = 0; counter7 < 90; ++counter7) {
        servo_2.write((90 - degree));
        servo_4.write((90 + degree));
        degree = (degree + 1);
        delay(35); // Wait for 35 millisecond(s)
      }
      degree = 0;
      for (counter8 = 0; counter8 < 90; ++counter8) {
        servo_2.write((0 + degree));
        servo_4.write((180 - degree));
        degree = (degree + 1);
        delay(35); // Wait for 35 millisecond(s)
      }
    }
  }
  if (feel == 120) {
    for (counter12 = 0; counter12 < 10; ++counter12) {
      degree = 0;
      for (counter10 = 0; counter10 < 90; ++counter10) {
        servo_2.write((90 - degree));
        servo_4.write((90 + degree));
        degree = (degree + 1);
        delay(5); // Wait for 5 millisecond(s)
      }
      degree = 0;
      for (counter11 = 0; counter11 < 90; ++counter11) {
        servo_2.write((0 + degree));
        servo_4.write((180 - degree));
        degree = (degree + 1);
        delay(5); // Wait for 5 millisecond(s)
      }
    }
  }
}