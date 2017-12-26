package io.github.veeshostak.aichat.aws.dynamodb.model;


import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import java.util.ArrayList;

import io.github.veeshostak.aichat.database.entity.ChatPost;


// User Conversations
// Hash Key: string conversationId
// Range Key: string conversationDateTime
// userId: string userId
// other attributes...

@DynamoDBTable(tableName = "User-Conversations")
public class UserConversation {
    private String conversationId;
    private String conversationDateTime;

    private String userId;
    private ArrayList<String> conversation;


    @DynamoDBHashKey(attributeName = "ConversationId")
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) { this.conversationId = conversationId; }


    @DynamoDBRangeKey(attributeName="ConversationDateTime") // sort
    public String getConversationDateTime() { return conversationDateTime; }

    public void setConversationDateTime(String conversationDateTime) { this.conversationDateTime = conversationDateTime; }


    // =====


    @DynamoDBAttribute(attributeName = "UserId")
    public String getUserId() { return userId; }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDBAttribute(attributeName = "Conversation")
    public ArrayList<String> getConversation() {
        return conversation;
    }

    public void setConversation(ArrayList<String> conversation) {
        // shallow copy, not independent

        // don't use clone, clone is broken
        //this.conversation = (ArrayList<String>)conversation.clone();

        this.conversation = new ArrayList<String>(conversation);
    }






}
