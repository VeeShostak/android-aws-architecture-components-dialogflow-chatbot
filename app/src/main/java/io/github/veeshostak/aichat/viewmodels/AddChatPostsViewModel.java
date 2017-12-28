package io.github.veeshostak.aichat.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import io.github.veeshostak.aichat.database.AppDatabase;
import io.github.veeshostak.aichat.database.entity.ChatPost;

/**
 * Created by vladshostak on 12/26/17.
 */

public class AddChatPostsViewModel extends AndroidViewModel{

    private AppDatabase appDatabase;

    public AddChatPostsViewModel(Application application) {
        super(application);
        // get instance of our db
        appDatabase = AppDatabase.getDatabase(this.getApplication());
    }

    // create asyncTask sicne doesnt use liveData
    public void addChatPosts(ChatPost... chatPost) {
        new AddChatPostsAsyncTask(appDatabase).execute(chatPost);
    }
    private static class AddChatPostsAsyncTask extends AsyncTask<ChatPost, Void, Void> {
        private AppDatabase appDb;
        AddChatPostsAsyncTask(AppDatabase appDatabase) {
            appDb = appDatabase;
        }

        @Override
        protected Void doInBackground(ChatPost... params) {
            appDb.chatPostDao().insertChatPosts(params);
            return null;
        }
    }

}
