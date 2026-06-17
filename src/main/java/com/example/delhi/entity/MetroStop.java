package com.example.delhi.entity;

public class MetroStop {

    private String stop_id;
    private String stop_name;

    public MetroStop(String stop_id, String stop_name){
        this.stop_id = stop_id;
        this.stop_name = stop_name;
    }

    public String getStop_id(){
        return stop_id;
    }
    public String getStop_name(){
        return stop_name;
    }
    
}
