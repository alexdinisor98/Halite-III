package hlt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SubMap {
    public int startCol;
    public int startRow;
    public int endCol;
    public int endRow;
    public int maxHlt;

    public SubMap(int startRow, int endRow, int startCol, int endCol, int maxHlt) {
        super();
        this.startCol = startCol;
        this.startRow = startRow;
        this.endCol = endCol;
        this.endRow = endRow;
        this.maxHlt = maxHlt;
    }

    public ArrayList<SubMap> divideMap(GameMap gameMap) {
        ArrayList<SubMap> submaps = new ArrayList<>();
        int w0 = 0;
        int w1 = gameMap.width * 1 / 3;
        int w2 = gameMap.width * 2 / 3;
        int h0 = 0;
        int h1 = gameMap.height * 1 / 3;
        int h2 = gameMap.height * 2 / 3;
        int h3 = gameMap.height;

        submaps.add(new SubMap(h0, h1, w0, w1, computeHalite(w0, w1, h0, h1, gameMap)));
        submaps.add(new SubMap(h0, h1, w2, w1, computeHalite(w1, w2, h0, h1, gameMap)));

        submaps.add(new SubMap(h2, h3, w0, w1, computeHalite(w0, w1, h2, h3, gameMap)));
        submaps.add(new SubMap(h2, h3, w1, w2, computeHalite(w1, w2, h2, h3, gameMap)));


        Collections.sort(submaps, new Comparator<SubMap>() {
            @Override
            public int compare(SubMap o1, SubMap o2) {
                return o2.maxHlt - o1.maxHlt;
            }
        });
        return submaps;
    }

    public int computeHalite(int startCol, int endCol, int startRow, int endRow, GameMap gameMap) {
        int sumMax = 0;
        for (int i = startRow; i < endRow; i++) {
            for (int j = startCol; j < endCol; j++) {
                sumMax += gameMap.cells[i][j].halite;
            }
        }
        return sumMax;
    }
}
