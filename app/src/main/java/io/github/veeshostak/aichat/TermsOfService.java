package io.github.veeshostak.aichat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by vladshostak on 2/2/17.
 */

public class TermsOfService extends AppCompatActivity implements View.OnClickListener {

    private Button mFinishButton;
    private TextView mFullTermsText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_of_service);

        mFinishButton = (Button) findViewById(R.id.button2);
        mFinishButton.setOnClickListener(this);

        mFullTermsText = (TextView) findViewById(R.id.textTerms);
        mFullTermsText.setMovementMethod(new ScrollingMovementMethod());

    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.button2) {
            finish();
        }
    }


}