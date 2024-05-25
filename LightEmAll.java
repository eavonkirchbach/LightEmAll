import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.Deque;
import java.util.HashMap;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// EXTRA CREDIT OVERVIEW:
// - enhanced graphics through color changing as power station moves
// - added a time feature to the game and displayed it and utilized mutation techniques for this
// - added a clicks counter to the game and displayed it and utilized mutation techniques for this
// - added the ability to restart the game after losing and utilized mutation techniques in this
// - added the ability to restart the game at any point during the game
// - added a high score / fastest time ability and utilized mutation techniques for this


// represents the LightEmAll class
class LightEmAll extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  int tileSize;
  int rows;
  int cols;
  Random rand;
  int won;
  boolean useKruskal;
  int time;
  int clicks;
  int fastest;
  int latest;

  // constructor with random seed 
  LightEmAll(int width, int height, int tileSize, Random rand, boolean useKruskal) {
    this.width = width;
    this.height = height;
    this.tileSize = tileSize;

    this.rows = this.height / this.tileSize;
    this.cols = this.width / this.tileSize;

    this.board = new ArrayList<ArrayList<GamePiece>>();
    this.nodes = new ArrayList<GamePiece>();

    this.rand = rand;

    this.useKruskal = useKruskal;

    this.clicks = 0;
    this.latest = 1000000;



    if (useKruskal) {
      this.powerRow = 0;
      this.powerCol = 0;
      this.createConnectedBoard();
    }
    else {
      this.powerRow = this.rows / 2;
      this.powerCol = this.cols / 2;
      this.createBoard();
    }


    if (this.board.size() > 0) {
      this.board.get(this.powerCol).get(this.powerRow).setIsPowerStation(true);
    }



    this.setEdges();
    if (useKruskal) {
      this.createMST();
    }
    this.randomizeEdges();
    this.setEdges();
    this.setAllOuterEdges();
    this.setTilesPowered();
  }


  // constructor with random seed 5
  LightEmAll(int width, int height) {
    this(width, height, 25, new Random(5), true);
  }

  // constructor with random seed 5
  LightEmAll(int width, int height, int tileSize, Random rand) {
    this(width, height, tileSize, rand, true);
  }

  // sets the out edges for all game pieces 
  void setAllOuterEdges() {
    for (int col = 0; col < this.board.size(); col++) {
      for (int row = 0; row < this.board.get(col).size(); row++) {
        this.board.get(col).get(row).setOutEdges(this.mst);
      }
    }
  }

  // creates the board -- for part 1. This sets the manual board 
  void createBoard() {
    for (int i = 0; i < cols; i++) {
      this.board.add(new ArrayList<GamePiece>());
      for (int j = 0; j < rows; j++) {
        if (j == (rows / 2)) {
          if (i == 0) {
            this.board.get(i).add(new GamePiece(i, j, false, true, true, true, false, false));
          }
          else if (i == cols - 1) {
            this.board.get(i).add(new GamePiece(i, j, true, false, true, true, false, false));
          }
          else if (i == this.powerCol && j == this.powerRow) {
            this.board.get(i).add(new GamePiece(i, j, true, true, true, true, true, true));
          }
          else {
            this.board.get(i).add(new GamePiece(i, j, true, true, true, true, false, false));
          }
        }
        else {
          if (j == 0) {
            this.board.get(i).add(new GamePiece(i, j, false, false, false, true, false, false));
          }
          else if (j == rows - 1) {
            this.board.get(i).add(new GamePiece(i, j, false, false, true, false, false, false));
          }
          else {
            this.board.get(i).add(new GamePiece(i, j, false, false, true, true, false, false));
          }
        }
        nodes.add(this.board.get(i).get(j));
      }
    }
  }

  // creates the mst using Kruskal's algorithm 
  void createMST() {
    // initialize variables for kruskal's algorithm 
    HashMap<GamePiece, GamePiece> representatives = new HashMap<>();
    ArrayList<Edge> edgesInTree = new ArrayList<>();
    ArrayList<Edge> worklist = new ArrayList<>(this.mst);
    worklist.sort(new CompareEdge());

    // Initialize every node's representative to itself
    for (GamePiece node : this.nodes) {
      representatives.put(node, node);
    }

    while (edgesInTree.size() < this.nodes.size() && !worklist.isEmpty()) {
      Edge nextEdge = worklist.remove(0);
      GamePiece x = find(representatives, nextEdge.from);
      GamePiece y = find(representatives, nextEdge.to);

      if (!x.gamePieceEquals(y)) {
        edgesInTree.add(nextEdge);
        union(representatives, x, y);
      }
    }

    this.mst = edgesInTree;


    // sets everything to false, since it is true by default to create set of all possible edges 
    for (int col = 0; col < this.cols; col++) {
      for (int row = 0; row < this.rows; row++) {
        this.board.get(col).get(row).setLeft(false);
        this.board.get(col).get(row).setRight(false);
        this.board.get(col).get(row).setTop(false);
        this.board.get(col).get(row).setBottom(false);
        this.board.get(col).get(row).setPowered(false);
      }
    }

    // sets the wires such that the board is a reflection of the mst 
    for (Edge e : this.mst) {
      if (e.compareRows() == 0) {
        if (e.compareCols() < 0) {
          e.setPieceDirection("from", "right", true);
          e.setPieceDirection("to", "left", true);
        } else {
          e.setPieceDirection("from", "left", true);
          e.setPieceDirection("to", "right", true);
        }
      } else {
        if (e.compareRows() < 0) {
          e.setPieceDirection("from", "bottom", true);
          e.setPieceDirection("to", "top", true);
        } else {
          e.setPieceDirection("from", "top", true);
          e.setPieceDirection("to", "bottom", true);
        }
      }
    }
  }

  // find the value of the given game piece in the given hashmap
  GamePiece find(HashMap<GamePiece, GamePiece> representatives, GamePiece node) {
    if (!representatives.get(node).equals(node)) {
      representatives.put(node, find(representatives, representatives.get(node)));
    }
    return representatives.get(node);
  }

  // sets the representative for the given game piece to the
  // other given game piece in the given hashmap
  void union(HashMap<GamePiece, GamePiece> representatives, GamePiece x, GamePiece y) {
    representatives.put(find(representatives, x), find(representatives, y));
  }

  // creates a fully connected board used to find the set of all possible edges 
  // when running Kruskal's algorithm 
  void createConnectedBoard() {
    this.board = new ArrayList<ArrayList<GamePiece>>();
    for (int col = 0; col < this.cols; col++) {
      this.board.add(new ArrayList<GamePiece>());
      for (int row = 0; row < this.rows; row++) {
        this.board.get(col).add(new GamePiece(row, col, true, true, true, true, false, false));
        this.nodes.add(this.board.get(col).get(row));
      }
    }
  }

  // randomize the orientation of the edges 
  void randomizeEdges() {
    for (int col = 0; col < this.board.size(); col++) {
      for (int row = 0; row < this.board.get(col).size(); row++) {
        int randInt = rand.nextInt(4);
        GamePiece tile = this.board.get(col).get(row);
        for (int i = 0; i < randInt; i++) {
          tile.rotate();
        }
      }
    }
  }

  // sets the powered status of tiles 
  void setTilesPowered() {
    for (int col = 0; col < this.board.size(); col++) {
      for (int row = 0; row < this.board.get(col).size(); row++) {
        GamePiece piece = this.board.get(col).get(row);
        if (this.bfs(piece, this.board.get(this.powerCol).get(this.powerRow))) {
          piece.setPowered(true);
        }
        else {
          piece.setPowered(false);
        }
      }
    }
  }

  // set the edges array such that every connecting wire is represented
  void setEdges() {
    ArrayList<Edge> edges = new ArrayList<>();

    for (int col = 0; col < this.cols; col++) {
      for (int row = 0; row < this.rows; row++) {
        int weight = this.rand.nextInt(500);
        if (!this.useKruskal) {
          weight = 1;
        }
        // check the bottom for a connection
        if (row + 1 < this.rows 
            && this.board.get(col).get(row + 1).hasTop() 
            && this.board.get(col).get(row).hasBottom()) {
          edges.add(new Edge(this.board.get(col).get(row), this.board.get(col).get(row + 1),
              weight));
        }

        // check the top for a connection
        if (row > 0 && this.board.get(col).get(row - 1).hasBottom() 
            && this.board.get(col).get(row).hasTop()) {
          edges.add(new Edge(this.board.get(col).get(row), this.board.get(col).get(row - 1),
              weight));
        }

        // check the left for a connection
        if (col > 0 && this.board.get(col - 1).get(row).hasRight() 
            && this.board.get(col).get(row).hasLeft()) {
          edges.add(new Edge(this.board.get(col).get(row), this.board.get(col - 1).get(row),
              weight));
        }

        // check the right for a connection
        if (col + 1 < this.cols && this.board.get(col + 1).get(row).hasLeft() 
            && this.board.get(col).get(row).hasRight()) {
          edges.add(new Edge(this.board.get(col).get(row), this.board.get(col + 1).get(row),
              weight));
        }
      }
    }

    this.mst = edges;
  }

  // updates the game state every tick
  public void onTick() {
    if (this.won == 0) {
      this.time++;
    }
  }

  // renders the world 
  public WorldScene makeScene() {
    WorldScene ws = new WorldScene(this.width, this.height);
    for (int i = 0; i < this.board.size(); i++) {
      for (int j = 0; j < this.board.get(i).size(); j++) {
        GamePiece piece = this.board.get(i).get(j);
        Color color = Color.black;
        if (piece.isPowered()) {
          int green = 255 - 25 * (int) Math.sqrt(Math.pow(this.powerCol - i, 2) 
              + Math.pow(this.powerRow - j, 2));
          color = new Color(255, green, 0);
        }
        ws.placeImageXY(piece.tileImage(this.tileSize, 15, color, piece.isPowerStation()),
            this.tileSize * i + this.tileSize / 2, this.tileSize * j + this.tileSize / 2);
        ws.placeImageXY(
            new RectangleImage(this.tileSize, this.tileSize, OutlineMode.OUTLINE, Color.black),
            this.tileSize * i + this.tileSize / 2, this.tileSize * j + this.tileSize / 2);
        if (won == 1) {
          ws.placeImageXY(new RectangleImage(200, 180, OutlineMode.SOLID, Color.GRAY),
              200, 215);
          ws.placeImageXY(new RectangleImage(200, 180, OutlineMode.OUTLINE, Color.ORANGE),
              200, 215);
          ws.placeImageXY(new TextImage("You win!!", 35, FontStyle.BOLD, Color.BLACK),
              203, 180);
          ws.placeImageXY(new TextImage("You win!!", 35, FontStyle.BOLD, Color.ORANGE),
              200, 180);
          ws.placeImageXY(new TextImage("Time: " + Integer.toString(this.time),
              35, FontStyle.BOLD, Color.ORANGE),
              200, 230);
          ws.placeImageXY(new TextImage("Fastest time: " + Integer.toString(this.fastest),
              20, FontStyle.BOLD, Color.ORANGE),
              200, 275);
        } 
      }
    }
    ws.placeImageXY(new RectangleImage(115, 40, OutlineMode.SOLID, Color.GRAY), 65, 475);
    ws.placeImageXY(new RectangleImage(115, 40, OutlineMode.OUTLINE, Color.ORANGE), 65, 475);
    ws.placeImageXY(new TextImage("Time: " + Integer.toString(this.time),
        15, FontStyle.BOLD, Color.ORANGE),
        65, 475);
    ws.placeImageXY(new RectangleImage(125, 40, OutlineMode.SOLID, Color.GRAY), 200, 475);
    ws.placeImageXY(new RectangleImage(125, 40, OutlineMode.OUTLINE, Color.ORANGE), 200, 475);
    ws.placeImageXY(new TextImage("Clicks: " + Integer.toString(this.clicks),
        15, FontStyle.BOLD, Color.ORANGE),
        200, 475);
    ws.placeImageXY(new RectangleImage(115, 40, OutlineMode.SOLID, Color.GRAY), 330, 475);
    ws.placeImageXY(new RectangleImage(115, 40, OutlineMode.OUTLINE, Color.ORANGE), 330, 475);
    ws.placeImageXY(new TextImage("Restart", 15, FontStyle.BOLD, Color.ORANGE),
        330, 475);
    return ws;
  }


  //rotates the piece when the mouse is clicked 
  public void onMouseClicked(Posn pos) {
    // finds the game piece clicked
    int col = pos.x / this.tileSize;
    int row = pos.y / this.tileSize;

    if (col < this.cols && row < this.rows) {
      GamePiece tile = this.board.get(col).get(row);

      // rotates the gamepiece and resets the edges 
      tile.rotate();
      this.setEdges();
      this.setAllOuterEdges();
      this.setTilesPowered();

      // increases the clicks
      clicks++;

      // ends the game if all pieces are powered
      if (this.isGameOver()) {
        this.fastest = Math.min(this.time, this.latest);
        this.won = 1;
        this.latest = this.time;
      }
    }
    // resets the game if the reset button is clicked 
    if (pos.x >= 272.5 && pos.x <= 387.5 && pos.y >= 455 && pos.y <= 495) {
      this.won = 0;
      this.time = 0;
      this.clicks = 0;
      this.powerRow = 0;
      this.powerCol = 0;
      this.createConnectedBoard();
      this.setEdges();
      this.createMST();
      this.randomizeEdges();
      this.setEdges();
      this.setAllOuterEdges();
      this.setTilesPowered();
      this.board.get(this.powerCol).get(this.powerRow).setIsPowerStation(true);
    }
  }

  // checks if all game pieces are powered 
  boolean isGameOver() {
    for (GamePiece node : this.nodes) {
      if (!node.isPowered()) {
        return false;
      }
    }
    return true;
  }


  //  moves the power station when the mouse is clicked 
  public void onKeyEvent(String key) {
    GamePiece currentPowerPiece = this.board.get(this.powerCol).get(this.powerRow);
    currentPowerPiece.setIsPowerStation(false);
    if (key.equals("left")) {
      if (this.powerCol > 0 
          && currentPowerPiece.hasLeft() 
          && this.board.get(this.powerCol - 1).get(this.powerRow).hasRight()) {
        this.powerCol--;
      }
    }
    else if (key.equals("right")) {
      if (this.powerCol < this.cols - 1 
          && currentPowerPiece.hasRight() 
          && this.board.get(this.powerCol + 1).get(this.powerRow).hasLeft()) {
        this.powerCol++;
      }
    }
    else if (key.equals("up")) {
      if (this.powerRow > 0 
          && currentPowerPiece.hasTop() 
          && this.board.get(this.powerCol).get(this.powerRow - 1).hasBottom()) {
        this.powerRow--;
      }
    }
    else if (key.equals("down")) {
      if (this.powerRow < this.rows - 1  
          && currentPowerPiece.hasBottom() 
          && this.board.get(this.powerCol).get(this.powerRow + 1).hasTop()) {
        this.powerRow++;
      }
    }
    this.board.get(this.powerCol).get(this.powerRow).setIsPowerStation(true);
    this.setEdges();
    this.setAllOuterEdges();
    this.setTilesPowered();
  }

  // helps to conduct binary first search
  boolean bfs(GamePiece from, GamePiece to) {
    return searchHelp(from, to, new Queue<GamePiece>());
  }

  // helper for bfs method
  boolean searchHelp(GamePiece from, GamePiece to, ICollection<GamePiece> worklist) {
    Deque<GamePiece> alreadySeen = new ArrayDeque<GamePiece>();

    // Initialize the worklist with the from vertex
    worklist.add(from);
    // As long as the worklist isn't empty...
    while (!worklist.isEmpty()) {
      GamePiece next = worklist.remove();
      if (next.equals(to)) {
        return true; // Success!
      }
      else if (alreadySeen.contains(next)) {
        // do nothing: we've already seen this one
      }
      else {
        // add all the neighbors of next to the worklist for further processing
        for (Edge e : next.outEdges) {
          worklist.add(e.to);
        }
        // add next to alreadySeen, since we're done with it
        alreadySeen.addFirst(next);
      }
    }
    // We haven't found the to vertex, and there are no more to try
    return false;
  }
}


