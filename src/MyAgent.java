import com.sun.istack.internal.NotNull;
import javafx.util.Pair;

import java.io.*;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

//region Classes

class Snake{
    public ArrayList<coord> body;
    public int pos;
    public boolean isAlive;
    public int length;
    public int kills;

    Snake(int pos, boolean isAlive, int length, int kills){
        body = new ArrayList<coord>();
        this.pos = pos;
        this.isAlive = isAlive;
        this.length = length;
        this.kills = kills;
    }

    Snake(ArrayList<coord> c,int pos, boolean isAlive, int length, int kills) {
        body = c;
        this.pos = pos;
        this.isAlive = isAlive;
        this.length = length;
        this.kills = kills;
    }

    public coord head() {
        return body.get(0);
    }

    public coord tail() {
        return body.get(length - 1);
    }

    public void append(coord c) {
        body.add(c);
    }

    public void append(coord[] c) {
        int size = c.length;

        body.addAll(Arrays.asList(c).subList(0, size));
    }

}

class coord {
    public int row, col;
    coord parent;

    coord(int row, int col){
        this.row = row;
        this.col = col;
        this.parent = null;
    }

    coord (int row, int col, coord parent){
        this.row = row;
        this.col = col;
        this.parent = parent;
    }

}

class movePyth{
    int move;
    int pythag;

    movePyth(int move, int pythag){
        this.move = move;
        this.pythag = pythag;
    }

    public boolean amSmaller(movePyth mp){
        return this.pythag < mp.pythag;
    }

}

//endregion

public class MyAgent extends za.ac.wits.snake.DevelopmentAgent {

    //region Constants
    final static int obstacleNumb = 9;
    final static int nearSnakeHead = 8;
    final static int appleNumb = 7;

    static final float badApple = 5f;
    static final float decayRate = 0.1f;
    static int amountOfIterations = 756;

    static final int north = 0;
    static final int south = 1;
    static final int west = 2;
    static final int east = 3;
    static final int relLeft = 4;
    static final int straight = 5;
    static final int relRight = 6;

    final static int offZone = 7500;
    final static int noGoInt = 5000;
    final static int nearSnakeIncrease = 2500;
    final static int nearByIncreaseLarge = 500;
    final static int nearByIncreaseSmall = 250;
    //endregion

    //region Initialization
    static int numbSnakes;
    static int width;
    static int height;
    static Snake[] snakeArr;
    static int[][] map;
    static int[][] disMap;
    static int[][] startMap;
    static int timeStep;
    static int appleTime;
    static coord apple;
    static int mySnake;
    static ArrayList<coord> obstacles;
    static int move;
    static boolean isAppleHealthy;

    static int iteration;

    //endregion

    public static void main(String[] args) throws IOException{
        MyAgent agent = new MyAgent();
        MyAgent.start(agent, args);
    }

    @Override
    public void run() {
        try(BufferedReader br = new BufferedReader(new InputStreamReader(System.in))){
            //region Set Starter Values
            String initString = br.readLine();
            String[] temp = initString.split(" ");
            numbSnakes = Integer.parseInt(temp[0]);
            width = Integer.parseInt(temp[1]);
            height = Integer.parseInt(temp[2]);
            snakeArr = new Snake[numbSnakes];
            obstacles = new ArrayList<>();
            //endregion

            //region Initializing Stage
            timeStep = 0;
            appleTime = 0;
            isAppleHealthy = true;
            //endregion

            while (true) {
                //region Game Over
                String line = br.readLine();
                if (line.contains("Game Over")) {
                    break;
                }
                //endregion

                //region Get Apple
                try {
                    //do stuff with apples
                    String[] appleArr = line.split(" ");
                    if (timeStep == 0){
                        apple = new coord(Integer.parseInt(appleArr[1]), Integer.parseInt(appleArr[0]));
                    }
                    else{
                        coord tempCoord = new coord(Integer.parseInt(appleArr[1]), Integer.parseInt(appleArr[0]));
                        if (!equalCoords(tempCoord,apple)){
                            appleTime = 0;
                        }
                        apple = tempCoord;
                    }

                }
                catch (Exception e){
                    log(e.toString());
                    log("Problem with getting the apple");
                }

                //log("time is " + (time - System.currentTimeMillis()));
                //endregion

                //region Get Obstacles
                int nObstacles = 3;
                for (int obstacle = 0; obstacle < nObstacles; obstacle++) {
                    String obsLine = br.readLine();

                    String[] arr = obsLine.split(" ");

                    int size = arr.length;

                    for (int j = 0; j < size; j++) {
                        obstacles.add(getCoordComma(arr[j]));
                    }
                }
                //endregion

                //region Get Snakes
                mySnake = Integer.parseInt(br.readLine());
                for (int i = 0; i < numbSnakes; i++) {
                    String snakeLine = br.readLine();

                    snakeArr[i] = readSnake(snakeLine, i);

                }

                //Calculate move

                if (!snakeArr[mySnake].isAlive) {
                    log("died");
                    continue;
                }
                //endregion

                float timer = System.nanoTime();

                //region Actual Logic

                move = straight;

                setMap();

                if (timeStep == 0){
                    goToApple();
                }

                try{
                    if (isAppleViable()){
                        if (!goToApple()){
                            move = straight;
                        }
                    }

                    if (move == straight){
                        survive(false);
                    }


                }catch (Exception e){
                    log(e.toString());
                    move = straight;
                }

                try{
                    if (move == straight){
                        survive(true);
                    }
                }catch (Exception e){
                    log(e.toString());
                }

                try{
                    coord head = snakeArr[mySnake].head();
                    if (!isValidMove(head,move, map,disMap)){
                        lowestMove(head);
                    }
                }catch (Exception ignore){
                    move = straight;
                }

                //endregion

                timeStep += 1;
//                log("move is " + move);
                appleTime += 1;
//                log("timeStep is " + timeStep +" ,move " + move + " ,time " + Float.toString((System.nanoTime() - timer)/1000000));
                System.out.println(move);
            }
        }
        catch (IOException e){
            log("The pipe closing" + e.toString());
        }
    }

