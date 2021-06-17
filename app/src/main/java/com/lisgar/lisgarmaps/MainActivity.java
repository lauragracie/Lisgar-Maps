package com.lisgar.lisgarmaps;

//Import Android libraries and widgets

//Import libraries for reading from files
import android.content.res.AssetManager;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

//Import libraries for layout elements (buttons, images, etc)
import android.widget.Button;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

//Import libraries for pop-up dialog
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;

//Import libraries for basic app functionality
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.content.Context;

//Import library to get events for keyboard and searchBar
import android.view.inputmethod.InputMethodManager;

//Import library for Arrays
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{
    //Declare buttons
    Button bfloor1;
    Button bfloor2;
    Button bfloor3;
    Button bfloor4;
    Button bdirections;

    //Declare images for map and location marker
    ImageView floorimage;
    ImageView location;

    //Declare text input fields
    SearchView searchBar;
    EditText aTo;
    EditText aFrom;

    TextView textDisplay;

    AssetManager assetManager;

    //Define format of database (146 rooms with 6 data fields to read in for each)
    final int numRooms = 146;
    final int numDataFields = 6;

    boolean isRoomDisplay = false;
    String displayedRoom = "";

    //Initialize array to store room database and vector to store instructions
    ArrayList<Location> roomDatabase = new ArrayList<>();
    ArrayList<String> instructions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Read in database from text file
        roomDatabase.addAll(getDatabase());

        //Initialize buttons
        bfloor1 = findViewById(R.id.bfloor1);
        bfloor2 = findViewById(R.id.bfloor2);
        bfloor3 = findViewById(R.id.bfloor3);
        bfloor4 = findViewById(R.id.bfloor4);
        bdirections = findViewById(R.id.bdirections);

        //Initialize images
        floorimage = findViewById(R.id.floorimage);
        location = findViewById(R.id.location);

        //Initialize text display
        textDisplay = findViewById(R.id.textDisplay);

        //Initialize text fields
        searchBar = findViewById(R.id.searchBar);
        aTo = new EditText(this);
        aFrom = new EditText(this);

        //Create new dialog for getting directions between rooms
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        //Setup search bars
        aFrom.setHint("From");
        layout.addView(aFrom);
        aTo.setHint("To");
        layout.addView(aTo);

        builder.setView(layout);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //When the "Ok" button is pressed, read in the text from the search bars
                String from = aFrom.getText().toString();
                String to = aTo.getText().toString();

                //Display the final destination on the map
                int toRoomIndex = getRoomIndex(to);
                if(toRoomIndex != -1){
                    switchFloor(roomDatabase.get(toRoomIndex).getFloor());
                    displayRoom(to);
                    location.setAlpha(1.0f);
                }

                //Generate instructions between the two locations
                instructions.clear();
                instructions = generateInstructions(from, to);

                //Display numbered list of instructions to the screen
                textDisplay.setText("");
                for (int i = 0; i < instructions.size(); i++) {
                    textDisplay.append((i + 1) + ". ");
                    textDisplay.append(instructions.get(i));
                    textDisplay.append("\n");
                }

                dialog.dismiss();
            }
        });

        //When the cancel button is pressed, close the dialog
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final AlertDialog alertDialog = builder.create();

        //When a floor button is pressed, switch to the respective floor
        bfloor1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                switchFloor(1);
            }
        });

        bfloor2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                switchFloor(2);
            }
        });

        bfloor3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                switchFloor(3);
            }
        });

        bfloor4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                switchFloor(4);
            }
        });


        //When the directions button is pressed, bring up the dialog to get directions
        bdirections.setOnClickListener(new View.OnClickListener(){
            public void onClick (View view){
                alertDialog.show();
            }
        });


        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
            @Override
            //When a search in submitted, display the room on the map
            public boolean onQueryTextSubmit(String query) {
                displayRoom(query);
                closeKeyboard();
                floorimage.requestFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

    }


    //Display a room on a map using the location marker based on a room name or number
    public void displayRoom(String room){
        int roomIndex = getRoomIndex(room);

        if (roomIndex != -1) {
            Location myLocation = roomDatabase.get(roomIndex);

            //If the room is in the database, switch to the floor that that room is on and display location marker
            switchFloor(myLocation.getFloor());

            textDisplay.setText("");
            location.setAlpha(1.0f);
            displayedRoom = room;
            isRoomDisplay = true;

            //Move location marker to the position of the room on the map
            View location = findViewById(R.id.location);
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) location.getLayoutParams();
            int floorX = getFloorX();
            int floorY = getFloorY();

            //getHeight gives height on screen. getIntrinsicHeight shows original file height
            float scaleFactor = (float)floorimage.getHeight()/floorimage.getDrawable().getIntrinsicHeight();

            //Position on the map is defined by the x and y values for that room in the database
            //relative to the position of the floor map.
            int xPos = (int)(floorimage.getWidth() * scaleFactor * myLocation.getxPos());
            int yPos = (int)(floorimage.getHeight() * myLocation.getyPos());

            lp.leftMargin = floorX + xPos - (location.getMeasuredWidth()/2);
            lp.topMargin = floorY + yPos - location.getMeasuredHeight();

            location.setLayoutParams(lp);

        } else {
            //If the room isn't in the database, display error message and clear location marker
            location.setAlpha(0.0f);
            displayedRoom = "";
            isRoomDisplay = false;
            textDisplay.setText("Couldn't find a room with that name/number \n\n");
        }

    }


    //Function that generates a simple list of instructions to arrive at your destination
    //Takes in room name or number for start and end locations
    public ArrayList<String> generateInstructions (String start, String end) {
        ArrayList<String> instructions = new ArrayList<>();

        int startIndex = getRoomIndex(start);
        int endIndex = getRoomIndex(end);

        //Display error message if one of the rooms isn't in the database
        if (startIndex == -1) {
            instructions.add("Starting room does not exist");
            return instructions;
        }
        else if(endIndex == -1){
            instructions.add("Destination room does not exist");
            return instructions;
        }

        Location startLocation = roomDatabase.get(startIndex);
        Location endLocation = roomDatabase.get(endIndex);

        boolean isSameBuilding = startLocation.getBuilding().equals(endLocation.getBuilding());

        //If the rooms are in different building and the end location isn't a portable (portable floor = "N/A")
        //display instructions to change buildings
        if (!isSameBuilding & !endLocation.getBuilding().equals("N/A")) {

            String size;
            switch (endLocation.getBuilding()) {
                case "North":
                    size = "large";
                    break;
                case "South":
                    size = "small";
                    break;
                default:
                    size = "N/A";
            }

            instructions.add("Go to the " + endLocation.getBuilding() + " (" + size + ") building.");
        }

        //If destination is a portable, add instructions to go to the parking cage
        else if (endLocation.getBuilding().equals("N/A")) {
            instructions.add("Exit the building.");
            instructions.add("Go to the parking cage (on Lisgar street).");
        }


        //If the destination isn't a portable, add instruction on which floor to go to
        if (!endLocation.getBuilding().equals("N/A")) {

            int floorDifference;
            if(isSameBuilding){
                floorDifference = endLocation.getFloor() - startLocation.getFloor();
            }
            else{
                floorDifference = endLocation.getFloor() - 2;
            }


            if(floorDifference > 0){
                instructions.add("Go up " + floorDifference + " flights to floor " + endLocation.getFloor());
            }
            else if(floorDifference < 0){
                instructions.add("Go down " + Math.abs(floorDifference) + " flights to floor " + endLocation.getFloor());
            }
            if(floorDifference == 0 && isSameBuilding){
                instructions.add("Stay on the current floor");
            }

        }

        //Add instruction to proceed to the room
        //If the room has a name (ex: Cafeteria, Main Office, etc.), include it in the instruction
        if (!endLocation.getName().equals("N/A")){
            instructions.add("Go to Room " + endLocation.getRoomNumber() + " (" + endLocation.getName() + ").");
        }
        else {
            instructions.add("Go to Room " + endLocation.getRoomNumber() + ".");
        }

        return instructions;
    }

    //Read data into the roomDatabase array from the text file
    private ArrayList<Location> getDatabase() {
        ArrayList<Location> locationDatabase = new ArrayList<>();
        assetManager = getAssets();
        try {
            InputStream inputStream = assetManager.open("RoomDatabase.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String[] inputArray = new String[numDataFields];
                String receiveString;

                for(int i = 0; i<numRooms; i++){
                    //Read in first line (list number of room) but don't process the data.
                    bufferedReader.readLine();

                    //Read in the data fields from the file for each room.
                    //0 = room number, 1 = building, 2 = floor, 3 = name, 4 = xPos, 5 = yPos
                    for(int j = 0; j<numDataFields; j++){
                        receiveString = bufferedReader.readLine();
                        if(receiveString != null) {
                            inputArray[j] = receiveString;
                        }
                    }
                    //Read in empty space between entries.
                    bufferedReader.readLine();

                    Location newLocation = new Location();

                    newLocation.setRoomNumber(inputArray[0]);
                    newLocation.setBuilding(inputArray[1]);

                    int floor = Integer.parseInt(inputArray[2]);
                    newLocation.setFloor(floor);

                    newLocation.setName(inputArray[3]);

                    float xPos = Float.parseFloat(inputArray[4]);
                    newLocation.setxPos(xPos);

                    float yPos = Float.parseFloat(inputArray[5]);
                    newLocation.setyPos(yPos);

                    locationDatabase.add(newLocation);

                }
                inputStream.close();
            }
            return locationDatabase;
        }
        //Catch exceptions and log errors
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
            return null;
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
            return null;
        }
    }

    //Given a name or number for a room, return index of that room in the database
    private int getRoomIndex(String n){
        for(int j = 0; j < numRooms; j++){
            if(n.compareToIgnoreCase(roomDatabase.get(j).getRoomNumber()) == 0
                    || n.compareToIgnoreCase(roomDatabase.get(j).getName()) == 0){
                //Ignore cases when n is "N/A", which is the default room name value
                if(!n.equals("N/A")){
                    return j;
                }
            }
        }
        return -1;
    }

    //Return X value for the floor map
    private int getFloorX(){
        return (int)floorimage.getX();
    }

    //Return Y value for the floor map
    private int getFloorY(){
        return (int)floorimage.getY();
    }

    //Given a floor number, display the map for that floor
    public void switchFloor(int floor){
        floorimage.setAlpha(1.0f);
        switch(floor){
            case 1:
                floorimage.setImageResource(R.drawable.floor1);
                break;
            case 2:
                floorimage.setImageResource(R.drawable.floor2);
                break;
            case 3:
                Log.i("MARKER", "X Pos " + location.getX());
                Log.i("MARKER", "Y Pos " + location.getY());

                floorimage.setImageResource(R.drawable.floor3);
                break;
            case 4:
                floorimage.setImageResource(R.drawable.floor4);
                break;
            default:
                floorimage.setImageResource(R.drawable.floor2);
                break;
        }

        if(isRoomDisplay){
            int roomIndex = getRoomIndex(displayedRoom);

            //If the floor that the room is on is being displayed, show the location marker
            if(roomDatabase.get(roomIndex).getFloor() == floor){
                location.setAlpha(1.0f);
                isRoomDisplay = true;
            }
            //If the floor isn't being displayed, don't show the location marker
            else{
                location.setAlpha(0.0f);
            }
        }

//        if(isRoomDisplay){
//            int searchRoomIndex = getRoomIndex(searchBar.getText().toString());
//            int destinationRoomIndex = getRoomIndex(aTo.getText().toString());
//            int displayRoomIndex = -1;
//
//            //If the search bar has a valid room, set the displayRoomIndex to that value
//            if(searchRoomIndex != -1){
//                displayRoomIndex = searchRoomIndex;
//            }
//            //else if the "To:" search bar has a valid room, set the displayRoomIndex to that value
//            else if (destinationRoomIndex != -1){
//                displayRoomIndex = destinationRoomIndex;
//            }
//
//
//            //If the floor that the room is on is being displayed, show the location marker
//            if(displayRoomIndex != -1 && roomDatabase.get(displayRoomIndex).getFloor() == floor){
//                displayRoom();
//                location.setAlpha(1.0f);
//                isRoomDisplay = true;
//            }
//            //If the floor isn't being displayed, don't show the location marker
//            else{
//                location.setAlpha(0.0f);
//            }
//        }


//        if(isRoomDisplay){
//            int roomIndex = getRoomIndex(searchBar.getText().toString());
//            //If the search bar is empty, get the room name/number from the "To:" search bar
//            if(getRoomIndex(searchBar.getText().toString()) == -1 ){
//                roomIndex = getRoomIndex(aTo.getText().toString());
//            }
//            //If the floor that the room is on is being displayed, show the location marker
//            if(roomIndex != -1 && roomDatabase.get(roomIndex).getFloor() == floor){
//                location.setAlpha(1.0f);
//                isRoomDisplay = true;
//            }
//            //If the floor isn't being displayed, don't show the location marker
//            else{
//                location.setAlpha(0.0f);
//            }
//        }
    }

    //Close the keyboard
    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}