// represents a GamePiece
class GamePiece {
  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;
  // whether the power station is on this piece
  boolean powerStation;
  boolean powered;
  ArrayList<Edge> outEdges;

  GamePiece(int row, int col, boolean left, boolean right, boolean top, 
      boolean bottom, boolean powerStation, boolean powered) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.powered = powered;
    this.outEdges = new ArrayList<Edge>();
  }

  // sets out edges 
  void setOutEdges(ArrayList<Edge> edges) {
    ArrayList<Edge> outEdges = new ArrayList<Edge>();
    for (int i = 0; i < edges.size(); i++) {
      if (edges.get(i).fromEquals(this)) {
        outEdges.add(edges.get(i));
      }
    }
    this.outEdges = outEdges;
  }

  // sets the powered status of this cell
  void setPowered(boolean powered) {
    this.powered = powered;
  }

  // checks if the piece is powered
  boolean isPowered() {
    return this.powered;
  }

  // rotates this piece 
  void rotate() {
    boolean temp = this.left;
    this.left = this.bottom;
    this.bottom = this.right;
    this.right = this.top;
    this.top = temp;
  }

  // sets the left field 
  void setLeft(boolean left) {
    this.left = left;
  }

  // sets the right field
  void setRight(boolean right) {
    this.right = right;
  }

  // sets the top field
  void setTop(boolean top) {
    this.top = top;
  }

  // sets the bottom field
  void setBottom(boolean bottom) {
    this.bottom = bottom;
  }

  // checks if the wire goes left 
  boolean hasLeft() {
    return this.left;
  }

  // checks if the wire goes right
  boolean hasRight() {
    return this.right;
  }

  // checks if the wire goes down
  boolean hasBottom() {
    return this.bottom;
  }

  // checks if the wire goes up
  boolean hasTop() {
    return this.top;
  }

  // checks if a piece is a power station
  boolean isPowerStation() {
    return this.powerStation;
  }

  // set if a piece is a power station
  void setIsPowerStation(boolean powerStation) {
    this.powerStation = powerStation;
  }

  // checks if a game piece is equal to another game piece
  boolean gamePieceEquals(GamePiece that) {
    return this.row == that.row
        && this.col == that.col
        && this.left == that.left
        && this.right == that.right
        && this.top == that.top
        && this.bottom == that.bottom
        && this.powerStation == that.powerStation
        && this.powered == that.powered;
  }


  // Generate an image of this, the given GamePiece.
  // - size: the size of the tile, in pixels
  // - wireWidth: the width of wires, in pixels
  // - wireColor: the Color to use for rendering wires on this
  // - hasPowerStation: if true, draws a fancy star on this tile to represent the power station
  WorldImage tileImage(int size, int wireWidth, Color wireColor, boolean hasPowerStation) {
    // Start tile image off as a blue square with a wire-width square in the middle,
    // to make image "cleaner" (will look strange if tile has no wire, but that can't be)
    WorldImage image = new OverlayImage(
        new RectangleImage(wireWidth, wireWidth, OutlineMode.SOLID, wireColor),
        new RectangleImage(size, size, OutlineMode.SOLID, Color.DARK_GRAY));
    WorldImage vWire = new RectangleImage(wireWidth, (size + 1) / 2, OutlineMode.SOLID, wireColor);
    WorldImage hWire = new RectangleImage((size + 1) / 2, wireWidth, OutlineMode.SOLID, wireColor);

    if (this.top) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, 0, image);
    }
    if (this.right) {
      image = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    if (this.bottom) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, vWire, 0, 0, image);
    }
    if (this.left) {
      image = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    if (hasPowerStation) {
      image = new OverlayImage(
          new OverlayImage(
              new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
              new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))),
          image);
    }
    return image;
  }

  // compares the row of this game piece to the row of a given game piece
  int compareGamePieceRows(GamePiece that) {
    return this.row - that.row;
  }

  // compares the cols of this game piece to the row of a given game piece
  int compareGamePieceCols(GamePiece that) {
    return this.col - that.col;
  }
}

