package com.lisgar.lisgarmaps;

public class Location {
    private String roomNumber;
    private String building;
    private int floor;
    private String name;
    private float xPos;
    private float yPos;

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public float getxPos() { return xPos; }
    public void setxPos(float xPos) { this.xPos = xPos; }

    public float getyPos() { return yPos; }
    public void setyPos(float yPos) { this.yPos = yPos; }
}