    //region goToCoord

    public boolean goToCoord(coord toGo, boolean force, boolean longMode){
        if (goToCoord(toGo, snakeArr[mySnake].head(), force, longMode)){
            return true;
        }
        move = straight;
        return false;
    }

    public boolean goToCoord(coord toGo){
        return goToCoord(toGo, false);
    }

    public boolean goToCoord(coord toGo, boolean force){
        return goToCoord(toGo,force,false);
    }


    public boolean goToCoord(coord toGo, coord startCoord, boolean force, boolean longMode){
        int iteration = 0;
        iteration++;

        //region Intializationf
        int thres;
        if (force){
            thres =  (int)(noGoInt * 0.8);
        }
        else{
            thres = (int)(nearSnakeIncrease * 0.5);
        }
        //endregion

        ArrayList<coord> toVisit = new ArrayList<>();
        ArrayList<coord> visited = new ArrayList<>();

        disMap = new int[width][height];

        //region Setting up Dismap
        for (int row = 0; row < height; row++){
            for (int col = 0; col < width; col++){
                disMap[col][row] += truePythag(toGo,new coord(row, col));

                if (col == 0 || row == 0 || col == width-1 || row == height -1){
                    if (map[col][row] != appleNumb) {
                        disMap[col][row] += 100;
                    }
                    else{
                        disMap[col][row] -= 5000;
                    }
                }

                //NearSnakeHead
                if (map[col][row] == nearSnakeHead){
                    disMap[col][row] += nearSnakeIncrease;

                    try{
                        if (map[col+1][row] == 0) disMap[col+1][row] += 125;
                    }catch (Exception ignore){}

                    try{
                        if (map[col-1][row] == 0) disMap[col-1][row] += 125;
                    }catch (Exception ignore){}

                    try{
                        if (map[col][row+1] == 0) disMap[col][row+1] += 125;
                    }catch (Exception ignore){}

                    try{
                        if (map[col][row-1] == 0) disMap[col][row-1] += 125;
                    }catch (Exception ignore){}

                    try{
                        if (map[col+1][row+1] == 0) disMap[col+1][row+1] += 50;
                    }catch (Exception ignore){}

                    try{
                        if (map[col-1][row+1] == 0) disMap[col-1][row+1] += 50;
                    }catch (Exception ignore){}

                    try{
                        if (map[col-1][row-1] == 0) disMap[col-1][row-1] += 50;
                    }catch (Exception ignore){}

                    try{
                        if (map[col+1][row-1] == 0) disMap[col+1][row-1] += 50;
                    }catch (Exception ignore){}

                    continue;
                }

                //Add values near enemy snakes bodies
                if (map[col][row] == 1 || map[col][row] == 2 || map[col][row] == 3 || map[col][row] == 4 || map[col][row] == obstacleNumb){
                    disMap[col][row] = noGoInt;


                    if (map[col][row] != mySnake+1){
                        try{
                            disMap[col+1][row] += 30;
                        }catch (Exception ignore){}

                        try{
                            disMap[col-1][row] += 30;
                        }catch (Exception ignore){}

                        try{
                            disMap[col][row+1] += 30;
                        }catch (Exception ignore){}

                        try{
                            disMap[col][row-1] += 30;
                        }catch (Exception ignore){}
                    }

                }

                //If apple skip
                if (map[col][row] == appleNumb){
                    int iCount = 0;

                    try{
                        if (disMap[col+1][row] > 100) iCount++;
                    }catch (Exception e){
                        iCount++;
                    }

                    try{
                        if (disMap[col-1][row] > 100) iCount++;
                    }catch (Exception e){
                        iCount++;
                    }

                    try{
                        if (disMap[col][row+1] > 100) iCount++;
                    }catch (Exception e){
                        iCount++;
                    }
                    try{
                        if (disMap[col][row-1] > 100) iCount++;
                    }catch (Exception e){
                        iCount++;
                    }

                    if (iCount > 1) disMap[col][row] += noGoInt;
                };

                //If smashed noGoInt
                try{
                    if ((disMap[col+1][row] > noGoInt && disMap[col-1][row] > noGoInt) ||
                            (disMap[col][row+1] > noGoInt && disMap[col][row-1] > noGoInt)){
                        disMap[col][row] += nearSnakeIncrease/4;
                    }
                }catch (Exception ignore){}


            }
        }
        //endregion

        if (!isAppleHealthy){
            disMap[apple.col][apple.row] = noGoInt;
        }

        //region Sort out values near Target

        {
            int toCol = toGo.col;
            int toRow = toGo.row;

            try{
                if (map[toCol+1][toRow] == nearSnakeHead){
                    disMap[toCol][toRow] = noGoInt;
                }
            }catch (Exception ignored){}

            try{
                if (map[toCol-1][toRow] == nearSnakeHead){
                    disMap[toCol][toRow] = noGoInt;
                }
            }catch (Exception ignored){}

            try{
                if (map[toCol][toRow+1] == nearSnakeHead){
                    disMap[toCol][toRow] = noGoInt;
                }
            }catch (Exception ignored){}

            try{
                if (map[toCol][toRow-1] == nearSnakeHead){
                    disMap[toCol][toRow] = noGoInt;
                }
            }catch (Exception ignored){}

            try{
                if (map[toCol+1][toRow+1] == nearSnakeHead){
                    disMap[toCol][toRow] = noGoInt;
                }
            }catch (Exception ignored){}

            try{
                if (map[toCol-1][toRow+1] == nearSnakeHead){
                    disMap[toCol][toRow] = noGoInt;
                }
            }catch (Exception ignored){}

            try{
                if (map[toCol+1][toRow-1] == nearSnakeHead){
                    disMap[toCol][toRow] = noGoInt;
                }
            }catch (Exception ignored){}

            try{
                if (map[toCol-1][toRow-1] == nearSnakeHead){
                    disMap[toCol][toRow] = noGoInt;
                }
            }catch (Exception ignored){}
        }

        //endregion

        startMap = new int[width][height];

        //region Setting up startMap
        for (int row = 0; row < height; row++){
            for (int col = 0; col < width; col++){
                startMap[col][row] = pythag(startCoord,new coord(row, col));
            }
        }
        //endregion

        toVisit.add(startCoord);

        //region Writing Files

//
//        try{
//            log(timeStep + "");
//            String s = "loggie" + timeStep + ".txt";
//            createFile(s);
//            FileWriter fileWriter = new FileWriter(s);
//            fileWriter.write("\nMap\n");
//            fileWriter.write("===\n");
//            writeMap(map, fileWriter);
//            fileWriter.write("\n\n");
//            fileWriter.write("disMap\n");
//            fileWriter.write("======\n");
//            writeMap(disMap, fileWriter);
//            fileWriter.write("\n\n");
//            fileWriter.write("Move\n");
//            fileWriter.write("====\n");
//            fileWriter.write("\nHead currently is col" + startCoord.col + " with row " + startCoord.row);
//            try{
//                fileWriter.write("\ntoGo col is " + toGo.col + ", row is " + toGo.row);
//            }catch (Exception ignore){}
//            try{
//                fileWriter.write("\nApple col is " + apple.col + ", row is " + apple.row);
//            }catch (Exception ignore){}
//            fileWriter.write("\n\nDisMap");
//            try {
//                fileWriter.write("\ndisMap at row " + (startCoord.row+1) + ", col " + startCoord.col + " is disVal " + disMap[startCoord.col][startCoord.row + 1]);
//            }catch (Exception ignore){}
//
//            try{
//                fileWriter.write("\ndisMap at row " + (startCoord.row-1) + ", col " + startCoord.col + " is disVal " + disMap[startCoord.col][startCoord.row - 1]);
//            }catch (Exception ignore){}
//
//            try{
//                fileWriter.write("\ndisMap at row " + startCoord.row + ", col " + (startCoord.col + 1) + " is disVal " + disMap[startCoord.col+1][startCoord.row]);
//            }catch (Exception ignore){}
//
//            try{
//                fileWriter.write("\ndisMap at row " + startCoord.row + ", col " + (startCoord.col - 1) + " is disVal " + disMap[startCoord.col-1][startCoord.row]);
//            }catch (Exception ignore){}
//            fileWriter.close();
//        }catch (Exception e){
//            log(e.toString() + ", failed to write to file");
//        }


        //endregion

//        int multi = 1;
//
//        if (longMode){
//            multi *= 10;
//        }

        while (!toVisit.isEmpty()){
            iteration++;
            coord node;
            if (longMode){
                node = getLowest(toVisit);
            }else{
                node = getLowest(toVisit);
            }

            if (iteration > amountOfIterations){
                log("did too may iterations");
                return false;
            }

            toVisit.remove(node);
            visited.add(node);

            if (equalCoords(node,toGo)){
                move = getDirection(startCoord,returnSecondGrandParent(node));
                return isValidMove(startCoord,move,map,disMap);
            }

            //region Children
            ArrayList<coord> children = new ArrayList<>();
            int nRow = node.row;
            int nCol = node.col;

            {
                int val;

                //East
                try{
                    val = disMap[nCol +1][nRow];
                    if (val < thres){
                        children.add(new coord(nRow, nCol + 1, node));
                    }

                }catch (Exception ignore){}

                //West
                try{
                    val = disMap[nCol -1][nRow];
                    if (val < thres){
                        children.add(new coord(nRow, nCol - 1, node));
                    }
                }catch (Exception ignore){}

                //South
                try{
                    val = disMap[nCol][nRow+1];
                    if (val < thres){
                        children.add(new coord(nRow+1, nCol, node));
                    }
                }catch (Exception ignore){}

                //North
                try{
                    val = disMap[nCol][nRow-1];
                    if (val < thres){
                        children.add(new coord(nRow-1, nCol, node));
                    }
                }catch (Exception ignore){}

            }

            //endregion

            for (coord currChild : children){

                if (!inList(toVisit,currChild) && !inList(visited,currChild)){
                    toVisit.add(currChild);
                }
                else{
                    if (longMode){
                        if (getStartCurr(node) > getStartCurr(currChild)){
                            if (inList(toVisit,currChild)){
                                toVisit.remove(currChild);
                                visited.add(currChild);
                            }
                        }
                    }else{
                        if (getStartCurr(node) < getStartCurr(currChild)){
                            if (inList(toVisit,currChild)){
                                toVisit.remove(currChild);
                                visited.add(currChild);
                            }
                        }
                    }
                }
            }

        }

        log("I return false in goToCoord :(");
        return false;
    }