// represents an edge
class Edge {
  GamePiece to;
  GamePiece from;
  int weight;

  Edge(GamePiece to, GamePiece from, int weight) {
    this.to = to;
    this.from = from;
    this.weight = weight;
  }

  // checks if the from game piece matches the given game piece
  boolean fromEquals(GamePiece that) {
    return this.from.gamePieceEquals(that);
  }

  // compares the rows of the two game piece fields 
  int compareRows() {
    return this.to.compareGamePieceRows(this.from);
  }

  // compares the cols of the two game piece fields 
  int compareCols() {
    return this.to.compareGamePieceCols(this.from);
  }

  // sets the value of a field for a given piece
  void setPieceDirection(String piece, String dir, boolean val) {
    if (piece.equals("to")) {
      if (dir.equals("left")) {
        this.to.setLeft(val);
      }
      else if (dir.equals("right")) {
        this.to.setRight(val);
      }
      else if (dir.equals("top")) {
        this.to.setTop(val);
      }
      else if (dir.equals("bottom")) {
        this.to.setBottom(val);
      }
    }
    else if (piece.equals("from")) {
      if (dir.equals("left")) {
        this.from.setLeft(val);
      }
      else if (dir.equals("right")) {
        this.from.setRight(val);
      }
      else if (dir.equals("top")) {
        this.from.setTop(val);
      }
      else if (dir.equals("bottom")) {
        this.from.setBottom(val);
      }
    }
  }
}

// Represents a mutable collection of items
interface ICollection<T> {

  // Is this collection empty?
  boolean isEmpty();

  // EFFECT: adds the item to the collection
  void add(T item);

  // Returns the first item of the collection
  // EFFECT: removes that first item
  T remove();
}


// represents a Queue
class Queue<T> implements ICollection<T> {
  Deque<T> contents;

  Queue() {
    this.contents = new ArrayDeque<T>();
  }

  //Is this collection empty?
  public boolean isEmpty() {
    return this.contents.isEmpty();
  }

  //Returns the first item of the collection
  // EFFECT: removes that first item
  public T remove() {
    return this.contents.removeFirst();
  }

  //EFFECT: adds the item to the collection
  public void add(T item) {
    this.contents.addLast(item);
  }
}

class CompareEdge implements Comparator<Edge> {

  // compares edges by weight 
  public int compare(Edge o1, Edge o2) {
    return o1.weight - o2.weight;
  }
}


//examples and tests
class ExamplesLightEmAll {

  LightEmAll world1;
  LightEmAll world2;
  LightEmAll emptyWorld;
  LightEmAll world3;
  LightEmAll world4;
  LightEmAll world5;

  GamePiece piece00;
  GamePiece piece01;
  GamePiece piece02;
  GamePiece piece03;
  GamePiece piece04;
  GamePiece piece05;
  GamePiece piece06;
  GamePiece piece07;
  GamePiece piece10;
  GamePiece piece11;
  GamePiece piece12;
  GamePiece piece13;
  GamePiece piece14;
  GamePiece piece15;
  GamePiece piece16;
  GamePiece piece17;
  GamePiece piece20;
  GamePiece piece21;
  GamePiece piece22;
  GamePiece piece23;
  GamePiece piece24;
  GamePiece piece25;
  GamePiece piece26;
  GamePiece piece27;
  GamePiece piece30;
  GamePiece piece31;
  GamePiece piece32;
  GamePiece piece33;
  GamePiece piece34;
  GamePiece piece35;
  GamePiece piece36;
  GamePiece piece37;
  GamePiece piece40;
  GamePiece piece41;
  GamePiece piece42;
  GamePiece piece43;
  GamePiece piece44;
  GamePiece piece45;
  GamePiece piece46;
  GamePiece piece47;
  GamePiece piece50;
  GamePiece piece51;
  GamePiece piece52;
  GamePiece piece53;
  GamePiece piece54;
  GamePiece piece55;
  GamePiece piece56;
  GamePiece piece57;
  GamePiece piece60;
  GamePiece piece61;
  GamePiece piece62;
  GamePiece piece63;
  GamePiece piece64;
  GamePiece piece65;
  GamePiece piece66;
  GamePiece piece67;
  GamePiece piece70;
  GamePiece piece71;
  GamePiece piece72;
  GamePiece piece73;
  GamePiece piece74;
  GamePiece piece75;
  GamePiece piece76;
  GamePiece piece77;
  GamePiece piece80;
  GamePiece piece81;
  GamePiece piece82;
  GamePiece piece83;
  GamePiece piece84;
  GamePiece piece85;
  GamePiece piece86;
  GamePiece piece87;

  GamePiece piece200;
  GamePiece piece201;
  GamePiece piece210;
  GamePiece piece211;

