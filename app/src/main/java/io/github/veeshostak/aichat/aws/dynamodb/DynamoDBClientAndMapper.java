package io.github.veeshostak.aichat.aws.dynamodb;

// aws

import android.content.Context;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

import io.github.veeshostak.aichat.R;
//import com.amazonaws.services.dynamodbv2.model.*;


public class DynamoDBClientAndMapper {

    // private AmazonDynamoDBClient ddbClient;
    private DynamoDBMapper mapper;

    public DynamoDBClientAndMapper(Context theApplicationContext, CognitoCachingCredentialsProvider credentialsProvider) {

        // ==============
        // START aws

        // Initialize the Amazon Cognito credentials provider
//        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
//                theApplicationContext,
//                theApplicationContext.getString(R.string.dynamodb_identity_pool_id), // Identity pool ID
//                Regions.US_WEST_2 // Region
//        );


        //String identityId = credentialsProvider.getIdentityId();
        //Log.d("LogTag", "my ID is " + identityId);

        // NOTE: the default endpoint of the created AmazonDynamoDBClient is
        // us-east-1, we must set the correct region

        //ddbClient = new AmazonDynamoDBClient(credentialsProvider); // uses the default, wrong region

        AmazonDynamoDBClient ddbClient = Region.getRegion(Regions.US_WEST_2)
                .createClient(
                        AmazonDynamoDBClient.class,
                        credentialsProvider,
                        new ClientConfiguration()
                );

        mapper = new DynamoDBMapper(ddbClient);

        // ==============
        // END aws
    }

    public DynamoDBMapper getMapper() { return mapper; }
}