    public coord returnSecondGrandParent(coord c){
        coord temp = c;
        try{
            while (temp.parent.parent != null){
                temp = temp.parent;
            }
        }catch (Exception e){
            log("returnSecondGrandParent exception ticked");
            temp = c;
        }
        return temp;
    }

    public int getMapVal(coord c){
        return map[c.col][c.row];
    }

    public int getDisVal(coord c){
        return disMap[c.col][c.row];
    }

    public int getFn(coord c){
        return getStartCurr(c) + getDisVal(c);
    }

    public int getStartCurr(coord c){
        return startMap[c.col][c.row];
    }

    public boolean isLowestG(ArrayList<coord> lst, coord curr, coord startCoord){
        boolean isLowest = true;
        for (coord c : lst){
            if (getFn(curr) <= getFn(c)){
                isLowest = false;
                break;
            }
        }
        return isLowest;
    }

    public boolean inList(ArrayList<coord> lst, coord  c){
        for (coord p : lst){
            if (equalCoords(p, c)){
                return true;
            }
        }

        return false;
    }

    public coord getHighest(ArrayList<coord> lst){
        coord biggest = lst.get(0);
        int size = lst.size();

        for (int i = 1; i < size; i++){
            if (getFn(lst.get(i)) > getFn(biggest)){
                biggest = lst.get(i);
            }
        }

        return biggest;
    }