  Edge edge13;
  Edge edge24;
  Edge edge31;
  Edge edge42;
  Edge edge34;
  Edge edge43;
  ArrayList<Edge> edges1;


  GamePiece p300;
  GamePiece p301;
  GamePiece p302;
  GamePiece p310;
  GamePiece p311;
  GamePiece p312;
  GamePiece p320;
  GamePiece p321;
  GamePiece p322;

  ArrayList<ArrayList<GamePiece>> gamePieces;
  ArrayList<ArrayList<GamePiece>> gamePieces2;
  ArrayList<ArrayList<GamePiece>> gamePieces3;

  HashMap<GamePiece, GamePiece> representatives;

  void setup() {

    this.representatives = new HashMap<GamePiece, GamePiece>();

    this.world1 = new LightEmAll(400, 450, 50, new Random(5), false);
    this.world2 = new LightEmAll(100, 100, 50, new Random(5), false);
    this.emptyWorld = new LightEmAll(0, 0, 50, new Random(5), false);
    this.world3 = new LightEmAll(150, 150, 50, new Random(5), false);
    this.world4 = new LightEmAll(100, 100, 50, new Random(5));
    this.world5 = new LightEmAll(100, 100, 50, new Random(8));


    this.piece200 = new GamePiece(0, 0, false, false, true, false, false, false);
    this.piece201 = new GamePiece(0, 1, false, true, true, true, false, false);
    this.piece210 = new GamePiece(1, 0, false, false, false, true, false, true);
    this.piece211 = new GamePiece(1, 1, false, true, true, true, true, true);

    this.gamePieces2 = new ArrayList<>(Arrays.asList(
        new ArrayList<GamePiece>(Arrays.asList(this.piece200, this.piece201)),
        new ArrayList<GamePiece>(Arrays.asList(this.piece210, this.piece211))));

    // check order - bottom, top, left, right
    this.edge13 = new Edge(this.piece200, this.piece201, 1);
    this.edge24 = new Edge(this.piece201, this.piece210, 1);
    this.edge31 = new Edge(this.piece201, this.piece211, 1);
    this.edge34 = new Edge(this.piece210, this.piece211, 1);
    this.edge42 = new Edge(this.piece211, this.piece201, 1);
    this.edge43 = new Edge(this.piece211, this.piece210, 1);

    this.piece200.outEdges = new ArrayList<Edge>(Arrays.asList(this.edge13));
    this.piece201.outEdges = new ArrayList<Edge>(Arrays.asList(this.edge24, this.edge31,
        this.edge42));
    this.piece210.outEdges = new ArrayList<Edge>(Arrays.asList(this.edge34, this.edge43));
    this.piece211.outEdges = new ArrayList<Edge>(Arrays.asList());

    this.edges1 = new ArrayList<>(Arrays.asList(
        this.edge34, this.edge43));

    this.world2.board = this.gamePieces2;

    this.piece00 = new GamePiece(0, 0, false, false, false, true, false, false);
    this.piece01 = new GamePiece(0, 1, false, false, false, true, false, false);
    this.piece02 = new GamePiece(0, 2, false, false, false, true, false, false);
    this.piece03 = new GamePiece(0, 3, false, false, false, true, false, false);
    this.piece04 = new GamePiece(0, 4, false, false, false, true, false, false);
    this.piece05 = new GamePiece(0, 5, false, false, false, true, false, false);
    this.piece06 = new GamePiece(0, 6, false, false, false, true, false, false);
    this.piece07 = new GamePiece(0, 7, false, false, false, true, false, false);
    this.piece10 = new GamePiece(1, 0, false, false, true, true, false, false);
    this.piece11 = new GamePiece(1, 1, false, false, true, true, false, false);
    this.piece12 = new GamePiece(1, 2, false, false, true, true, false, false);
    this.piece13 = new GamePiece(1, 3, false, false, true, true, false, false);
    this.piece14 = new GamePiece(1, 4, false, false, true, true, false, false);
    this.piece15 = new GamePiece(1, 5, false, false, true, true, false, false);
    this.piece16 = new GamePiece(1, 6, false, false, true, true, false, false);
    this.piece17 = new GamePiece(1, 7, false, false, true, true, false, false);
    this.piece20 = new GamePiece(2, 0, false, false, true, true, false, false);
    this.piece21 = new GamePiece(2, 1, false, false, true, true, false, false);
    this.piece22 = new GamePiece(2, 2, false, false, true, true, false, false);
    this.piece23 = new GamePiece(2, 3, false, false, true, true, false, false);
    this.piece24 = new GamePiece(2, 4, false, false, true, true, false, false);
    this.piece25 = new GamePiece(2, 5, false, false, true, true, false, false);
    this.piece26 = new GamePiece(2, 6, false, false, true, true, false, false);
    this.piece27 = new GamePiece(2, 7, false, false, true, true, false, false);
    this.piece30 = new GamePiece(3, 0, false, false, true, true, false, false);
    this.piece31 = new GamePiece(3, 1, false, false, true, true, false, false);
    this.piece32 = new GamePiece(3, 2, false, false, true, true, false, false);
    this.piece33 = new GamePiece(3, 3, false, false, true, true, false, false);
    this.piece34 = new GamePiece(3, 4, false, false, true, true, false, false);
    this.piece35 = new GamePiece(3, 5, false, false, true, true, false, false);
    this.piece36 = new GamePiece(3, 6, false, false, true, true, false, false);
    this.piece37 = new GamePiece(3, 7, false, false, true, true, false, false);
    this.piece40 = new GamePiece(4, 0, false, true, true, true, false, false);
    this.piece41 = new GamePiece(4, 1, true, true, true, true, false, false);
    this.piece42 = new GamePiece(4, 2, false, true, true, true, false, false);
    this.piece43 = new GamePiece(4, 3, false, true, true, true, false, false);
    this.piece44 = new GamePiece(4, 4, false, true, true, true, true, true);
    this.piece45 = new GamePiece(4, 5, false, true, true, true, false, false);
    this.piece46 = new GamePiece(4, 6, false, true, true, true, false, false);
    this.piece47 = new GamePiece(4, 7, true, false, true, true, false, false);
    this.piece50 = new GamePiece(5, 0, false, false, true, true, false, false);
    this.piece51 = new GamePiece(5, 1, false, false, true, true, false, false);
    this.piece52 = new GamePiece(5, 2, false, false, true, true, false, false);
    this.piece53 = new GamePiece(5, 3, false, false, true, true, false, false);
    this.piece54 = new GamePiece(5, 4, false, false, true, true, false, false);
    this.piece55 = new GamePiece(5, 5, false, false, true, true, false, false);
    this.piece56 = new GamePiece(5, 6, false, false, true, true, false, false);
    this.piece57 = new GamePiece(5, 7, false, false, true, true, false, false);
    this.piece60 = new GamePiece(6, 0, false, false, true, true, false, false);
    this.piece61 = new GamePiece(6, 1, false, false, true, true, false, false);
    this.piece62 = new GamePiece(6, 2, false, false, true, true, false, false);
    this.piece63 = new GamePiece(6, 3, false, false, true, true, false, false);
    this.piece64 = new GamePiece(6, 4, false, false, true, true, false, false);
    this.piece65 = new GamePiece(6, 5, false, false, true, true, false, false);
    this.piece66 = new GamePiece(6, 6, false, false, true, true, false, false);
    this.piece67 = new GamePiece(6, 7, false, false, true, true, false, false);
    this.piece70 = new GamePiece(7, 0, false, false, true, true, false, false);
    this.piece71 =
        new GamePiece(7, 1, false, false, true, true, false, false);
    this.piece72 = new GamePiece(7, 2, false, false, true, true, false, false);
    this.piece73 = new GamePiece(7, 3, false, false, true, true, false, false);
    this.piece74 = new GamePiece(7, 4, false, false, true, true, false, false);
    this.piece75 = new GamePiece(7, 5, false, false, true, true, false, false);
    this.piece76 = new GamePiece(7, 6, false, false, true, true, false, false);
    this.piece77 = new GamePiece(7, 7, false, false, true, true, false, false);
    this.piece81 = new GamePiece(8, 1, false, false, true, false, false, false);
    this.piece82 = new GamePiece(8, 2, false, false, true, false, false, false);
    this.piece83 = new GamePiece(8, 3, false, false, true, false, false, false);
    this.piece84 = new GamePiece(8, 4, false, false, true, false, false, false);
    this.piece85 = new GamePiece(8, 5, false, false, true, false, false, false);
    this.piece86 = new GamePiece(8, 6, false, false, true, false, false, false);
    this.piece87 = new GamePiece(8, 7, false, false, true, false, false, false);



    gamePieces = new ArrayList<>();


    this.gamePieces = new ArrayList<>();

    this.gamePieces.add(new ArrayList<>(Arrays.asList(
        this.piece00, this.piece10, this.piece20, this.piece30, this.piece40, 
        this.piece50, this.piece60, this.piece70, this.piece81)));

    this.gamePieces.add(new ArrayList<>(Arrays.asList(
        this.piece01, this.piece11, this.piece21, this.piece31, this.piece41, 
        this.piece51, this.piece61, this.piece71, this.piece82)));

    this.gamePieces.add(new ArrayList<>(Arrays.asList(
        this.piece02, this.piece12, this.piece22, this.piece32, this.piece42, 
        this.piece52, this.piece62, this.piece72, this.piece83)));

    this.gamePieces.add(new ArrayList<>(Arrays.asList(
        this.piece03, this.piece13, this.piece23, this.piece33, this.piece43, 
        this.piece53, this.piece63, this.piece73, this.piece84)));

    this.gamePieces.add(new ArrayList<>(Arrays.asList(
        this.piece04, this.piece14, this.piece24, this.piece34, this.piece44, 
        this.piece54, this.piece64, this.piece74, this.piece85)));

    this.gamePieces.add(new ArrayList<>(Arrays.asList(
        this.piece05, this.piece15, this.piece25, this.piece35, this.piece45, 
        this.piece55, this.piece65, this.piece75, this.piece86)));

    this.gamePieces.add(new ArrayList<>(Arrays.asList(
        this.piece06, this.piece16, this.piece26, this.piece36, this.piece46, 
        this.piece56, this.piece66, this.piece76, this.piece86)));

    this.gamePieces.add(new ArrayList<>(Arrays.asList(
        this.piece07, this.piece17, this.piece27, this.piece37, this.piece47, 
        this.piece57, this.piece67, this.piece77, this.piece87)));


    this.p300 = new GamePiece(0, 0, false, false, false, true, false, false);
    this.p301 = new GamePiece(0, 1, false, true, true, true, false, false);
    this.p302 = new GamePiece(0, 2, false, false, true, false, false, false);
    this.p310 = new GamePiece(1, 0, false, false, false, true, false, false);
    this.p311 = new GamePiece(1, 1, true, true, true, true, true, true);
    this.p312 = new GamePiece(1, 2, false, false, true, false, false, false);
    this.p320 = new GamePiece(2, 0, false, false, false, true, false, false);
    this.p321 = new GamePiece(2, 1, true, false, true, true, false, false);
    this.p322 = new GamePiece(2, 2, false, false, true, false, false, false);

    this.gamePieces3 = new ArrayList<>(Arrays.asList(
        new ArrayList<GamePiece>(Arrays.asList(this.p300, this.p301, this.p302)),
        new ArrayList<GamePiece>(Arrays.asList(this.p310, this.p311, this.p312)),
        new ArrayList<GamePiece>(Arrays.asList(this.p320, this.p321, this.p322))));

  }

