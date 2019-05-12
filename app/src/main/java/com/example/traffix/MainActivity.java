package com.example.traffix;

// We need to use this Handler package
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    // Create the Handler object (on the main thread by default)
    Handler handler = new Handler();

    private long delayMillis = 1000;
    private int beepDuration = 500;
    private int possibilityThreshold = 70;
    private int directionDifferenceThreshold = 30;
    private int joinPossibilityThreshold = 60;

    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Handle velocity SeekBar
        SeekBar velocitySeekBar=(SeekBar)findViewById(R.id.seekBarVelocity);
        final TextView textViewVelocity =  (TextView) findViewById(R.id.textViewVelocity);
        velocitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewVelocity.setText("" + progress);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Handle distance SeekBar
        SeekBar distanceSeekBar=(SeekBar)findViewById(R.id.seekBarDistance);
        final TextView textViewDistance =  (TextView) findViewById(R.id.textViewDistance);
        distanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewDistance.setText("" + progress);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Handle direction SeekBar
        SeekBar directionSeekBar=(SeekBar)findViewById(R.id.seekBarDirection);
        final TextView textViewDirection =  (TextView) findViewById(R.id.textViewDirection);
        directionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewDirection.setText("" + progress);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Start the initial runnable task by posting through the handler
        handler.post(runnableCode);
    }

    // Define the code block to be executed
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            doTheJob();

            // Καθυστέρηση
            handler.postDelayed(this, delayMillis);
        }

        public void doTheJob() {
            final TextView alertTextView =  (TextView) findViewById(R.id.textViewAlert);
            alertTextView.setText("");

            long junction = 0;
            try {
                //junction = Long.valueOf(getString(R.string.junction));
                EditText editTextJunction =  (EditText) findViewById(R.id.editText);
                junction = Long.valueOf(editTextJunction.getText().toString());
            } catch (Exception e) {
                return;
            }

            // Read velocity
            TextView textViewVelocity =  (TextView) findViewById(R.id.textViewVelocity);
            int velocity = 0;
            try {
                velocity = Integer.valueOf(textViewVelocity.getText().toString());
            } catch (Exception e) {
                return;
            }

            // Read distance
            TextView textViewDistance=  (TextView) findViewById(R.id.textViewDistance);
            int distanceToJunction = 0;
            try {
                distanceToJunction = Integer.valueOf(textViewDistance.getText().toString());
            } catch (Exception e) {
                return;
            }
            if (distanceToJunction <= 0) {
                return;
            }

            // Read direction
            TextView textViewDirection=  (TextView) findViewById(R.id.textViewDirection);
            int direction = 0;
            try {
                direction = Integer.valueOf(textViewDirection.getText().toString());
            } catch (Exception e) {
                return;
            }
            if (direction < 0) {
                return;
            }
            if (direction > 360) {
                return;
            }

            // Εδώ ο τύπος
            double g=9.98;               //accelaration due to gravity
            double m=0.4;                 //friction coefficient
            double u=velocity*1000/3600;                     //transform velocity in m/s
            double a=m*g;                                    //decelaration
            double t=u/a ;                                      //needed time for immobilazation
            double minStopDistance = u*t-a*t*t/2  ;                      //minnimum immobilazation distance            int minStopDistance = 2 * velocity / 10;

            int possibility = Double.valueOf(100 * minStopDistance / distanceToJunction).intValue();
            if (possibility > 100) {
                possibility = 100;
            }
            if (possibility < 0) {
                possibility = 0;
            }

            TextView ektimisiTextView =  (TextView) findViewById(R.id.textViewEktimisi);
            ektimisiTextView.setText("" + possibility);

            if (possibility < possibilityThreshold) {
                return;
            }

            // Set dieleusi info
            String country = "Greece";
            String city = "Athens";

            long time = Calendar.getInstance().getTimeInMillis();

            Dieleusi dieleusi = new Dieleusi(junction,
                    time,
                    possibility,
                    direction);

            /**/
            // Store it to Firestore
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            CollectionReference countryCollection = firestore.collection(country);
            DocumentReference cityDocument = countryCollection.document(city);
            CollectionReference junctionCollection = cityDocument.collection(""+junction);
            junctionCollection.document(time + "_" + direction).set(dieleusi);

            // Ερώτημα για ενδεχόμενη σύγκρουση
            final int directionCheck = direction;
            final int possibilityCheck = possibility;
            Query query = junctionCollection.whereGreaterThan("time", time - 1000);
            query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            //Map m = document.getData();
                            long dir = document.getLong("direction");

                            long dirDiff = directionCheck - dir;
                            if (dirDiff < 0) {
                                dirDiff = -dirDiff;
                            }

                            if (dirDiff > directionDifferenceThreshold) {
                                long pos = document.getLong("possibility");
                                long pp = pos * possibilityCheck / 100;
                                if (pp >= joinPossibilityThreshold) {
                                    alertTextView.setText("ALERT!!!");
                                    // Beep
                                    //toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,beepDuration);
                                    toneGen1.startTone(ToneGenerator.TONE_DTMF_0,beepDuration);
                                }
                            }
                        }
                    }
                }
            });
            /**/
        }
    };
}
