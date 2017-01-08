package com.omeryaari.minesweeper.ui;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.omeryaari.minesweeper.logic.Highscore;
import com.omeryaari.minesweeper.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HighscoreActivity extends AppCompatActivity {

    private String level;
    private LinearLayout fragmentLayout;
    private Fragment tableFragment;
    private Fragment mapFragment;
    private DatabaseReference highscoresDB;
    private ArrayList<Highscore> highscoreList = new ArrayList<>();
    private final String TAG = this.getClass().getSimpleName();
    private boolean isMap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_highscore);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        level = determineLevel(getIntent().getExtras().getInt("key"));
        loadScores();
        TextView highscoresLevel = (TextView) findViewById(R.id.highscores_level_textview);
        highscoresLevel.setText(level);
        fragmentLayout = (LinearLayout) findViewById(R.id.highscores_linear_layout);
        //createMapFragment();
        setupButtons();
    }

    //  Determines level.
    public String determineLevel(int level) {
        switch (level) {
            case 0:
                return "Easy";
            case 1:
                return "Normal";
            case 2:
                return "Hard";
        }
        return "";
    }

    //  Sets the two buttons up and assigns listeners to them.
    private void setupButtons() {
        Button tableButton = (Button) findViewById(R.id.highscores_table_button);
        Button mapButton = (Button) findViewById(R.id.highscores_map_button);
        Button returnButton = (Button) findViewById(R.id.highscores_return_button);
        tableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMap)
                    showTable();
            }
        });
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isMap)
                    showMap();
            }
        });
        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    //  Creates the table fragment on the activity's creation.
    private void createTableFragment() {
        tableFragment = TableScoreFragment.newInstance(highscoreList);
    }

    //  Creates the map fragment on the activity's creation.
    private void createMapFragment() {

    }

    //  Loads the table fragment into the fragment place holder.
    private void showTable() {
        isMap = false;
        FragmentTransaction fragTrans = getFragmentManager().beginTransaction();
        fragTrans.replace(R.id.highscores_fragment_placeholder, tableFragment);
        fragTrans.commit();
    }

    //  Loads the map fragment into the fragment place holder.
    private void showMap() {
        isMap = true;
        if (isGoogleMapsInstalled()) {
            // Add the Google Maps fragment dynamically
            final FragmentTransaction transaction = getFragmentManager().beginTransaction();
            MapFragment mapFragment = MapFragment.newInstance();
            transaction.replace(R.id.highscores_fragment_placeholder, mapFragment);
            transaction.commit();

            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    for (int i = 0; i < highscoreList.size(); i++) {
                        Highscore tempScore = highscoreList.get(i);
                        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocation(tempScore.getLatitude(), tempScore.getLongitude(), 1);
                            googleMap.addMarker(new MarkerOptions().
                                    position(new LatLng(tempScore.getLatitude(), tempScore.getLongitude())).
                                    title("Highscore #" + (i+1)).
                                    snippet("Name: " + tempScore.getName() + "\n" + "Time: " + tempScore.getCorrectedTimeString() + "\n" + "Address: " + addresses.get(0).getAddressLine(0)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    //  Loads the high scores from firebase.
    private void loadScores() {
        DatabaseReference highscoresDB = FirebaseDatabase.getInstance().getReference();
        highscoresDB.child("Highscores").child(level).child("List").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot snap: dataSnapshot.getChildren()) {
                    Highscore tempHighScore = snap.getValue(Highscore.class);
                    highscoreList.add(tempHighScore);
                }
                Collections.sort(highscoreList);
                createTableFragment();
                showTable();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "Error loading scores from firebase (onCancalled was called).");
            }
        });
    }

    //  Checks if google maps is installed.
    public boolean isGoogleMapsInstalled() {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0 );
            return info != null;
        }
        catch(PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
