package com.example.dopecoder.myapplication;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dopecoder on 14/5/17.
 */

public class TrafficSignal {
    public String id;
    public long no_of_points;
    public List<Loc> points;

    public TrafficSignal(String id, long no_of_points, List<Loc> loc){
        this.id = id;
        this.no_of_points = no_of_points;
        this.points = loc;// = new ArrayList<Loc>();
        //points.add(loc);
    }

    public void addLoc(Loc loc){
        this.points.add(loc);
    }
}
