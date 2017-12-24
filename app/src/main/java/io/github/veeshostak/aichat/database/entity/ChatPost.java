package io.github.veeshostak.aichat.database.entity;

/**
 * Created by vladshostak on 12/23/17.
 */

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;


@Entity(tableName = "chat_posts")
public class ChatPost {

    @PrimaryKey(autoGenerate=true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "user_query")
    private String userQuery;

    @ColumnInfo(name = "response")
    private String response;

    @ColumnInfo(name = "created_at")
    private String createdAt;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserQuery() {
        return userQuery;
    }

    public void setUserQuery(String userQuery) {
        this.userQuery = userQuery;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

