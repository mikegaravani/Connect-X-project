# SouthPasadena, a Connect X AI

Connect X is a version of the popular board game Connect 4 where the board dimensions are arbitrary.
SouthPasadena is a software player for Connect X , designed to select the best move on the board. For more information check the report file (written in italian).

## Play a game of Connect X against SouthPasadena!

1. Make sure to have the Java Development Kit (JDK) installed on your computer.
2. Once you're in the Connect-X-project directory, run:
	```
	javac -cp ".." *.java */*.java
	```
3. And finally, to launch the game, run:
	```
	java -cp ".." connectx.CXGame M N X connectx.SouthPasadena.SouthPasadena
	```
	Make sure to set the parameters M N X to the dimensions of the board you want to play with. M is the number of rows, N is the number of columns and X is the tokens to connect to win. If you want to play a classic Connect 4 game, set the parameters to 6, 7 and 4.