    public coord getLowest(ArrayList<coord> lst){
        coord smallest = lst.get(0);
        int size = lst.size();

        for (int i = 1; i < size; i++){
            if (getFn(lst.get(i)) < getFn(smallest)){
                smallest = lst.get(i);
            }
        }

        return smallest;
    }

    //endregion

    //region Game Logic
        public boolean isNotFill(coord toGo){
            return isNotFill(toGo, snakeArr[mySnake].length, noGoInt);
        }

        public boolean isNotFill(coord toGo, int maxLength, int thres){

            ArrayList<coord> toVisit = new ArrayList<>();
            ArrayList<coord> visited = new ArrayList<>();

            int nRow = toGo.row;
            int nCol = toGo.col;

            {
                int val;

                //East
                try{
                    val = disMap[nCol +1][nRow];
                    if (val < thres){
                        toVisit.add(new coord(nRow, nCol + 1));
                    }

                }catch (Exception ignore){}

                //West
                try{
                    val = disMap[nCol -1][nRow];
                    if (val < thres){
                        toVisit.add(new coord(nRow, nCol - 1));
                    }
                }catch (Exception ignore){}

                //South
                try{
                    val = disMap[nCol][nRow+1];
                    if (val < thres){
                        toVisit.add(new coord(nRow+1, nCol));
                    }
                }catch (Exception ignore){}

                //North
                try{
                    val = disMap[nCol][nRow-1];
                    if (val < thres){
                        toVisit.add(new coord(nRow-1, nCol));
                    }
                }catch (Exception ignore){}

            }

            while (!toVisit.isEmpty()){

                coord curr = toVisit.get(0);
                toVisit.remove(0);
                visited.add(curr);

                nRow = curr.row;
                nCol = curr.col;
                coord cTemp;

                if (visited.size() > maxLength){
                    break;
                }

                {
                    int val;

                    //East
                    try{
                        val = disMap[nCol +1][nRow];
                        cTemp = new coord(nRow, nCol + 1);
                        if (val < thres && !inList(visited,cTemp)){
                            toVisit.add(cTemp);
                        }

                    }catch (Exception ignore){}

                    //West
                    try{
                        val = disMap[nCol -1][nRow];
                        cTemp = new coord(nRow, nCol - 1);
                        if (val < thres && !inList(visited,cTemp)){
                            toVisit.add(cTemp);
                        }
                    }catch (Exception ignore){}

                    //South
                    try{
                        val = disMap[nCol][nRow+1];
                        cTemp = new coord(nRow+1, nCol);
                        if (val < thres && !inList(visited,cTemp)){
                            toVisit.add(cTemp);
                        }
                    }catch (Exception ignore){}

                    //North
                    try{
                        val = disMap[nCol][nRow-1];
                        cTemp = new coord(nRow-1, nCol);
                        if (val < thres && !inList(visited,cTemp)){
                            toVisit.add(cTemp);
                        }
                    }catch (Exception ignore){}

                }

            }

            return visited.size() > maxLength;

        }


