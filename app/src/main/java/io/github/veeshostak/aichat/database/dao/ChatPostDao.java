package io.github.veeshostak.aichat.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import io.github.veeshostak.aichat.database.entity.ChatPost;

/**
 * Created by vladshostak on 12/23/17.
 */

//  @Insert: Room generates an implementation that inserts all parameters into the database in a
// single transaction.

@Dao
public interface ChatPostDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public void insertChatMessages(ChatPost... chatMessages); // array of chatMessages

    @Update
    public void updateChatPosts(ChatPost... chatPost);

    @Delete // uses the primary keys to find the entities to delete.
    public void deleteChatPosts(ChatPost... chatPost);

    // Queries:
    // Each @Query method is verified at compile time. (Room also verifies the return value of the query)

    @Query("SELECT * FROM chat_posts")
    public ChatPost[] getAllChatPosts();

    @Query("SELECT * FROM chat_posts WHERE user_query = :userQuery LIMIT 1")
    public ChatPost getChatPostWithUserQuery(String userQuery);



}