  // test the create MST method
  void testCreateMST(Tester t) {
    setup();
    this.world4.createConnectedBoard();
    this.world4.powerCol = 0;
    this.world4.powerRow = 0;
    this.world4.setEdges();
    this.world4.createMST();
    this.world4.setTilesPowered();
    // test on a randomly generated world 
    t.checkExpect(this.world4.mst, new ArrayList<Edge>(Arrays.asList(
        new Edge(new GamePiece(1, 0, true, false, false, true, false, false),
            new GamePiece(0, 0, false, false, true, false, false, true), 60),
        new Edge(new GamePiece(1, 0, true, false, false, true, false, false),
            new GamePiece(1, 1, false, true, false, true, false, false), 60),
        new Edge(new GamePiece(1, 1, false, true, false, true, false, false),
            new GamePiece(0, 1, false, false, true, false, false, false), 90))));
    // test on a second randomly generated world 
    this.world5.createConnectedBoard();
    this.world5.powerCol = 0;
    this.world5.powerRow = 0;
    this.world5.setEdges();
    this.world5.createMST();
    this.world5.setTilesPowered();
    t.checkExpect(this.world5.mst, new ArrayList<Edge>(Arrays.asList(
        new Edge(new GamePiece(0, 1, false, true, true, false, false, false),
            new GamePiece(1, 1, false, false, false, true, false, false), 328),
        new Edge(new GamePiece(0, 1, false, true, true, false, false, false),
            new GamePiece(0, 0, true, false, true, false, false, true), 328),
        new Edge(new GamePiece(1, 0, false, false, false, true, false, false),
            new GamePiece(0, 0, true, false, true, false, false, true), 357))));
  }

  // test the find method 
  void testFind(Tester t) {
    setup();
    this.representatives.put(this.piece00, this.piece00);
    this.representatives.put(this.piece01, this.piece01);
    this.representatives.put(this.piece02, this.piece02);
    this.representatives.put(this.piece03, this.piece03);
    // test finding piece 00
    t.checkExpect(this.world1.find(this.representatives, this.piece00), this.piece00);
    // test finding piece 00 after unioning it to 01
    this.world1.union(this.representatives, this.piece00, this.piece01);
    t.checkExpect(this.world1.find(this.representatives, this.piece00), this.piece01);
  }

  // test the union method 
  void testUnion(Tester t) {
    setup();
    this.representatives.put(this.piece00, this.piece00);
    this.representatives.put(this.piece01, this.piece01);
    this.representatives.put(this.piece02, this.piece02);
    this.representatives.put(this.piece03, this.piece03);
    // test unioning two game piecs
    this.world1.union(this.representatives, piece01, piece00);
    t.checkExpect(this.representatives.get(this.piece01), this.piece00);
    // test if the pieces are already combined (ie it would create a cycle)
    this.world1.union(this.representatives, this.piece02, this.piece03);
    t.checkExpect(this.representatives.get(this.piece02), this.piece03);
  }

  // test teh set piece direction method
  void testSetPieceDirection(Tester t) {
    setup();
    // test before any changes
    t.checkExpect(this.edge13.from.left, false);
    t.checkExpect(this.edge13.from.right, true);
    t.checkExpect(this.edge13.from.top, true);
    t.checkExpect(this.edge13.from.bottom, true);
    t.checkExpect(this.edge13.to.left, false);
    t.checkExpect(this.edge13.to.right, false);
    t.checkExpect(this.edge13.to.top, true);
    t.checkExpect(this.edge13.to.bottom, false);
    // test changing from left 
    this.edge13.setPieceDirection("from", "left", true);
    t.checkExpect(this.edge13.from.left, true);
    // test changing from right
    this.edge13.setPieceDirection("from", "right", false);
    t.checkExpect(this.edge13.from.right, false);
    // test changing from top
    this.edge13.setPieceDirection("from", "top", false);
    t.checkExpect(this.edge13.from.top, false);
    // test changing from bottom
    this.edge13.setPieceDirection("from", "bottom", false);
    t.checkExpect(this.edge13.from.bottom, false);
    // test changing to left 
    this.edge13.setPieceDirection("to", "left", true);
    t.checkExpect(this.edge13.to.left, true);
    // test changing to right
    this.edge13.setPieceDirection("to", "right", true);
    t.checkExpect(this.edge13.to.right, true);
    // test changing to top
    this.edge13.setPieceDirection("to", "top", false);
    t.checkExpect(this.edge13.to.top, false);
    // test changing to bottom
    this.edge13.setPieceDirection("to", "bottom", true);
    t.checkExpect(this.edge13.to.bottom, true);
  }

  // tests the is game over method 
  void testIsGameOver(Tester t) {
    setup();
    // test a game that is not over
    t.checkExpect(this.world1.isGameOver(), false);
    // test on a game that is over
    // power everything 
    for (GamePiece node : this.world1.nodes) {
      node.setPowered(true);
    }
    t.checkExpect(this.world1.isGameOver(), true);
  }

  // test the compare game piece cols method 
  void testCompareGamePieceRows(Tester t) {
    setup();
    // test when they are in the same row
    t.checkExpect(this.piece200.compareGamePieceRows(p300), 0);
    // test when the first is in the row prior
    t.checkExpect(this.piece200.compareGamePieceRows(p310), -1);
    // test when the first is in the row after
    t.checkExpect(this.piece210.compareGamePieceRows(p301), 1);
  }

