package net.sofaer.icenein;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingApi;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static android.location.LocationManager.GPS_PROVIDER;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{

    LocationManager l;
    GoogleApiClient mGoogleApiClient;
    boolean locationDenied;

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    ValueEventListener fenceUpdater = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Sighting object and create a fence
            HashMap sightings = (HashMap)dataSnapshot.getValue();
            GeofencingRequest.Builder requestBuilder = new GeofencingRequest.Builder();
            for(Object key: sightings.keySet()) {
                Geofence.Builder builder = new Geofence.Builder();
                HashMap location = (HashMap)((HashMap)sightings.get(key)).get("location");
                builder.setCircularRegion((double)location.get("latitude"), (double)location.get("longitude"), 1000);
                builder.setRequestId((String)key);
                builder.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER);
                builder.setExpirationDuration(1000 * 60 * 60 * 8);
                requestBuilder.addGeofence(builder.build());
            }
            GeofencingRequest request = requestBuilder.build();
            PendingResult<Status> result = LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, request, null);

            Log.v("Fence", request.toString());
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context c = getApplicationContext();

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        locationDenied = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        if (locationDenied) {
            Button button=(Button)findViewById(R.id.button2);
            button.setText("Allow Location");
        }
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference sightings = database.getReference("sightings");
        sightings.addValueEventListener(fenceUpdater);
    }

    public void reportLocation(View view) {
        Log.v("Button", "clicked");

        if (locationDenied) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
            this.recreate();
            return;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference sightings = database.getReference("sightings");

        String key = sightings.push().getKey();

        HashMap data = new HashMap();
        Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);;
        data.put("location", loc);
        data.put("time", System.currentTimeMillis());
        sightings.child(key).setValue(data);

        Button button=(Button)findViewById(R.id.button2);
        button.setText("Thanks!");
        button.setEnabled(false);


        Log.v("Data", data.toString());
        // Kabloey
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.v("Connected", "true");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v("Connected", "false");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.v("Connected", "nope");

    }
}
