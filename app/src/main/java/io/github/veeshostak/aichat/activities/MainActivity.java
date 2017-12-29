package io.github.veeshostak.aichat.activities;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

// start api.ai imports

import ai.api.AIServiceException;
import ai.api.RequestExtras;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
//import ai.api.AIListener;
//import ai.api.android.AIService;

import ai.api.model.AIContext;
import ai.api.model.AIError;
import ai.api.model.AIEvent;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import io.github.veeshostak.aichat.adapters.ChatAdapter;
import io.github.veeshostak.aichat.database.entity.ChatPost;
import io.github.veeshostak.aichat.models.ChatMessage;
import io.github.veeshostak.aichat.aws.dynamodb.DynamoDBClientAndMapper;
import io.github.veeshostak.aichat.recyclerview.adapter.MessageListAdapter;
import io.github.veeshostak.aichat.utils.Installation;
import io.github.veeshostak.aichat.R;
import io.github.veeshostak.aichat.aws.dynamodb.model.User;
import io.github.veeshostak.aichat.aws.dynamodb.model.UserConversation;
import io.github.veeshostak.aichat.viewmodels.AddChatPostsViewModel;
import io.github.veeshostak.aichat.viewmodels.DeleteAllChatPostsViewModel;
import io.github.veeshostak.aichat.viewmodels.ListAllChatPostsViewModel;
import io.github.veeshostak.aichat.viewmodels.ListNotInRemoteChatPostsViewModel;
import io.github.veeshostak.aichat.viewmodels.UpdateChatPostsViewModel;
// end api.ai imports


import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;


import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;


import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;