  // test compare game piece rows
  void testCompareGamePieceCols(Tester t) {
    setup();
    // test when they are in the same col 
    t.checkExpect(this.piece200.compareGamePieceCols(this.p300), 0);
    // test when the first game piece is in the prior col 
    t.checkExpect(this.piece200.compareGamePieceCols(this.p301), -1);
    // test when the first game piece is in the next col 
    t.checkExpect(this.piece201.compareGamePieceCols(this.p300), 1);
  }

  // test the compare cols method 
  void testCompareCols(Tester t) {
    setup();
    // test when to is in the column before from
    t.checkExpect(this.edge13.compareCols(), -1);
    // test when they are in the same column
    t.checkExpect(this.edge31.compareCols(), 0);
    // test when to is in the column after from
    t.checkExpect(this.edge24.compareCols(), 1);
  }

  // test the compare rows method 
  void testCompareRows(Tester t) {
    setup();
    // test when the edges are in the same row 
    t.checkExpect(this.edge13.compareRows(), 0);
    // test when to is after
    t.checkExpect(this.edge42.compareRows(), 1);
    // test when to is before 
    t.checkExpect(this.edge24.compareRows(), -1);
  }

  // test the compare edge function object 
  void testCompareEdge(Tester t) {
    setup();
    // test edges that are equal in weight
    t.checkExpect(new CompareEdge().compare(this.edge13, this.edge24), 0);
    // test edges where the first has a greater weight
    t.checkExpect(new CompareEdge().compare(new Edge(this.piece00, this.piece01, 5),
        new Edge(this.piece02, this.piece03, 1)), 4);
    // test edges where the second has a greater weight
    t.checkExpect(new CompareEdge().compare(new Edge(this.piece00, this.piece01, 10),
        new Edge(this.piece02, this.piece03, 40)), -30);
  }

  // test set top
  void testSetTop(Tester t) {
    setup();
    // test before changing 
    t.checkExpect(this.piece200.top, true);
    // test changing to false 
    this.piece200.setTop(false);
    t.checkExpect(this.piece200.top, false);
    // test changing to true 
    this.piece200.setTop(true);
    t.checkExpect(this.piece200.top, true);
  }

  // test set bottom 
  void testSetBottom(Tester t) {
    setup();
    // test before changing 
    t.checkExpect(this.piece200.bottom, false);
    // test changing to false 
    this.piece200.setBottom(true);
    t.checkExpect(this.piece200.bottom, true);
    // test changing to true 
    this.piece200.setBottom(false);
    t.checkExpect(this.piece200.bottom, false);
  }

  // test set left 
  void testSetLeft(Tester t) {
    setup();
    // test before changing 
    t.checkExpect(this.piece200.left, false);
    // test changing to false 
    this.piece200.setLeft(true);
    t.checkExpect(this.piece200.left, true);
    // test changing to true 
    this.piece200.setLeft(false);
    t.checkExpect(this.piece200.left, false);
  }

  // test set left 
  void testSetRight(Tester t) {
    setup();
    // test before changing 
    t.checkExpect(this.piece200.right, false);
    // test changing to false 
    this.piece200.setRight(true);
    t.checkExpect(this.piece200.right, true);
    // test changing to true 
    this.piece200.setRight(false);
    t.checkExpect(this.piece200.right, false);
  }

  // test the create connected board method 
  void testCreateConnectedBoard(Tester t) {
    setup();
    // test on an empty world 
    this.emptyWorld.createConnectedBoard();
    t.checkExpect(this.emptyWorld.board, new ArrayList<ArrayList<GamePiece>>());
    // test on a non-empty board
    this.world3.createConnectedBoard();
    // now test that a piece in the left corner has left, right, top, bottom set to true
    t.checkExpect(this.world3.board.get(0).get(0).left, true);
    t.checkExpect(this.world3.board.get(0).get(0).right, true);
    t.checkExpect(this.world3.board.get(0).get(0).top, true);
    t.checkExpect(this.world3.board.get(0).get(0).bottom, true);
    // now test that a piece in the middle  has left, right, top, bottom set to true
    t.checkExpect(this.world3.board.get(1).get(1).left, true);
    t.checkExpect(this.world3.board.get(1).get(1).right, true);
    t.checkExpect(this.world3.board.get(1).get(1).top, true);
    t.checkExpect(this.world3.board.get(1).get(1).bottom, true);
  }

  // test the create board method 
  void testCreateBoard(Tester t) {
    setup();
    // set the board to be empty 
    this.world3.board = new ArrayList<ArrayList<GamePiece>>();
    // test setting world 2's board 
    this.world3.createBoard();
    t.checkExpect(this.world3.board, this.gamePieces3);
  }

  // tests the method onTick
  void testOnTick(Tester t) {
    setup();
    // tests when game is running
    this.world1.won = 0;
    this.world1.onTick();
    t.checkExpect(this.world1.time, 1);
    // tests when game is not running
    this.world2.won = 1;
    this.world2.onTick();
    t.checkExpect(this.world2.time, 0);
  }

  // test the set edges method 
  void testSetEdges(Tester t) {
    setup();
    // test on an empty world
    t.checkExpect(this.emptyWorld.mst, new ArrayList<Edge>());
    // test on a non-empty world 
    this.world2.createBoard();
    this.world2.setEdges();
    this.world2.setAllOuterEdges();
    this.world2.setTilesPowered();
    t.checkExpect(this.world2.mst, this.edges1);
  }

  // test the on mouse clicked method 
  void testOnMouseClicked(Tester t) {
    setup();
    LightEmAll worldBefore = new LightEmAll(400, 450, 50, new Random(5), false);
    // test the piece's initial conditions 
    t.checkExpect(this.world1.board.get(0).get(0), worldBefore.board.get(0).get(0));
    worldBefore.board.get(0).get(0).rotate();
    worldBefore.setEdges();
    worldBefore.setAllOuterEdges();
    worldBefore.setTilesPowered();
    this.world1.onMouseClicked(new Posn(0, 0));
    // test after the piece has been clicked in the top left corner
    t.checkExpect(this.world1.board.get(0).get(0), worldBefore.board.get(0).get(0));
    // test clicking on a piece in the middle of the board 
    t.checkExpect(this.world1.board.get(3).get(5), worldBefore.board.get(3).get(5));
    this.world1.onMouseClicked(new Posn(157, 264));
    worldBefore.board.get(3).get(5).rotate();
    worldBefore.setEdges();
    worldBefore.setAllOuterEdges();
    worldBefore.setTilesPowered();
    // test after the piece has been clicked 
    t.checkExpect(this.world1.board.get(3).get(5), worldBefore.board.get(3).get(5));
  }

  // test the set is a power station method 
  void testSetIsAPowerStation(Tester t) {
    setup();
    // check that a tile is not a power station
    t.checkExpect(this.world1.board.get(0).get(0).powerStation, false);
    // set it to a power station
    this.world1.board.get(0).get(0).setIsPowerStation(true);
    // check that a tile is  a power station
    t.checkExpect(this.world1.board.get(0).get(0).powerStation, true);
    // set it to not a power station
    this.world1.board.get(0).get(0).setIsPowerStation(false);
    // check that it is not a power station
    t.checkExpect(this.world1.board.get(0).get(0).powerStation, false);
  }

  // test the isPowerStation method 
  void testIsPowerStation(Tester t) {
    setup();
    // check that a cell that is a power station returns true 
    t.checkExpect(this.piece44.isPowerStation(), true);
    // check that a cell that is not a power station returns false
    t.checkExpect(this.piece00.isPowerStation(), false);
  }

