package io.github.veeshostak.aichat.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.List;

import io.github.veeshostak.aichat.database.AppDatabase;
import io.github.veeshostak.aichat.database.entity.ChatPost;

/**
 * Created by vladshostak on 12/26/17.
 */

public class DeleteAllChatPostsViewModel extends AndroidViewModel {

    private AppDatabase appDatabase;

    public DeleteAllChatPostsViewModel(Application application) {
        super(application);
        // get instance of our db
        appDatabase = AppDatabase.getDatabase(this.getApplication());
    }
    // create asyncTask since doesnt use live data.
    public void deleteAllChatPosts() {
        new DeleteAllChatPostsAsyncTask(appDatabase).execute();
    }
    private static class DeleteAllChatPostsAsyncTask extends AsyncTask<Void, Void, Void> {
        private AppDatabase appDb;
        DeleteAllChatPostsAsyncTask(AppDatabase appDatabase) {
            appDb = appDatabase;
        }
        @Override
        protected Void doInBackground(Void... voids) {
            appDb.chatPostDao().deleteAllChatPosts();
            return null;
        }
    }

}