// for TTS: implement AIListener
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ChatActivity";
    //private final int REQUEST_INTERNET = 1;

    // when pushing to DynamoDb, push list of String messages prefixed accordingly
    private static final String USER_QUERY_PREFIX = "u:";
    private static final String MACHINE_RESPONSE_PREFIX = "m:";

    // ViewModels with Room Persistent DB
    private AddChatPostsViewModel addChatPostsViewModel;
    private ListAllChatPostsViewModel listAllChatPostsViewModel;
    private ListNotInRemoteChatPostsViewModel listNotInRemoteChatPostsViewModel;
    private UpdateChatPostsViewModel updateChatPostsViewModel;
    private DeleteAllChatPostsViewModel deleteAllChatPostsViewModel;

    // all local chatPosts from local Room persistent db
    ArrayList<ChatPost> allLocalChatPosts;
    ArrayList<ChatPost> allLocalNotInRemoteChatPosts;

    // user id
    private String uniqueId;

    private EditText userMessage;
    private Button sendBtn;

    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    private LinearLayoutManager mManager;




    // Dialogflow vars
    //private AIService aiService;
    private AIDataService aiDataService;

    //aws
    private AmazonDynamoDBClient ddbClient;
    private DynamoDBMapper mapper;

    // ================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        final AIConfiguration config = new AIConfiguration(getString(R.string.dialog_dlow_client_token),
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);


        /*
        // permissions for listening
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_INTERNET);
        }

        aiService = AIService.getService(this, config);
        aiService.setListener(this);
        */

        aiDataService = new AIDataService(this, config);

        initControls();
    }

    private void initControls() {


        userMessage = (EditText) findViewById(R.id.userMessageField);
        sendBtn = (Button) findViewById(R.id.sendMessageButton);
        sendBtn.setOnClickListener(this);

        // get reference to rec view
        mMessageRecycler = (RecyclerView) findViewById(R.id.reyclerview_message_list);

        // choose layOutManager, reverse layout
        mManager = new LinearLayoutManager(this);
        //mManager.setReverseLayout(true);
        //mManager.setStackFromEnd(true);
        mMessageRecycler.setLayoutManager(mManager);

        // populate adapter with dataSource
        mMessageAdapter = new MessageListAdapter(this, fromChatPostListToChatMessageList(allLocalChatPosts));
        mMessageRecycler.setLayoutManager(mManager);

        // set adapter
        mMessageRecycler.setAdapter(mMessageAdapter);



        // retrieve ID from signInActivity
//        Bundle extras = getIntent().getExtras();
//        if (extras != null) {
//            uniqueId = extras.getString("UNIQUE_ID");
//        }

        // retrieve ID from file
        Installation GetId = new Installation();
        uniqueId = GetId.id(getApplicationContext());

        DynamoDBClientAndMapper dynamoDb = new DynamoDBClientAndMapper(getApplicationContext());
        mapper = dynamoDb.getMapper();

        // START Create local Room Persistent Db
//        localDbChatPosts = Room.databaseBuilder(getApplicationContext(),
//                AppDatabase.class, "user-chat-posts").build();
        // END Create local Room Persistent Db

        listAllChatPostsViewModel = ViewModelProviders.of(this).get(ListAllChatPostsViewModel.class);
        listNotInRemoteChatPostsViewModel = ViewModelProviders.of(this).get(ListNotInRemoteChatPostsViewModel.class);
        addChatPostsViewModel = ViewModelProviders.of(this).get(AddChatPostsViewModel.class);
        deleteAllChatPostsViewModel = ViewModelProviders.of(this).get(DeleteAllChatPostsViewModel.class);
        updateChatPostsViewModel = ViewModelProviders.of(this).get(UpdateChatPostsViewModel.class);




        // TODO: ======== INIT Method finishes Here ===================

        // observe all chatPosts
        listAllChatPostsViewModel.getData().observe(MainActivity.this, new Observer<List<ChatPost>>() {
            @Override
            public void onChanged(@Nullable List<ChatPost> chatPosts) {
                //recyclerViewAdapter.addItems(chatPost);
                allLocalChatPosts = new ArrayList<>(chatPosts);
                mMessageAdapter.addItems(fromChatPostListToChatMessageList(allLocalChatPosts));
            }
        });

        // observe chatPosts that are not in remote db (have not benn pushed back)
        listNotInRemoteChatPostsViewModel.getData().observe(MainActivity.this, new Observer<List<ChatPost>>() {
            @Override
            public void onChanged(@Nullable List<ChatPost> chatPosts) {
                allLocalNotInRemoteChatPosts = new ArrayList<>(chatPosts);
            }
        });

        // TODO: add a welcome message on app start to also trigger onChange


        // START: check to see if there are chatPosts that need to be pushed to RemoteDb, push them and mark as pushed
        // if they exist, push them back to remoteDb. else do nothing.
        // then set pushedToRemoteDb value of each retrieved chatPost to true and run update transaction

        //ArrayList<ChatPost> chatPostsToPush = new ArrayList<ChatPost>(listNotInRemoteChatPostsViewModel.getAllChatPostsNotInRemoteDb());
        if (allLocalNotInRemoteChatPosts != null && !allLocalNotInRemoteChatPosts.isEmpty()) {
            // update db with stored chat history, on success update local (set pushedToRemoteDb to true)
            new TaskAddChatPostsToRemoteDb(this, allLocalNotInRemoteChatPosts).execute();
        }
        // if they do not exist, do nothing
        // END: check to see if there are chatPosts that need to be pushed to RemoteDb, push them and mark as pushed

        // START: get all ChatPosts from local Room persistent Db and display in RecyclerView
        // get all chat posts from local Room persistent db, shallow copy

        //allLocalChatPosts = new ArrayList<ChatPost>(listAllChatPostsViewModel.getData());
        if (allLocalChatPosts == null || allLocalChatPosts.isEmpty()) {
            // empty, so select random welcome message and add to RecyclerView
            // selectWelcomeChatMessage();

        }
        /*
        else {
            // not needed since observing liveData
            // display all chat posts

            for (ChatPost chatPost : allLocalChatPosts) {
                ChatMessage tempMsgMe = new ChatMessage();
                ChatMessage tempMsgMachine = new ChatMessage();

                tempMsgMe.setIsMe(true);
                tempMsgMe.setMessage(chatPost.getUserQuery());
                tempMsgMe.setCreatedAt(chatPost.getCreatedAt());

                tempMsgMachine.setIsMe(false);
                tempMsgMachine.setMessage(chatPost.getResponse());
                tempMsgMachine.setCreatedAt(chatPost.getCreatedAt());

                displayMessage(tempMsgMe);
                displayMessage(tempMsgMachine);


            }
        }
        */
        // END: get all ChatPosts from local Room persistent Db and display in RecyclerView

    }




    /*
     Use Weak Reference b/c:
     The inner class needs to be accessing the outside class during its entire lifetime.
     What happens when the Activity is destroyed? The AsyncTask is holding a reference to the
     Activity, and the Activity cannot be collected by the GC. We get a memory leak.

     a weak reference is a reference not strong enough to keep the object in memory, the object
     will be garbage-collected.
     When the Activity stops existing, since it is hold through a WeakReference, it can be collected.

     the Activity within the inner class is now referenced as WeakReference<MainActivity> mainActivity;
     */
    private static class TaskAddChatPostsToRemoteDb extends AsyncTask<String, Void, AmazonClientException> {

        private WeakReference<MainActivity> activityReference; // weak ref to our MainActivity.java
        private ArrayList<ChatPost> chatPostsToAdd;

        // only retain a weak reference to the activity
        TaskAddChatPostsToRemoteDb(MainActivity context, ArrayList<ChatPost> chatPostsToAdd) {
            activityReference = new WeakReference<>(context);
            this.chatPostsToAdd = new ArrayList<>(chatPostsToAdd);
        }

        // can pass params in .execute(Params...)

        @Override
        protected AmazonClientException doInBackground(final String... params) {

            // access params
            // params[0]

            try {

                // START: add conversation to User-Conversations Table
                String conversationDateTime = DateFormat.getDateTimeInstance().format(new Date()); // todo: get UTC time from server

                // create chatPostId
                String conversationId = UUID.randomUUID().toString();
                // add the conversation to conversation table
                UserConversation conversation = new UserConversation();
                conversation.setConversationId(conversationId);
                conversation.setConversationDateTime(conversationDateTime);

                conversation.setUserId(activityReference.get().uniqueId);

                // create chatpost list of strings to push to NoSql db
                ArrayList<String> tempChatPostsStringList = new ArrayList<String>();
                for (ChatPost chatPost : chatPostsToAdd) {
                    tempChatPostsStringList.add(USER_QUERY_PREFIX + chatPost.getUserQuery());
                    tempChatPostsStringList.add(MACHINE_RESPONSE_PREFIX + chatPost.getResponse());
                }
                conversation.setConversation(tempChatPostsStringList);
                // END: add conversation to User-Conversations Table

                // START: add conversationId to this User in the User Table
                // (User has many conversations: @One To Many)

                HashSet<String> conversationIds; // to obtain stored conversations ids
                // get user with Id PK
                User selectedUser = activityReference.get().mapper.load(User.class, activityReference.get().uniqueId);


                // get existing conversationsIds from user, if they exist
                if (selectedUser != null && selectedUser.getConversationIds() != null && !selectedUser.getConversationIds().isEmpty() ) {
                    // they exist, shallow copy them
                    conversationIds = new HashSet<String>(selectedUser.getConversationIds());
                } else {
                    // they DNE, initialize empty hashSet, we will add the current conversationId to  it
                    conversationIds = new HashSet<String>();
                }
                // add new conversation id to hashSet
                conversationIds.add(conversationId);
                // add updated hashSet to our loaded user

                selectedUser.setConversationIds(conversationIds);


                // END: add conversationId to this User in the User Table

                // save user with the updated conversationId
                activityReference.get().mapper.save(selectedUser);
                // save conversation
                activityReference.get().mapper.save(conversation);

            } catch (AmazonClientException ace) {
                return ace;
            }

            return null;

        }

        @Override
        protected void onPostExecute(AmazonClientException ace) {
            if (ace == null) {
                onResult();

            } else {
                onError(ace);
            }
        }

        public Void onResult() {

            // START set pushedToRemoteDb boolean value of each retrieved chatPost to true, update local Room Db

            // note: since we might have had encountered an error when pushing to db (chatPosts wouldn't be pushed),
            // we cannot set pushedToRemoteDb to true when converting chatPosts to a temp chatPostsList of Strings. (to avoid iterating again)
            // (in case of an error we would be in onError() and then next time we will try to push again so we cannot mark it as pushed b4 hand)

            for (ChatPost chatPost: chatPostsToAdd) {
                chatPost.setPushedToRemoteDb(true);
            }
            // run update transaction on local room persistent db
            // note: pass in toArray(new ChatPost[0]) instead of toArray(new ChatPost[chatPostsToAdd.size()]) due to JVM optimizations
            activityReference.get().updateChatPostsViewModel.updateChatPosts((chatPostsToAdd.toArray(new ChatPost[0])));
            // END set pushedToRemoteDb boolean value of each retrieved chatPost to true, update local Room Db
            return null;
        }
        public Void onError(AmazonClientException ace) {

            Log.d(TAG, ace.toString());
            Toast.makeText(activityReference.get().getApplicationContext(),
                    "Internal error occurred communicating with DynamoDB\n" + "Error Message:  " + ace.getMessage(),
                    Toast.LENGTH_LONG).show();
            return null;
        }
    }

    public List<ChatMessage> fromChatPostListToChatMessageList(List<ChatPost> chatPosts) {

        if (chatPosts == null ) {
            return null;
        }

        List<ChatMessage> mMessageList = new ArrayList<>();

        for(ChatPost chatPost: chatPosts) {

            ChatMessage tempMsgMe = new ChatMessage();
            ChatMessage tempMsgMachine = new ChatMessage();

            tempMsgMe.setIsMe(true);
            tempMsgMe.setMessage(chatPost.getUserQuery());
            tempMsgMe.setCreatedAt(chatPost.getCreatedAt());

            tempMsgMachine.setIsMe(false);
            tempMsgMachine.setMessage(chatPost.getResponse());
            tempMsgMachine.setCreatedAt(chatPost.getCreatedAt());

            mMessageList.add(tempMsgMe);
            mMessageList.add(tempMsgMachine);
        }
        return mMessageList;

    }

    /*
    public void displayMessage(ChatMessage message) {
        mMessageAdapter.add(message);
        mMessageAdapter.notifyDataSetChanged();
        //scroll();
    }
    */

    private void selectWelcomeChatMessage(){
        // todo: implement. randomly select random greeting message
        int unicode = 0x1F60A;
        String smile = new String(Character.toChars(unicode));

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        if (id == R.id.action_terms) {
            // show terms of service
            startActivity(new Intent(MainActivity.this, TermsOfService.class));
            return true;
        } else if (id == R.id.action_delete_chat) {
            // START: check to see if there are chatPosts that need to be pushed to RemoteDb, push them and mark as pushed
            // if they exist, push them back to remoteDb. else do nothing.
            // then set pushedToRemoteDb value of each retrieved chatPost to true and run update transaction

            ArrayList<ChatPost> chatPostsToPush = new ArrayList<ChatPost>(allLocalNotInRemoteChatPosts);
            if (chatPostsToPush != null && !chatPostsToPush.isEmpty()) {
                // update db with stored chat history, on success update local (set pushedToRemoteDb to true)
                new TaskAddChatPostsToRemoteDb(this, chatPostsToPush).execute();
            }
            // if they do not exist, do nothing
            // END: check to see if there are chatPosts that need to be pushed to RemoteDb, push them and mark as pushed

            // clear chatPosts from local Room persisted db
            deleteAllChatPostsViewModel.deleteAllChatPosts();

            // RecyclerView is cleared since we are obesrving livedata
            // clear RecyclerView
            // mMessageAdapter.clearMessages();
            // mMessageAdapter.notifyDataSetChanged();

            // select welcome message to add
            //selectWelcomeChatMessage();



            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.sendMessageButton) {

            String messageText = userMessage.getText().toString().trim();

            if (TextUtils.isEmpty(messageText)) {
                return;
            }
            userMessage.setText(""); // clear message field

            /*
            // Not needed since we are observing liveData
            // START: display userQuery in RecyclerView
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMessage(messageText);
            chatMessage.setIsMe(true);

            displayMessage(chatMessage); // display in RecyclerView
            // END: display userQuery in RecyclerView
            */

            // START: send userQuery to dialogFlow API, once obtain response: add to RecyclerView, add chatPost to local Room db
            // pass params
            final String eventString = null;
            final String contextString = null;
            new TaskSendUserQueryToDialogFlow(this, messageText).execute(eventString, contextString);
            // END: send userQuery to dialogFlow API, once obtain response: add to RecyclerView, add chatPost to local Room db

            //aiService.startListening();
        }
    }

    /*
    // If implements AIListener (for tts)
    // start Dialogflow listening
    @Override
    public void onListeningStarted() {}

    @Override
    public void onListeningCanceled() {}

    @Override
    public void onListeningFinished() {}

    @Override
    public void onResult(AIResponse result) {

    }

    @Override
    public void onError(AIError error) {

    }

    @Override
    public void onAudioLevel(final float level) {}
    // end Dialogflow listening
    */


    /*
      AIRequest should have query OR event
      sends request with userQuery to dialogFlow API, once obtain response: add to RecyclerView, add chatPost to local Room db
    */
    private class TaskSendUserQueryToDialogFlow extends AsyncTask<String, Void, AIResponse> {

        private WeakReference<MainActivity> activityReference; // weak ref to our MainActivity.java
        private String userQuery;
        private AIError aiError;

        public TaskSendUserQueryToDialogFlow(MainActivity context, String userQuery) {
            activityReference = new WeakReference<MainActivity>(context);
            this.userQuery = userQuery;
        }

        @Override
        protected AIResponse doInBackground(final String... params) {

            final AIRequest request = new AIRequest();
            String query = userQuery;
            String event = params[0];

            // set request variables
            if (!TextUtils.isEmpty(query))
                request.setQuery(query);
            if (!TextUtils.isEmpty(event))
                request.setEvent(new AIEvent(event));

            final String contextString = params[1];
            RequestExtras requestExtras = null;

            // specify additional contexts in the query using RequestExtras object
            if (!TextUtils.isEmpty(contextString)) {
                final List<AIContext> contexts = Collections.singletonList(new AIContext(contextString));
                requestExtras = new RequestExtras(contexts, null);
            }

            try {
                // aiDataService returns an AIResponse
                return aiDataService.request(request, requestExtras);
            } catch (final AIServiceException e) {
                aiError = new AIError(e);
                return null;
            }

        }

        @Override
        protected void onPostExecute(final AIResponse response) {
            if (response != null) {
                onResult(response);
            } else {
                onError(aiError);
            }
        }

        public void onResult(final AIResponse response) {

            // here runOnUiThread is not needed since onPostExecute is invoked on the UI thread. (just some info)
            // You have to use runOnUiThread() when you want to update your UI from a Non-UI Thread.
            // ex: If you want to update your UI from a background Thread. You can also use Handler for the same thing.

            // Runs the specified action on the UI thread. If the current thread is the UI thread, then
            // the action is executed immediately. If the current thread is not the UI thread, the action
            // is posted to the event queue of the UI thread.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Log.d(TAG, "onResult");
                    //resultTextView.setText(gson.toJson(response));
                    Log.i(TAG, "Received success response");

                    // get speech from the result object
                    final Result result = response.getResult();
                    final String speech = result.getFulfillment().getSpeech();

                    //Log.i(TAG, "Speech: " + speech);
                    //Toast.makeText(getApplicationContext(),"Speech:" + speech , Toast.LENGTH_LONG).show();

                    // not needed since we are observing live data, but to display right away we can call it now
                    // since live data will only be updated when we have full ChatPost with response in AsyncTask
                    // display message in RecyclerView

                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.setMessage(speech);
                    chatMessage.setIsMe(false);
                    chatMessage.setCreatedAt(DateFormat.getDateTimeInstance().format(new Date()));
                    mMessageAdapter.add(chatMessage);


                    // add ChatPost to local Room persistent db
                    ChatPost mChatPost = new ChatPost();
                    mChatPost.setUserQuery(userQuery);
                    mChatPost.setResponse(speech);
                    mChatPost.setPushedToRemoteDb(false);
                    mChatPost.setCreatedAt(DateFormat.getDateTimeInstance().format(new Date()));
                    addChatPostsViewModel.addChatPosts(mChatPost);
                }

            });

        }

        public void onError(final AIError error) {
            Log.d(TAG, error.toString());
            Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
            //resultTextView.setText(error.toString());
        }

    }


//    @Override
//    public void onPause() {
//        super.onPause();
//
//        // use onPause because:
//        // onStop may not always be called in low-memory situations,
//        // as well as onDestroy, such as when Android is starved for
//        // resources and cannot properly background the Activity.
//
//        //  determine whether the activity is simply pausing or completely finishing.
//        if (isFinishing()) {
//            // save conversation to the db
//
//        }
//
//    }


}
