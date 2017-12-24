package io.github.veeshostak.aichat.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import io.github.veeshostak.aichat.database.dao.ChatPostDao;
import io.github.veeshostak.aichat.database.entity.ChatPost;

/**
 * Created by vladshostak on 12/23/17.
 */

@Database(entities = {ChatPost.class}, version = 1)
public abstract class ChatPostDatabase extends RoomDatabase {
    public abstract ChatPostDao chatPostDao();
}
