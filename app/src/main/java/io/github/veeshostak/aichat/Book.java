package io.github.veeshostak.aichat;



import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

import java.util.ArrayList;


@DynamoDBTable(tableName = "Books")
public class Book {
    private String title;
    private String author;
    private int price;
    private String isbn;
    private Boolean hardCover;
    private ArrayList<String> conversation;

    @DynamoDBIndexRangeKey(attributeName = "Title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @DynamoDBIndexHashKey(attributeName = "Author")
    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @DynamoDBAttribute(attributeName = "Price")
    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    @DynamoDBHashKey(attributeName = "ISBN")
    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    @DynamoDBAttribute(attributeName = "Hardcover")
    public Boolean getHardCover() {
        return hardCover;
    }

    public void setHardCover(Boolean hardCover) {
        this.hardCover = hardCover;
    }

    // ===

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