package io.github.veeshostak.aichat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;


import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import io.github.veeshostak.aichat.aws.cognito.CognitoHelper;
import io.github.veeshostak.aichat.R;


public class SignInActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SignInActivity"; // for logcat

    private EditText mNameField;
    private EditText mEmailField;
    private EditText mPasswordField;

    private TextView mTermOfServiceLink;
    private Button mSignUpButton;
    private Button mSignInButton;

    //private String uniqueId;

    private String email;
    private String name;




    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        initControls();

        CognitoHelper.init(getApplicationContext());

        // check if user is still authenticated (token still valid)
        // if yes sign them in automatically and go to ChatListActivity
        findCurrent();

    }

    private void initControls() {

        mNameField = (EditText) findViewById(R.id.nameField);
        mEmailField = (EditText) findViewById(R.id.emailField);
        mPasswordField = (EditText) findViewById(R.id.passwordField);

        mTermOfServiceLink = (TextView) findViewById(R.id.termsLink);
        mSignUpButton = (Button) findViewById(R.id.signUpButton);
        mSignInButton = (Button) findViewById(R.id.signInButton);

        // listeners
        mSignUpButton.setOnClickListener(this);
        mSignInButton.setOnClickListener(this);
        mTermOfServiceLink.setOnClickListener(this);

    }

    private void findCurrent() {
        CognitoUser user = CognitoHelper.getUserPool().getCurrentUser();
        email = user.getUserId();
        if(email != null) {
            CognitoHelper.setUser(email);
            user.getSessionInBackground(authenticationHandler);
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

            name = mNameField.getText().toString();
            if (name.isEmpty()) {
                mNameField.setError("Please enter a name");
                return;
            }

            // Dont need to set user in db, since we can access user analytics in Cognito
            //setUserInDb(name, email);

            // Create a CognitoUserAttributes object and add user attributes
            CognitoUserAttributes userAttributes = new CognitoUserAttributes();

            // Add the user attributes. Attributes are added as key-value pairs
            // Adding user's given name.
            // Note that the key is "name" which is the OIDC claim for name
            userAttributes.addAttribute("name", name);

            // Adding user's email
            userAttributes.addAttribute("email", email);

            // create a unique userId. // email is passed, id is created on server
            //uniqueId = createUniqueId();

            // call the sign-up api.
            CognitoHelper.getUserPool().signUpInBackground(email, mPasswordField.getText().toString(), userAttributes, null, signupCallback);

            // after sign up, user confirms their email. then they can sign in

        } else if (i == R.id.signInButton) {


            email = mEmailField.getText().toString();
            if (!isValidEmail(email)) {
                mEmailField.setError("Please enter a valid email");
                return;
            }

            name = mNameField.getText().toString();
            if (name.isEmpty()) {
                mNameField.setError("Please enter a name");
                return;
            }

            // Sign in the user
            CognitoHelper.getUserPool().getUser(email).getSessionInBackground(authenticationHandler);

        } else if (i == R.id.termsLink) {
            startActivity(new Intent(SignInActivity.this, TermsOfService.class));
        }
    }

    public void signInSuccess() {

        // Fetch the user details
        CognitoHelper.getUserPool().getUser(email).getDetailsInBackground(getDetailsHandler);

    }

    // ==================
    // START: Callbacks
    // ==================

    // Callback handler for the sign-in process
    AuthenticationHandler authenticationHandler = new AuthenticationHandler() {

        @Override
        public void onSuccess(CognitoUserSession cognitoUserSession, CognitoDevice cognitoDevice) {
            // Sign-in was successful, cognitoUserSession will contain tokens for the user

            CognitoHelper.setCurrSession(cognitoUserSession);


            signInSuccess();
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
            // The API needs user sign-in credentials to continue
            AuthenticationDetails authenticationDetails = new AuthenticationDetails(userId, mPasswordField.getText().toString(), null);

            // Pass the user sign-in credentials to the continuation
            authenticationContinuation.setAuthenticationDetails(authenticationDetails);

            // Allow the sign-in to continue
            authenticationContinuation.continueTask();
        }


        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {
            // Multi-factor authentication is required; get the verification code from user
            multiFactorAuthenticationContinuation.setMfaCode("123");
            // Allow the sign-in process to continue
            multiFactorAuthenticationContinuation.continueTask();
        }


        @Override
        public void onFailure(Exception exception) {
            // Sign-in failed, check exception for the cause
            Toast.makeText(getApplicationContext(),
                    "error: " + exception.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {
            /**
             * For Custom authentication challenge, implement your logic to present challenge to the
             * user and pass the user's responses to the continuation.
             */
            if ("NEW_PASSWORD_REQUIRED".equals(continuation.getChallengeName())) {
                // This is the first sign-in attempt for an admin created user
//                        newPasswordContinuation = (NewPasswordContinuation) continuation;
//                        AppHelper.setUserAttributeForDisplayFirstLogIn(newPasswordContinuation.getCurrentUserAttributes(),
//                                newPasswordContinuation.getRequiredAttributes());
//                        closeWaitDialog();
//                        firstTimeSignIn();
            } else if ("SELECT_MFA_TYPE".equals(continuation.getChallengeName())) {
//                        closeWaitDialog();
//                        mfaOptionsContinuation = (ChooseMfaContinuation) continuation;
//                        List<String> mfaOptions = mfaOptionsContinuation.getMfaOptions();
//                        selectMfaToSignIn(mfaOptions, continuation.getParameters());
            }
        }
    };

    // ===

    // Create a callback handler for sign-up. The onSuccess method is called when the sign-up is successful.
    SignUpHandler signupCallback = new SignUpHandler() {

        @Override
        public void onSuccess(CognitoUser cognitoUser, boolean userConfirmed, CognitoUserCodeDeliveryDetails cognitoUserCodeDeliveryDetails) {
            // Sign-up was successful

            // Check if this user (cognitoUser) needs to be confirmed
            if(!userConfirmed) {
                // This user must be confirmed and a confirmation link was sent to the user
                // cognitoUserCodeDeliveryDetails will indicate where the confirmation code was sent
                // Get the confirmation code from user if using confirmation code. else th user confirms an email link
                Toast.makeText(getApplicationContext(),
                        "Please confirm your email address " + cognitoUserCodeDeliveryDetails.getDestination(),
                        Toast.LENGTH_LONG).show();
            }
            else {
                // The user has already been confirmed
                Toast.makeText(getApplicationContext(),
                        "You have confirmed your email, please sign in! ",
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onFailure(Exception exception) {
            // Sign-up failed, check exception for the cause
            Toast.makeText(getApplicationContext(),
                    "error: " + exception.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    };

    // ===

    // Implement callback handler for getting details
    GetDetailsHandler getDetailsHandler = new GetDetailsHandler() {
        @Override
        public void onSuccess(CognitoUserDetails cognitoUserDetails) {
            // The user detail are in cognitoUserDetails

            CognitoHelper.setUserDetails(cognitoUserDetails);

            // Go to ChatListActivity
            startActivity(new Intent(SignInActivity.this, ChatListActivity.class));
            finish(); // exit, remove current activity
        }

        @Override
        public void onFailure(Exception exception) {
            // Fetch user details failed, check exception for the cause
            Toast.makeText(getApplicationContext(),
                    "error: " + exception.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    };

    // ==================
    // END: Callbacks
    // ==================

    public String createUniqueId() {
        // returns: 10/23/17 8:22AM
        String currentDateTimeString = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US).format(new Date());

        // format to 102317822AM
        currentDateTimeString = currentDateTimeString.replace("/", "").replace(" ", "").replace(":", "");

        // uuid + datetime
        String id = UUID.randomUUID().toString() + currentDateTimeString;

        return id;
    }

    public final static boolean isValidEmail(CharSequence target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }



}