  // test the on key event method 
  void testOnKeyEvent(Tester t) {
    setup();
    // test a key that does nothing 
    LightEmAll worldBefore = new LightEmAll(400, 450, 50, new Random(5), false);
    this.world1.onKeyEvent("h");
    t.checkExpect(worldBefore, world1);
    // test the right arrow key (done twice to align the power station for up and down)
    worldBefore.board.get(worldBefore.powerCol).get(worldBefore.powerRow).setIsPowerStation(false);
    worldBefore.powerCol++;
    worldBefore.board.get(worldBefore.powerCol).get(worldBefore.powerRow).setIsPowerStation(true);
    worldBefore.setEdges();
    worldBefore.setAllOuterEdges();
    worldBefore.setTilesPowered();
    this.world1.onKeyEvent("right");
    t.checkExpect(worldBefore, world1);
    // test the down arrow key 
    worldBefore.board.get(worldBefore.powerCol).get(worldBefore.powerRow).setIsPowerStation(false);
    worldBefore.powerRow++;
    worldBefore.board.get(worldBefore.powerCol).get(worldBefore.powerRow).setIsPowerStation(true);
    worldBefore.setEdges();
    worldBefore.setAllOuterEdges();
    worldBefore.setTilesPowered();
    this.world1.onKeyEvent("down");
    t.checkExpect(worldBefore, world1);
    // test the up arrow key 
    worldBefore.board.get(worldBefore.powerCol).get(worldBefore.powerRow).setIsPowerStation(false);
    worldBefore.powerRow--;
    worldBefore.board.get(worldBefore.powerCol).get(worldBefore.powerRow).setIsPowerStation(true);
    worldBefore.setEdges();
    worldBefore.setAllOuterEdges();
    worldBefore.setTilesPowered();
    this.world1.onKeyEvent("up");
    t.checkExpect(worldBefore, world1);
    // test down to make sure nothing happens since it will not be aligned with a connection
    this.world1.onKeyEvent("up");
    t.checkExpect(worldBefore, world1);
    // test the left arrow key 
    worldBefore.board.get(worldBefore.powerCol).get(worldBefore.powerRow).setIsPowerStation(false);
    worldBefore.powerCol--;
    worldBefore.board.get(worldBefore.powerCol).get(worldBefore.powerRow).setIsPowerStation(true);
    worldBefore.setEdges();
    worldBefore.setAllOuterEdges();
    worldBefore.setTilesPowered();
    this.world1.onKeyEvent("left");
    t.checkExpect(worldBefore, world1);

  }

  // run big bang
  void testBigBang(Tester t) {
    setup();
    new LightEmAll(400, 450, 50, new Random()).bigBang(400, 500, 1);
  }

  // tests hasLeft
  void testHasLeft(Tester t) {
    setup();
    t.checkExpect(this.piece00.hasLeft(), false);
    t.checkExpect(this.piece01.hasLeft(), false);
    t.checkExpect(this.piece47.hasLeft(), true);
  }

  // tests hasRight
  void testHasRight(Tester t) {
    setup();
    t.checkExpect(this.piece00.hasRight(), false);
    t.checkExpect(this.piece01.hasRight(), false);
    t.checkExpect(this.piece41.hasRight(), true);
  }

  // tests hasBottom
  void testHasBottom(Tester t) {
    setup();
    t.checkExpect(this.piece00.hasBottom(), true);
    t.checkExpect(this.piece87.hasBottom(), false);
    t.checkExpect(this.piece41.hasBottom(), true);
  }

  // tests hasTop
  void testHasTop(Tester t) {
    setup();
    t.checkExpect(this.piece00.hasTop(), false);
    t.checkExpect(this.piece87.hasTop(), true);
    t.checkExpect(this.piece41.hasTop(), true);
  }

  // tests rotate 
  void testRotate(Tester t) {
    setup();
    // tests a cross piece
    this.piece41.rotate();
    t.checkExpect(this.piece41.left, true);
    t.checkExpect(this.piece41.right, true);
    t.checkExpect(this.piece41.top, true);
    t.checkExpect(this.piece41.bottom, true);
    // test a piece that is not a cross piece
    this.piece00.rotate();
    t.checkExpect(this.piece00.left, true);
    t.checkExpect(this.piece00.right, false);
    t.checkExpect(this.piece00.top, false);
    t.checkExpect(this.piece00.bottom, false);
    // tests a piece that has a powerStation
    this.piece44.rotate();
    t.checkExpect(this.piece44.left, true);
    t.checkExpect(this.piece44.right, true);
    t.checkExpect(this.piece44.top, false);
    t.checkExpect(this.piece44.bottom, true);

  }

  //tests makeScene
  void testMakeScene(Tester t) {
    setup();
    // tests an empty game
    WorldScene finalEmpty = new WorldScene(0, 0);
    t.checkExpect(this.emptyWorld.makeScene(), finalEmpty);

    // tests a non-empty game with power station without end screen

    this.world1.won = 0;

    WorldScene finalWorld = new WorldScene(100, 100);
    WorldImage outline = new RectangleImage(50, 50, OutlineMode.OUTLINE, Color.black);
    WorldImage tile = new OverlayImage(
        new RectangleImage(15, 15, OutlineMode.SOLID, Color.BLACK),
        new RectangleImage(50, 50, OutlineMode.SOLID, Color.DARK_GRAY));

    // first piece (up only, piece200)
    finalWorld.placeImageXY(new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, 
        new RectangleImage(15, 25, OutlineMode.SOLID, Color.BLACK), 0, 0,
        tile),
        25, 25);
    finalWorld.placeImageXY(outline, 25, 25);

