package io.github.veeshostak.aichat.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import io.github.veeshostak.aichat.database.entity.ChatPost;

/**
 * Created by vladshostak on 12/23/17.
 */

//  @Insert: Room generates an implementation that inserts all parameters into the database in a
// single transaction.

@Dao
public interface ChatPostDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertChatPosts(ChatPost... chatPosts); // array of chatPosts (0 or more)

    @Update
    void updateChatPosts(ChatPost... chatPosts);

    @Delete // uses the primary keys to find the entities to delete.
    void deleteChatPosts(ChatPost... chatPosts);

    // Queries:
    // Each @Query method is verified at compile time. (Room also verifies the return value of the query)

    @Query("SELECT * FROM chat_posts")
    LiveData<List<ChatPost>> getAllChatPosts();

    @Query("SELECT * FROM chat_posts WHERE pushed_to_remote_db = 0")
    LiveData<List<ChatPost>> getAllChatPostsNotInRemoteDb();

    @Query("DELETE FROM chat_posts")
    void deleteAllChatPosts();

//    @Query("SELECT * FROM chat_posts WHERE user_query = :userQuery LIMIT 1")
//    ChatPost getChatPostWithUserQuery(String userQuery);

}
