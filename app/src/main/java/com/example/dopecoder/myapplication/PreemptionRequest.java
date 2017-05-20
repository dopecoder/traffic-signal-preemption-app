package com.example.dopecoder.myapplication;

/**
 * Created by dopecoder on 14/5/17.
 */

public class PreemptionRequest {
    public String _id;
    public String traffic_id;
    public Loc direction_data;
    public String authentication_data;

    public PreemptionRequest(String _id, String traffic_id, String authentication_data, Loc direction_data){
        this._id = _id;
        this.traffic_id = traffic_id;
        this.direction_data = direction_data;
        this.authentication_data = authentication_data;
    }
}
