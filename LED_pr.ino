// C++ code
//
int feel = 0;

int level = 0;

int time = 0;

void setup()
{
  pinMode(11, OUTPUT);
  pinMode(10, OUTPUT);
  pinMode(9, OUTPUT);
}

void loop()
{
  feel = 113;
  level = 0;
  time = (2.1 - level / 10);
  if (feel == 113) {
    analogWrite(11, 225);
    analogWrite(10, 105);
    analogWrite(9, 180);
    delay(2000); // Wait for 2000 millisecond(s)
    analogWrite(11, 225);
    analogWrite(10, 69);
    analogWrite(9, 0);
    delay(2000); // Wait for 2000 millisecond(s)
  }
  if (feel == 115) {
    analogWrite(11, 128);
    analogWrite(10, 0);
    analogWrite(9, 0);
    delay(2000); // Wait for 2000 millisecond(s)
    analogWrite(11, 128);
    analogWrite(10, 128);
    analogWrite(9, 128);
    delay(2000); // Wait for 2000 millisecond(s)
  }
  if (feel == 118) {
    analogWrite(11, 0);
    analogWrite(10, 0);
    analogWrite(9, 225);
    delay(2000); // Wait for 2000 millisecond(s)
    analogWrite(11, 0);
    analogWrite(10, 191);
    analogWrite(9, 225);
    delay(2000); // Wait for 2000 millisecond(s)
  }
  if (feel == 120) {
    analogWrite(11, 225);
    analogWrite(10, 140);
    analogWrite(9, 0);
    delay(2000); // Wait for 2000 millisecond(s)
    analogWrite(11, 225);
    analogWrite(10, 225);
    analogWrite(9, 0);
    delay(2000); // Wait for 2000 millisecond(s)
  }
}
