package io.github.veeshostak.aichat.aws.cognito;

import android.content.Context;
import android.content.res.Resources;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.regions.Regions;

import java.util.HashSet;
import java.util.Set;

import io.github.veeshostak.aichat.R;

/**
 * Created by vladshostak on 12/30/17.
 */

public class CognitoHelper {

    // App settings
    private static CognitoHelper appHelper;
    private static CognitoUserPool userPool;
    private static String user;

    private static String userPoolId;
    private static String clientId;
    /**
     * App secret associated with your app id - if the App id does not have an associated App secret,
     * set the App secret to null.
     * e.g. clientSecret = null;
     */
    private static String clientSecret;
    private static Regions cognitoRegion = Regions.US_WEST_2;

    // User details from the service
    private static CognitoUserSession currSession;
    private static CognitoUserDetails userDetails;


    public static void init(Context context) {

        userPoolId = context.getString(R.string.cognito_user_pool_id);
        clientId = context.getString(R.string.cognito_client_id);

        /**
         * App secret associated with your app id - if the App id does not have an associated App secret,
         * set the App secret to null.
         * e.g. clientSecret = null;
         */
        clientSecret = context.getString(R.string.cognito_client_secret_id);;


        if (appHelper != null && userPool != null) {
            return;
        }

        if (appHelper == null) {
            appHelper = new CognitoHelper();
        }

        if (userPool == null) {

            // Create a user pool with default ClientConfiguration
            userPool = new CognitoUserPool(context, userPoolId, clientId, clientSecret, cognitoRegion);

            // This will also work
            /*
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            AmazonCognitoIdentityProvider cipClient = new AmazonCognitoIdentityProviderClient(new AnonymousAWSCredentials(), clientConfiguration);
            cipClient.setRegion(Region.getRegion(cognitoRegion));
            userPool = new CognitoUserPool(context, userPoolId, clientId, clientSecret, cipClient);
            */


        }

    }

    public static CognitoUserPool getUserPool() {
        return userPool;
    }

    public static void setUserPool(CognitoUserPool userPool) {
        CognitoHelper.userPool = userPool;
    }

    public static CognitoUserSession getCurrSession() {
        return currSession;
    }

    public static void setCurrSession(CognitoUserSession currSession) {
        CognitoHelper.currSession = currSession;
    }

    public static CognitoUserDetails getUserDetails() {
        return userDetails;
    }

    public static void setUserDetails(CognitoUserDetails userDetails) {
        CognitoHelper.userDetails = userDetails;
    }

    public static String getUser() {
        return user;
    }

    public static void setUser(String user) {
        CognitoHelper.user = user;
    }


    public static String getUserPoolId() {
        return userPoolId;
    }

    public static Regions getCognitoRegion() {
        return cognitoRegion;
    }
}