        public boolean lowestMove(){
            try{
                return lowestMove(snakeArr[mySnake].head());
            }
            catch (Exception e){
                return false;
            }
        }

        public boolean lowestMove(coord curr){
            ArrayList<coord> neighbours = new ArrayList<>();
            //region Setting up Dismap
            for (int row = 0; row < height; row++){
                for (int col = 0; col < width; col++){
                    disMap[col][row] += truePythag(curr,new coord(row, col));

                    if (col == 0 || row == 0 || col == width-1 || row == height -1){
                        if (map[col][row] != appleNumb) {
                            disMap[col][row] += 15;
                        }
                        else{
                            disMap[col][row] -= 5000;
                        }
                    }

                    //NearSnakeHead
                    if (map[col][row] == nearSnakeHead){
                        disMap[col][row] += nearSnakeIncrease;

                        try{
                            if (map[col+1][row] == 0) disMap[col+1][row] += 125;
                        }catch (Exception ignore){}

                        try{
                            if (map[col-1][row] == 0) disMap[col-1][row] += 125;
                        }catch (Exception ignore){}

                        try{
                            if (map[col][row+1] == 0) disMap[col][row+1] += 125;
                        }catch (Exception ignore){}

                        try{
                            if (map[col][row-1] == 0) disMap[col][row-1] += 125;
                        }catch (Exception ignore){}

                        try{
                            if (map[col+1][row+1] == 0) disMap[col+1][row+1] += 50;
                        }catch (Exception ignore){}

                        try{
                            if (map[col-1][row+1] == 0) disMap[col-1][row+1] += 50;
                        }catch (Exception ignore){}

                        try{
                            if (map[col-1][row-1] == 0) disMap[col-1][row-1] += 50;
                        }catch (Exception ignore){}

                        try{
                            if (map[col+1][row-1] == 0) disMap[col+1][row-1] += 50;
                        }catch (Exception ignore){}

                        continue;
                    }

                    //Add values near enemy snakes bodies
                    if (map[col][row] == 1 || map[col][row] == 2 || map[col][row] == 3 || map[col][row] == 4 || map[col][row] == obstacleNumb){
                        disMap[col][row] = noGoInt;


                        if (map[col][row] != mySnake+1){
                            try{
                                disMap[col+1][row] += 30;
                            }catch (Exception ignore){}

                            try{
                                disMap[col-1][row] += 30;
                            }catch (Exception ignore){}

                            try{
                                disMap[col][row+1] += 30;
                            }catch (Exception ignore){}

                            try{
                                disMap[col][row-1] += 30;
                            }catch (Exception ignore){}
                        }

                    }

                    //If apple skip
                    if (map[col][row] == appleNumb){
                        int iCount = 0;

                        try{
                            if (disMap[col+1][row] > 100) iCount++;
                        }catch (Exception e){
                            iCount++;
                        }

                        try{
                            if (disMap[col-1][row] > 100) iCount++;
                        }catch (Exception e){
                            iCount++;
                        }

                        try{
                            if (disMap[col][row+1] > 100) iCount++;
                        }catch (Exception e){
                            iCount++;
                        }
                        try{
                            if (disMap[col][row-1] > 100) iCount++;
                        }catch (Exception e){
                            iCount++;
                        }

                        if (iCount > 1) disMap[col][row] += noGoInt;
                    };

                    //If smashed noGoInt
                    try{
                        if ((disMap[col+1][row] > noGoInt && disMap[col-1][row] > noGoInt) ||
                                (disMap[col][row+1] > noGoInt && disMap[col][row-1] > noGoInt)){
                            disMap[col][row] += nearSnakeIncrease/4;
                        }
                    }catch (Exception ignore){}


                }
            }
            //endregion

            int col = curr.col;
            int row = curr.row;


            try{
                neighbours.add(new coord(row+1,col));
            }catch (Exception ignore){}

            try{
                neighbours.add(new coord(row-1,col));
            }catch (Exception ignore){}

            try {
                neighbours.add(new coord(row, col + 1));
            }catch (Exception ignore){}

            try {
                neighbours.add(new coord(row, col - 1));
            }catch (Exception ignore){}

            int size = neighbours.size();

            if (size == 0){
                move = straight;
                return false;
            }

            for (int i = 0; i < size; i++){
                try{
                    int direc = getDirection(curr,neighbours.get(i));
                    if (isValidMove(curr,direc,map,disMap)){
                        return goToCoord(neighbours.get(i));
                    }
                }catch (Exception ignore){}
            }

            int smallVal = getDisVal(neighbours.get(0));
            int index = 0;

            for (int i = 1; i < size; i++){
                if (getDisVal(neighbours.get(i)) < smallVal){
                    smallVal = getDisVal(neighbours.get(i));
                    index = i;
                }
            }

            move = getDirection(curr,neighbours.get(index));
            log("trying my best");
            return true;

        }

