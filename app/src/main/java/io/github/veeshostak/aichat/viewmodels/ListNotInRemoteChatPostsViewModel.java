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

public class ListNotInRemoteChatPostsViewModel extends AndroidViewModel {

    private AppDatabase appDatabase;

    private LiveData<List<ChatPost>> notInRemoteChatPostsList;

    public ListNotInRemoteChatPostsViewModel(Application application) {
        super(application);
        // get instance of our db
        appDatabase = AppDatabase.getDatabase(this.getApplication());
        // Set ChatPosts. Live Data runs the query asynchronously on a background thread
        notInRemoteChatPostsList = appDatabase.chatPostDao().getAllChatPostsNotInRemoteDb();
    }
    // Live Data runs the query asynchronously on a background thread
    public LiveData<List<ChatPost>>getAllChatPostsNotInRemoteDb() {
        return  appDatabase.chatPostDao().getAllChatPostsNotInRemoteDb();
    }

    // Get chatPosts. Live Data runs the query asynchronously on a background thread
    public LiveData<List<ChatPost>> getData() {
        if (notInRemoteChatPostsList == null) {
            notInRemoteChatPostsList = appDatabase.chatPostDao().getAllChatPostsNotInRemoteDb();
        }
        return notInRemoteChatPostsList;
    }


    // delete doesnt use liveData, execute in another thread. (Live Data runs the query asynchronously on a background thread when needed)
    public void deleteItem(ChatPost toDeleteChatPost) {
        new ListNotInRemoteChatPostsViewModel.DeleteAsyncTask(appDatabase).execute(toDeleteChatPost);
    }
    private static class DeleteAsyncTask extends AsyncTask<ChatPost, Void, Void> {
        private AppDatabase appDb;
        DeleteAsyncTask(AppDatabase appDatabase) {
            appDb = appDatabase;
        }
        @Override
        protected Void doInBackground(final ChatPost... params) {
            appDb.chatPostDao().deleteChatPosts(params[0]);
            return null;
        }
    }


}
