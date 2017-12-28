package io.github.veeshostak.aichat.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import io.github.veeshostak.aichat.database.AppDatabase;
import io.github.veeshostak.aichat.database.dao.ChatPostDao;
import io.github.veeshostak.aichat.database.entity.ChatPost;

/**
 * Created by vladshostak on 12/26/17.
 */

/*

ViewModels do not contain code related to the UI. This helps in the decoupling of our app components.
In Room, the database instance should ideally be contained in a ViewModel rather than on the Activity/Fragment.

ViewModels are entities that are free of the Activity/Fragment lifecycle.
For example, they can retain their state/data even during an orientation change.
*/

// If your ViewModel needs the application context, it must extend AndroidViewModel, else extend ViewModel
public class ListAllChatPostsViewModel extends AndroidViewModel {

    private LiveData<List<ChatPost>> chatPostList;

    private AppDatabase appDatabase;

    public ListAllChatPostsViewModel(Application application) {
        super(application);
        // get instance of our db
        appDatabase = AppDatabase.getDatabase(this.getApplication());
        // Set ChatPosts. Live Data runs the query asynchronously on a background thread
        chatPostList = appDatabase.chatPostDao().getAllChatPosts();
    }

    // Get chatPosts. Live Data runs the query asynchronously on a background thread
    public LiveData<List<ChatPost>> getData() {
        if (chatPostList == null) {
            chatPostList = appDatabase.chatPostDao().getAllChatPosts();
        }
        return chatPostList;
    }


    // delete doesnt use liveData, execute in another thread. (Live Data runs the query asynchronously on a background thread when needed)
    public void deleteItem(ChatPost toDeleteChatPost) {
        new DeleteAsyncTask(appDatabase).execute(toDeleteChatPost);
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

