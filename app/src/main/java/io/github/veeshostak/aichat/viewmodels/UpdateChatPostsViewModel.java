package io.github.veeshostak.aichat.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.os.AsyncTask;

import io.github.veeshostak.aichat.database.AppDatabase;
import io.github.veeshostak.aichat.database.entity.ChatPost;

/**
 * Created by vladshostak on 12/26/17.
 */

public class UpdateChatPostsViewModel extends AndroidViewModel{

    private AppDatabase appDatabase;

    public UpdateChatPostsViewModel(Application application) {
        super(application);
        // get instance of our db
        appDatabase = AppDatabase.getDatabase(this.getApplication());
    }

    // create asyncTask sicne doesnt use liveData
    public void updateChatPosts(ChatPost... chatPost) {
        new UpdateChatPostsAsyncTask(appDatabase).execute(chatPost);
    }
    private static class UpdateChatPostsAsyncTask extends AsyncTask<ChatPost, Void, Void> {
        private AppDatabase appDb;
        UpdateChatPostsAsyncTask(AppDatabase appDatabase) {
            appDb = appDatabase;
        }

        @Override
        protected Void doInBackground(ChatPost... params) {
            appDb.chatPostDao().updateChatPosts(params);
            return null;
        }
    }

}