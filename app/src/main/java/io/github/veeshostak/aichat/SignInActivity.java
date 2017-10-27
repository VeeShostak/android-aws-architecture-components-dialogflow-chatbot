package io.github.veeshostak.aichat;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;


import java.io.File;
import java.util.HashSet;

import io.github.veeshostak.aichat.Models.User;


public class SignInActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SignInActivity"; // for logcat

    private EditText mFirstNameField;
    private EditText mLastNameField;
    private EditText mEmailField;
    private TextView mTermOfServiceLink;
    private Button mSignUpButton;

    private DynamoDBMapper mapper;

    private String uniqueId;

    private String fullName;
    private String email;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        initControls();

        // Authenticate
        // check if file exists (user created), if yes, then obtain ID
        // if not, create and get id

        File installation = new File(getApplicationContext().getFilesDir(), "INSTALLATION");
        try {
            if (installation.exists()) {
                // if file exists, then uniqueId exists, proceed to application

                //Log.d(TAG, "SignInAcitvity: " + "ID: " + uniqueId + "\n");

                // pass along userID to main activity
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("UNIQUE_ID", uniqueId);
                startActivity(intent);

                // Go to MainActivity
                startActivity(new Intent(SignInActivity.this, MainActivity.class));
                finish(); // exit, remove current activity
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    private void initControls() {
        mFirstNameField = (EditText) findViewById(R.id.firstNameField);
        mLastNameField = (EditText) findViewById(R.id.lastNameField);
        mEmailField = (EditText) findViewById(R.id.emailField);
        mTermOfServiceLink = (TextView) findViewById(R.id.termsLink);
        mSignUpButton = (Button) findViewById(R.id.signUpButton);

        // listeners
        mSignUpButton.setOnClickListener(this);
        mTermOfServiceLink.setOnClickListener(this);

        DynamoDBClientAndMapper dynamoDb = new DynamoDBClientAndMapper(getApplicationContext());
        mapper = dynamoDb.getMapper();
    }


    private void setUserInDb() {
        final AsyncTask<String, Void, Void> taskUpdateDb = new AsyncTask<String, Void, Void>() {


            @Override
            protected Void doInBackground(final String... params) {

                String signUpDateTime = String.valueOf(System.currentTimeMillis() / 100); // get seconds

                // push back uID, first name, last name, email, and empty conversaton list

                User user = new User();
                user.setUserId(uniqueId);
                user.setSignUpDateTime(signUpDateTime); // todo: use AWS Lambda to set dt

                user.setFullName(fullName);
                user.setEmail(email);
                //user.setConversationIds(new HashSet<String>());

                mapper.save(user);

                return null;


            }

            @Override
            protected void onPostExecute(Void err) {
                if (err != null) {
                    //onResult(response);

                } else {
                    //onError(dbError);
                }
            }
        };

        // EXECUTE
        taskUpdateDb.execute();
    }


    public final static boolean isValidEmail(CharSequence target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }


    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.signUpButton) {

            email = mEmailField.getText().toString();
            if (!isValidEmail(email)) {
                mEmailField.setError("Please enter a valid email");
                return;
            }

            String firstName = mFirstNameField.getText().toString();
            if (firstName.length() < 1) {
                mEmailField.setError("Please enter a first name");
                return;
            }

            String lastName = mLastNameField.getText().toString();
            if (lastName.length() < 1) {
                mEmailField.setError("Please enter a last name");
                return;
            }

            fullName = firstName + " " + lastName;

            // get id (either retrieve existing, or create)
            Installation GetId = new Installation();
            uniqueId = GetId.id(getApplicationContext());


            // push back uID, first name, last name, email, and empty conversaton list
            setUserInDb();

            // Go to MainActivity
            startActivity(new Intent(SignInActivity.this, MainActivity.class));
            finish(); // exit, remove current activity

        } else if (i == R.id.termsLink) {
            startActivity(new Intent(SignInActivity.this, TermsOfService.class));
        }

    }
}