        public boolean isAppleViable(){
            appleChangeReset();
            if (!isAppleHealthy){
                map[apple.col][apple.row] = obstacleNumb;
            }
            return isAppleHealthy && iMakeAppleFirst();
        }

        public boolean iMakeItFirst(coord toGo){
            return mySnake == whoMakeItFirst(toGo);
        }

        public boolean iMakeAppleFirst(){
            return iMakeItFirst(apple);
        }

        public boolean goToApple(){
            return goToCoord(apple);
        }

        public boolean isCenterViable(){
            int row = height/2;
            int col = width/2;

            int middle = map[row][col];
            int top = map[row + 1][col];
            int right = map[row][col + 1];
            int topRight = map[row + 1][col + 1];

            //If not the players snake, plain ground or an obstacle, center is not viable
            return  (middle == 0 || middle == mySnake || middle == -1)
                    && (top == 0 || top == mySnake || top == -1)
                    && (right == 0 || right == mySnake || right == -1)
                    && (topRight == 0 || topRight == mySnake || topRight == -1);
        }

        public boolean goToCenter(){
            coord center = new coord((int)(height/2), (int)(width/2));

            if (pythagNoRoot(center,snakeArr[mySnake].head()) < 5){
                return false;
            }

            return goToCoord(center);
        }

        public int findClosetSnake(coord pos){
            int snakeBodySize = snakeArr[0].body.size();
            coord snake = snakeArr[0].body.get(snakeBodySize/2);
            int smallest = 0;

            int size = snakeArr.length;

            for (int i = 1; i < size; i++){
                snakeBodySize = snakeArr[i].body.size();
                coord tempSnake = snakeArr[i].body.get(snakeBodySize/2);

                if (pythagNoRoot(tempSnake, pos) < pythagNoRoot(snake, pos)){
                    snake = tempSnake;
                    smallest = i;
                }
            }

            return smallest;

        }

        public boolean survive(boolean force){

            int sumCol = 0;
            int sumRow = 0;

            for (int i = 0; i < size; i++){
                log("Damn this keyboard feels really nice actually, like nah famn this laptop keyboard is the freaking shit man xD!" +
                        "Yea i really love this keyboard dman. Please stop talking ma you making a really loud noise and you know you cant hear well you know that jeez i really love this keyboard so damn muchQQQQQQQQQQQQAGGG ITS SOC OOL AND SOFT TO TOUCH")
            }

            for (Snake s : snakeArr){
                if (s.pos != mySnake){
                    sumCol += s.head().col;
                    sumRow += s.head().row;
                }
            }

            sumRow /= (numbSnakes - 1);
            sumCol /= (numbSnakes - 1);

            coord toGo = new coord(height - sumRow, width - sumCol);
            int iCount = 0;
            while (pythagNoRoot(toGo, snakeArr[mySnake].head()) < 15 || !isViableSpot(toGo)){
                if (iCount > 25) break;
                toGo = freeSpace();
                iCount++;
            }

           return goToCoord(toGo, force, true);


        }

        public int getDirection(coord curr, coord toGo){

            int x = curr.col - toGo.col;
            int y = curr.row - toGo.row;

            if (x < 0){
                return east;
            }
            else if (x > 0){
                return west;
            }

            if (y < 0){
                return south;
            }

            return north;
        }




        //region Game Logic Helper Functions
        public void appleChangeReset(){
            int distance = (pythag(snakeArr[mySnake].head(), apple));
//            log("distance is " + distance + " and appleLife is " + (badApple - (decayRate * appleTime)) *10);
            isAppleHealthy = (distance) < (badApple - (decayRate * appleTime))* 10 ;
        }

        coord freeSpace(){
            Random r = new Random();
            int rCol = r.nextInt(width-5)+2;
            int rRow = r.nextInt(height-5)+2;
            boolean isFree = map[rCol][rRow] == 0 && disMap[rCol][rRow] < nearSnakeIncrease;
            int iCount = 0;
            while (!isFree){
                if (iCount > 20) break;
                rCol = r.nextInt(width-5)+2;
                rRow = r.nextInt(height-5)+2;

                isFree = map[rCol][rRow] == 0 && disMap[rCol][rRow] < nearSnakeIncrease;
                iCount++;
            }

            return new coord(rRow, rCol);
        }



        //endregion

    //endregion

    //region Helper Functions

