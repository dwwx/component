package com.racoon.redisex.base;


public class User {
    private String name;
    private String hoppy;
    private String school;

    public User(String name, String hoppy, String school){
        this.name = name;
        this.hoppy = hoppy;
        this.school = school;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHoppy() {
        return hoppy;
    }

    public void setHoppy(String hoppy) {
        this.hoppy = hoppy;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    @Override
    public String toString() {
        return name+"-"+hoppy+"-"+school;
    }
}
