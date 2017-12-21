package io.github.veeshostak.aichat.models;


import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import java.util.HashSet;


// Users
// Hash Key: userId
// Range Key: string signUpDateTime
// conversationIds (String-set)
// other attributes...


@DynamoDBTable(tableName = "Users")
public class User {

    private String userId;
    private String signUpDateTime;

    private HashSet<String> conversationIds;
    private String fullName;
    private String email;


    @DynamoDBHashKey(attributeName = "UserId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) { this.userId = userId; }

    // =====

    @DynamoDBAttribute(attributeName="SignUpDateTime")
    public String getSignUpDateTime() { return signUpDateTime; }

    public void setSignUpDateTime(String signUpDateTime) { this.signUpDateTime = signUpDateTime; }


    @DynamoDBAttribute(attributeName = "ConversationIds")
    public HashSet<String> getConversationIds() {
        return conversationIds;
    }

    public void setConversationIds(HashSet<String> conversationIds) {
        // shallow copy, not independent

        // don't use clone, clone is broken
        //this.conversation = (ArrayList<String>)conversation.clone();

        this.conversationIds = new HashSet<String>(conversationIds);
    }


    @DynamoDBAttribute(attributeName = "FullName")
    public String getFullName() { return fullName; }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @DynamoDBAttribute(attributeName = "Email")
    public String getEmail() { return email; }

    public void setEmail(String email) {
        this.email = email;
    }


}