    public void printStartCurr(ArrayList<coord> lst, coord startCoord){
        for (coord c : lst){
            printCoord(c);
            log(" with g(n) being " + getStartCurr(c));
        }
    }

    public void printCoord(ArrayList<coord> lst){
        for (coord c : lst){
            printCoord(c);
        }
    }

    public boolean isViableSpot(coord c){
        int col = c.col;
        int row = c.row;
        int iAroundBig = 0;

        try{
            if (disMap[col+1][row] >= nearSnakeIncrease && map[col+1][row] != obstacleNumb) iAroundBig++;
        }catch (Exception e){
            iAroundBig++;
        }

        try{
            if (disMap[col-1][row] >= nearSnakeIncrease && map[col-1][row] != obstacleNumb) iAroundBig++;
        }catch (Exception e){
            iAroundBig++;
        }

        try{
            if (disMap[col][row+1] >= nearSnakeIncrease && map[col][row+1] != obstacleNumb) iAroundBig++;
        }catch (Exception e){
            iAroundBig++;
        }

        try{
            if (disMap[col][row-1] >= nearSnakeIncrease && map[col][row-1] != obstacleNumb) iAroundBig++;
        }catch (Exception e){
            iAroundBig++;
        }

        return map[col][row] == 0 && disMap[col][row] < nearSnakeIncrease && iAroundBig <= 3;

    }

    public boolean isViableSpot(int row,int col){
        return map[col][row] == 0;
    }

    public boolean isValidMove(coord head, int move,int[][] map, int[][] disMap){
        if (move == straight) return false;
        coord nextCoord = nextLoc(head,move);
        int nextLoc;
        int col = nextCoord.col;
        int row = nextCoord.row;

        try{
            nextLoc = map[col][row];
        }catch (Exception e) {
            return false;
        }

        if (nextLoc == 1 || nextLoc == 2 || nextLoc == 3 || nextLoc == 4 || nextLoc == obstacleNumb){
            return false;
        }

        if (getDisVal(nextCoord) > noGoInt*0.8) return false;

        int around = 0;

        try{
            if (disMap[col+1][row] >= nearSnakeIncrease/4) around++;
        }catch (Exception e){
            around++;
        }

        try{
            if (disMap[col-1][row] >= nearSnakeIncrease/4) around++;
        }catch (Exception e){
            around++;
        }

        try{
            if (disMap[col][row+1] >= nearSnakeIncrease/4) around++;
        }catch (Exception e){
            around++;
        }

        try{
            if (disMap[col][row-1] >= nearSnakeIncrease/4) around++;
        }catch (Exception e){
            around++;
        }

        if (!isNotFill(nextCoord)) return false;

        return around < 4;
    }

    public Snake readSnake(String sLine, int pos) {
        String[] sArr = sLine.split(" ");

        boolean isAlive = sArr[0].equals("alive");

        int length = Integer.parseInt(sArr[1]);

        int kills = Integer.parseInt(sArr[2]);

        Snake s = new Snake(pos, isAlive, length, kills);

        int sArrSize = sArr.length;

        for (int i = 3; i < sArrSize; i++) {
            s.append(getCoordComma(sArr[i]));
        }

        return s;
    }

    public void surround(coord toSurround){
        int length = snakeArr[mySnake].length;

    }

    public coord getCoordComma(String sLine) {
        String[] sCoords = sLine.split(",");

        return new coord(Integer.parseInt(sCoords[1]), Integer.parseInt(sCoords[0]));
    }

    public int toOne(int n){
        return n/Math.abs(n);
    }

    public int pythag(coord c1, coord c2){
        int x = c1.col - c2.col;
        int y = c1.row - c2.row;

        return (int) Math.ceil(Math.sqrt(x*x + y*y));
    }


    public coord nextLoc (coord me, int direc){
        if (direc == east) {
            return new coord(me.row,me.col + 1);
        } else if (direc == west) {
            return new coord(me.row,me.col - 1);
        } else if (direc == north) {
            return new coord(me.row - 1,me.col);
        } else{
            return new coord(me.row + 1,me.col);
        }
    }

    public void writeMap(int[][] map, FileWriter fileWriter){
        for (int i = 0; i < map.length; i++){
            String sTemp = "";
            for (int j = 0; j < map[0].length; j++){
                sTemp += map[j][i] + "\t";
            }

            sTemp += "\n";

            try{
                fileWriter.write(sTemp);
            }catch (Exception e){
                log("wrtie map failed");
            }

        }
    }

    public void printMap(int[][] map){
        for (int i = 0; i < map.length; i++){
            String sTemp = "";
            for (int j = 0; j < map[0].length; j++){
                sTemp += map[j][i] + "\t";
            }

            sTemp += "\n";

            try{
                log(sTemp);
            }catch (Exception e){
                log("print map failed");
            }

        }
    }