    // second piece (up, right, bottom, piece210)
    finalWorld.placeImageXY(
        new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, 
            new RectangleImage(15, 25, OutlineMode.SOLID, Color.BLACK), 0, 0,
            new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
                new RectangleImage(25, 15, OutlineMode.SOLID, Color.BLACK), 0, 0,
                new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
                    new RectangleImage(15, 25, OutlineMode.SOLID, Color.BLACK), 0, 0,
                    tile))),
        25, 75);
    finalWorld.placeImageXY(outline, 25, 75);

    // third piece (down only, piece201)
    finalWorld.placeImageXY(
        new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, 
            new RectangleImage(15, 25, OutlineMode.SOLID, new Color(255, 230, 0)), 0, 0,
            new OverlayImage(
                new RectangleImage(15, 15, OutlineMode.SOLID, new Color(255, 230, 0)),
                new RectangleImage(50, 50, OutlineMode.SOLID, Color.DARK_GRAY))),
        75, 25);

    // fourth piece (up, right, bottom, piece211) with power station drawn
    finalWorld.placeImageXY(
        new RectangleImage(50, 50, OutlineMode.OUTLINE, Color.BLACK), 75, 25);
    finalWorld.placeImageXY(
        new OverlayImage(
            new OverlayImage(
                new StarImage(16, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
                new StarImage(16, 7, OutlineMode.SOLID, new Color(0, 255, 255))),
            new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
                new RectangleImage(15, 25, OutlineMode.SOLID, new Color(255, 255, 0)), 0, 0,
                new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
                    new RectangleImage(25, 15, OutlineMode.SOLID, new Color(255, 255, 0)), 0, 0,
                    new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, 
                        new RectangleImage(15 ,25, OutlineMode.SOLID, new Color(255, 255, 0)), 0, 0,
                        new OverlayImage(
                            new RectangleImage(15, 15, OutlineMode.SOLID, new Color(255, 255, 0)),
                            new RectangleImage(50, 50, OutlineMode.SOLID, Color.DARK_GRAY)))))), 
        75, 75);
    finalWorld.placeImageXY(outline, 75, 75);

    t.checkExpect(this.world2.makeScene(), finalWorld);

    // testing ending scene

    this.world1.won = 1;

    WorldScene finalWorldEnd = new WorldScene(100, 100);
    WorldImage outlineEnd = new RectangleImage(50, 50, OutlineMode.OUTLINE, Color.black);
    WorldImage tileEnd = new OverlayImage(
        new RectangleImage(15, 15, OutlineMode.SOLID, Color.BLACK),
        new RectangleImage(50, 50, OutlineMode.SOLID, Color.DARK_GRAY));

    // first piece (up only, piece200)
    finalWorldEnd.placeImageXY(new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, 
        new RectangleImage(15, 25, OutlineMode.SOLID, Color.BLACK), 0, 0,
        tile),
        25, 25);
    finalWorldEnd.placeImageXY(outline, 25, 25);

    // second piece (up, right, bottom, piece210)
    finalWorldEnd.placeImageXY(
        new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, 
            new RectangleImage(15, 25, OutlineMode.SOLID, Color.BLACK), 0, 0,
            new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
                new RectangleImage(25, 15, OutlineMode.SOLID, Color.BLACK), 0, 0,
                new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
                    new RectangleImage(15, 25, OutlineMode.SOLID, Color.BLACK), 0, 0,
                    tileEnd))),
        25, 75);
    finalWorldEnd.placeImageXY(outlineEnd, 25, 75);

    // third piece (down only, piece201)
    finalWorldEnd.placeImageXY(
        new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, 
            new RectangleImage(15, 25, OutlineMode.SOLID, new Color(255, 230, 0)), 0, 0,
            new OverlayImage(
                new RectangleImage(15, 15, OutlineMode.SOLID, new Color(255, 230, 0)),
                new RectangleImage(50, 50, OutlineMode.SOLID, Color.DARK_GRAY))),
        75, 25);

    // fourth piece (up, right, bottom, piece211) with power station drawn
    finalWorldEnd.placeImageXY(
        new RectangleImage(50, 50, OutlineMode.OUTLINE, Color.BLACK), 75, 25);
    finalWorldEnd.placeImageXY(
        new OverlayImage(
            new OverlayImage(
                new StarImage(16, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
                new StarImage(16, 7, OutlineMode.SOLID, new Color(0, 255, 255))),
            new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
                new RectangleImage(15, 25, OutlineMode.SOLID, new Color(255, 255, 0)), 0, 0,
                new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
                    new RectangleImage(25, 15, OutlineMode.SOLID, new Color(255, 255, 0)), 0, 0,
                    new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, 
                        new RectangleImage(15 ,25, OutlineMode.SOLID, new Color(255, 255, 0)), 0, 0,
                        new OverlayImage(
                            new RectangleImage(15, 15, OutlineMode.SOLID, new Color(255, 255, 0)),
                            new RectangleImage(50, 50, OutlineMode.SOLID, Color.DARK_GRAY)))))), 
        75, 75);
    finalWorldEnd.placeImageXY(outlineEnd, 75, 75);

    finalWorldEnd.placeImageXY(new RectangleImage(200, 100, OutlineMode.SOLID, Color.LIGHT_GRAY),
        200, 200);
    finalWorldEnd.placeImageXY(new RectangleImage(200, 100, OutlineMode.OUTLINE, Color.ORANGE),
        200, 200);
    finalWorldEnd.placeImageXY(new TextImage("You win!!", 35, FontStyle.BOLD, Color.BLACK),
        203, 200);
    finalWorldEnd.placeImageXY(new TextImage("You win!!", 35, FontStyle.BOLD, Color.ORANGE),
        200, 200);

    t.checkExpect(this.world2.makeScene(), finalWorldEnd);


  }



  // tests the is powered method 
  void testIsPowered(Tester t) {
    setup();
    // test on a not powered tile
    t.checkExpect(this.piece00.isPowered(), false);
    // test on a powered cell
    this.piece00.setPowered(true);
    t.checkExpect(this.piece00.isPowered(), true);
  }

  // tests the game piece equals method 
  void testGamePieceEquals(Tester t) {
    setup();
    // test on identical game pieces
    t.checkExpect(this.piece00.gamePieceEquals(this.piece00), true);
    // test on game pieces that are the same, but not exactly identical
    GamePiece testPiece = new GamePiece(0, 0, false, false, false, false, false, false);
    GamePiece testPiece2 = new GamePiece(0, 0, false, false, false, false, false, false);
    t.checkExpect(testPiece.gamePieceEquals(testPiece2), true);
    // test on different game pieces
    t.checkExpect(this.piece00.gamePieceEquals(this.piece01), false);
  }

  // test the method fromEquals
  void testFromEquals(Tester t) {
    setup();

    // test on an edge whos from value is identical to the given gamepiece
    t.checkExpect(this.edge13.fromEquals(this.piece201), true);
    // test on an edge whos to value is identical to the given gamepiece
    t.checkExpect(this.edge24.fromEquals(this.piece201), false);
    // test on an edge whos to or from value is different than the given gamepiece
    t.checkExpect(this.edge24.fromEquals(this.piece211), false);
  }

  // test the set powered method 
  void testSetPowered(Tester t) {
    setup();
    // first confirm that a piece is not powered, then set it to powered, then 
    // confirm that it is now powered
    t.checkExpect(this.piece00.powered, false);
    this.piece00.setPowered(true);
    t.checkExpect(this.piece00.powered, true);
    // test setting a piece to not powered
    this.piece00.setPowered(false);
    t.checkExpect(this.piece00.powered, false);
  }

  // test the randomize edges method 
  void testRandomizeEdges(Tester t) {
    setup();
    // test on an empty world 
    this.emptyWorld.randomizeEdges();
    t.checkExpect(this.emptyWorld.board, new ArrayList<ArrayList<GamePiece>>());
    // test on a small world 
    t.checkExpect(this.world2.board, this.gamePieces2);
  }

  // test the set tiles powered method 
  void testSetTilesPowered(Tester t) {
    setup();
    // set all tiles to not powered
    for (int col = 0; col < this.world2.board.size(); col++) {
      for (int row = 0; row < this.world2.board.get(col).size(); row++) {
        this.world2.board.get(col).get(row).setPowered(false);
      }
    }
    this.world2.setTilesPowered();
    // check that the correct tiles are powered
    t.checkExpect(this.world2.board.get(0).get(0).powered, false);
    t.checkExpect(this.world2.board.get(0).get(1).powered, true);
    t.checkExpect(this.world2.board.get(1).get(0).powered, true);
    t.checkExpect(this.world2.board.get(1).get(1).powered, true);
  }

  //test the bfs method 
  void testBFS(Tester t) {
    setup();
    GamePiece powerStation = this.world1.board.get(this.world1.powerCol).get(this.world1.powerRow);
    GamePiece connectedPiece = this.world1.board.get(3).get(1);
    GamePiece unconnectedPiece = this.world1.board.get(0).get(0);
    // test on a cell not connected to the power station
    t.checkExpect(this.world1.bfs(connectedPiece, powerStation), true);
    // test on a piece not connected to the power station
    t.checkExpect(this.world1.bfs(unconnectedPiece, powerStation), false);
  }

  // test the searchHelp method 
  void testSearchHelp(Tester t) {
    setup();
    GamePiece powerStation = this.world1.board.get(this.world1.powerCol).get(this.world1.powerRow);
    GamePiece connectedPiece = this.world1.board.get(3).get(1);
    GamePiece unconnectedPiece = this.world1.board.get(0).get(0);
    // test on a cell not connected to the power station
    t.checkExpect(this.world1.searchHelp(connectedPiece, powerStation, new Queue<GamePiece>()), 
        true);
    // test on a piece not connected to the power station
    t.checkExpect(this.world1.searchHelp(unconnectedPiece, powerStation, new Queue<GamePiece>()), 
        false);
  }


  // tests the method setOutEdges
  void testSetOutEdges(Tester t) {
    setup();

    // test where the edge is implemented
    t.checkExpect(this.piece211.outEdges, new ArrayList<Edge>(Arrays.asList()));
    this.piece211.setOutEdges(new ArrayList<Edge>(Arrays.asList(this.edge31, this.edge42)));
    t.checkExpect(this.piece211.outEdges, new ArrayList<Edge>(Arrays.asList(this.edge31)));

    // test where the edge is not implemented
    t.checkExpect(this.piece210.outEdges, new ArrayList<Edge>(Arrays.asList(this.edge34,
        this.edge43)));
    this.piece210.setOutEdges(new ArrayList<Edge>(Arrays.asList(this.edge31)));
    t.checkExpect(this.piece210.outEdges, new ArrayList<Edge>());
  }


  // tests the method setAllOuterEdges
  void testSetAllOuterEdges(Tester t) {
    setup();

    ArrayList<Edge> edges = new ArrayList<>(Arrays.asList(
        this.edge34, this.edge42, this.edge24, this.edge43, this.edge13, this.edge31));

    this.world2.mst = edges;
    this.world2.setAllOuterEdges();
    // testing there are no edges where there are supposed to be none
    t.checkExpect(this.piece200.outEdges, new ArrayList<Edge>());
    t.checkExpect(this.piece201.outEdges, new ArrayList<Edge>(Arrays.asList(this.edge42,
        this.edge13)));
    // testing that there are edges where there are supposed to be
    t.checkExpect(this.piece210.outEdges, new ArrayList<Edge>(Arrays.asList(this.edge24,
        this.edge43)));
    t.checkExpect(this.piece211.outEdges, new ArrayList<Edge>(Arrays.asList(this.edge34,
        this.edge31)));
  }
}