    public void printMapAround(int[][] map, coord c){
        int col = c.col;
        int row = c.row;
        String sTemp;

        log("");

        try{
            sTemp = map[col-1][row-1] + " " +
                    map[col][row - 1] + " " + map[col + 1][row-1];
            log(sTemp);
        }catch (Exception e){}

        try{
            sTemp = map[col-1][row] + " " +
            map[col][row] + " " + map[col + 1][row];
            log(sTemp);
        }catch (Exception e){}

        try{
            sTemp = map[col-1][row+1] + " " +
                    map[col][row+1] + " " + map[col + 1][row+1];
            log(sTemp);
        }catch (Exception e){}

        log("");


    }

    public int pythagNoRoot(int col1, int col2, int row1, int row2){
        return (Math.abs(col1 - col2) + Math.abs(row1 - row2));
    }

    public int pythagNoRoot(coord c1, coord c2){
        return pythagNoRoot(c1.col, c2.col, c1.row, c2.row);
    }

    public void log(String error){
//        System.err.println(error);
    }

    public void printCoord(coord c){
        log("x is " + c.col + " ,y is " + c.row);
    }

    public void createFile(String str){
        File myFile = new File(str);
        try {
            myFile.createNewFile();
        }catch (Exception e){
            log("Failed to create file, " + e.toString());
        }
    }

    public void setMap(){
        clearMap();

        //Place obstacles
        for (coord i : obstacles){
            map[i.col][i.row] = obstacleNumb;
        }

        //Place Apple
        map[apple.col][apple.row] = appleNumb;

        //Place Snakes
        for (int i = 0; i < numbSnakes; i++){

            if (!snakeArr[i].isAlive){
                continue;
            }

            try {
                int snakeBodySize = snakeArr[i].body.size();

                for (int j = 1; j < snakeBodySize; j++) {
                    coord o = snakeArr[i].body.get(j - 1);
                    coord c = snakeArr[i].body.get(j);

                    drawSnakeOnMap(o, c, snakeArr[i].pos + 1);
                }

                if (i != mySnake) {
                    int col = snakeArr[i].head().col;
                    int row = snakeArr[i].head().row;

                    try {
                        if (map[col + 1][row] == 0){
                            map[col + 1][row] = nearSnakeHead;
                        }
                    } catch (Exception e) {
                    }

                    try {
                        if (map[col - 1][row] == 0){
                            map[col - 1][row] = nearSnakeHead;
                        }
                    } catch (Exception e) {
                    }


                    try {
                        if (map[col][row + 1] == 0) {
                            map[col][row + 1] = nearSnakeHead;
                        }
                    } catch (Exception e) {
                    }

                    try {
                        if (map[col][row - 1] == 0) {
                            map[col][row - 1] = nearSnakeHead;
                        }
                    } catch (Exception e) {
                    }

                    try {
                        if (map[col + 1][row + 1] == 0) {
                            map[col + 1][row + 1] = nearSnakeHead;
                        }
                    } catch (Exception e) {
                    }

                    try {
                        if (map[col - 1][row - 1] == 0) {
                            map[col - 1][row - 1] = nearSnakeHead;
                        }
                    } catch (Exception e) {
                    }

                    try {
                        if (map[col + 1][row - 1] == 0) {
                            map[col + 1][row - 1] = nearSnakeHead;
                        }
                    } catch (Exception e) {
                    }

                    try {
                        if (map[col - 1][row + 1] == 0) {
                            map[col - 1][row + 1] = nearSnakeHead;
                        }
                    } catch (Exception e) {}

                }
            }
            catch (Exception e){
            }

        }
    }


    public void drawSnakeOnMap(coord start, coord end, int pos){
        try {
            int startRow = start.row;
            int startCol = start.col;

            int endRow = end.row;
            int endCol = end.col;

            int rowDiff = startRow - endRow;
            int colDiff = startCol - endCol;


            if (rowDiff == 0) {
                int size = Math.abs(colDiff);
                int one = toOne(colDiff);

                for (int i = 0; i <= size; i++) {
                    map[startCol][startRow] = pos;
                    startCol -= one;
                }
            } else {
                int size = Math.abs(rowDiff);
                int one = toOne(rowDiff);

                for (int i = 0; i <= size; i++) {
                    map[startCol][startRow] = pos;
                    startRow -= one;
                }
            }
        }
        catch (Exception e){
            log(e.toString());
        }
    }

    public void clearMap(){
        map = new int[height][width];
    }

    public int whoMakeItFirst(coord toGo){
        int distance;

        try{
            distance = truePythag(snakeArr[0].head(),toGo)*2;
        }catch (Exception e){
            distance = 10000;
        }

        int index = snakeArr[0].pos;

        for (int i = 1; i < numbSnakes; i++){
            int nextDistance;

            try{
                nextDistance = truePythag(snakeArr[i].head(),toGo)*2;
            }catch (Exception e){
                nextDistance = 10000;
            }

            if (nextDistance <= distance){
                index = snakeArr[i].pos;
                distance = nextDistance;
            }
        }

        return index;
    }

    public int truePythag(coord curr, coord toGo){
        int col = Math.abs(curr.col - toGo.col);
        int row = Math.abs(curr.row - toGo.row);

        return col + row;
    }

    public boolean equalCoords(coord c1, coord c2){
        return c1.row == c2.row && c1.col == c2.col;
    }
    //endregion
}