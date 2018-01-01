package io.github.veeshostak.aichat.activities;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import io.github.veeshostak.aichat.aws.cognito.CognitoHelper;
import io.github.veeshostak.aichat.database.entity.ChatPost;
import io.github.veeshostak.aichat.models.ChatMessage;
import io.github.veeshostak.aichat.aws.dynamodb.DynamoDBClientAndMapper;
import io.github.veeshostak.aichat.recyclerview.adapter.MessageListAdapter;
import io.github.veeshostak.aichat.utils.Installation;
import io.github.veeshostak.aichat.R;
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
import java.util.HashMap;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;


// for TTS: implement AIListener
public class ChatListActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ChatActivity";
    //private final int REQUEST_INTERNET = 1;

    // when pushing to DynamoDb, push list of String messages prefixed accordingly
    private static final String USER_QUERY_PREFIX = "u:";
    private static final String MACHINE_RESPONSE_PREFIX = "m:";

    public static final String PREFS_IS_FIRST_TIME = "isFirstTime";

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
    private CognitoCachingCredentialsProvider credentialsProvider; // uses shared prefs to cache creds

    // shared prefs
    private SharedPreferences isFirstTimeSharedPref;


    // ================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        final AIConfiguration config = new AIConfiguration(getString(R.string.dialog_dlow_client_token),
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        /*
        // permissions for listening
        if (ContextCompat.checkSelfPermission(ChatListActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ChatListActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_INTERNET);
        }

        aiService = AIService.getService(this, config);
        aiService.setListener(this);
        */

        aiDataService = new AIDataService(this, config);

        initControls();

        // observe all chatPosts
        listAllChatPostsViewModel.getData().observe(ChatListActivity.this, new Observer<List<ChatPost>>() {
            @Override
            public void onChanged(@Nullable List<ChatPost> chatPosts) {
                //recyclerViewAdapter.addItems(chatPost);
                allLocalChatPosts = new ArrayList<>(chatPosts);
                mMessageAdapter.addItems(fromChatPostListToChatMessageList(allLocalChatPosts));
                scroll();
            }
        });

        // observe chatPosts that are not in remote db (have not benn pushed back)
        listNotInRemoteChatPostsViewModel.getData().observe(ChatListActivity.this, new Observer<List<ChatPost>>() {
            @Override
            public void onChanged(@Nullable List<ChatPost> chatPosts) {

                allLocalNotInRemoteChatPosts = new ArrayList<>(chatPosts);

                if (allLocalNotInRemoteChatPosts != null && !allLocalNotInRemoteChatPosts.isEmpty()) {
                    // START: check to see if conversation has 5*2=10 messages, if yes push the messages and mark as pushed
                    if (allLocalNotInRemoteChatPosts.size() >= 5) {
                        addToRemoteDbHelper();
                    }
                    // END: check to see if conversation has 5*2=10 messages, if yes push the messages and mark as pushed
                }
            }
        });

        // START: Determine if it's the user's first time opening the app, and just welcome or welcome them back accordingly
        Boolean firstTime = isFirstTimeSharedPref.getBoolean("firstTime", true);

        // the user is back to using the app, welcome them back! or just welcome new user if it's their first time
        selectWelcomeChatMessage(firstTime);

        SharedPreferences.Editor editor = isFirstTimeSharedPref.edit();
        editor.putBoolean("firstTime", false);
        editor.apply();
        // END: Determine if it's the user's first time openeing the app, and just welcome or welcome them back accordingly
    }

    private void initControls() {

        userMessage = (EditText) findViewById(R.id.userMessageField);
        sendBtn = (Button) findViewById(R.id.sendMessageButton);
        sendBtn.setOnClickListener(this);

        // get reference to rec view
        mMessageRecycler = (RecyclerView) findViewById(R.id.reyclerview_message_list);

        // choose layOutManager
        mManager = new LinearLayoutManager(this);
        //mManager.setReverseLayout(true);
        //mManager.setStackFromEnd(true);
        mMessageRecycler.setLayoutManager(mManager);

        // populate adapter with dataSource
        mMessageAdapter = new MessageListAdapter(this, fromChatPostListToChatMessageList(allLocalChatPosts));
        mMessageRecycler.setLayoutManager(mManager);

        // set adapter
        mMessageRecycler.setAdapter(mMessageAdapter);

        // START: scroll to end when keyboard opens
        mMessageRecycler.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v,
                                       int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    mMessageRecycler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mMessageRecycler.getAdapter().getItemCount() > 0) {
                                mMessageRecycler.smoothScrollToPosition(
                                        mMessageRecycler.getAdapter().getItemCount() - 1);
                            }
                        }
                    }, 100);
                }
            }
        });
        // END: scroll to end when keyboard opens


        // retrieve ID from file
        Installation GetId = new Installation();
        uniqueId = GetId.id(getApplicationContext());


        listAllChatPostsViewModel = ViewModelProviders.of(this).get(ListAllChatPostsViewModel.class);
        listNotInRemoteChatPostsViewModel = ViewModelProviders.of(this).get(ListNotInRemoteChatPostsViewModel.class);
        addChatPostsViewModel = ViewModelProviders.of(this).get(AddChatPostsViewModel.class);
        deleteAllChatPostsViewModel = ViewModelProviders.of(this).get(DeleteAllChatPostsViewModel.class);
        updateChatPostsViewModel = ViewModelProviders.of(this).get(UpdateChatPostsViewModel.class);

        Context context = this;
        isFirstTimeSharedPref = context.getSharedPreferences(PREFS_IS_FIRST_TIME, Context.MODE_PRIVATE);

        // START: get AWS credentials to access AWS resources

        CognitoUserSession cognitoUserSession = CognitoHelper.getCurrSession();
        // Get id token from CognitoUserSession.
        String idToken = cognitoUserSession.getIdToken().getJWTToken();

        // Create a credentials provider, or use the existing provider.
        credentialsProvider = new CognitoCachingCredentialsProvider(context, getApplicationContext().getString(R.string.cognito_identity_pool_id), CognitoHelper.getCognitoRegion());

        // Set up as a credentials provider.
        Map<String, String> logins = new HashMap<String, String>();
        logins.put(getApplicationContext().getString(R.string.cognito_login), idToken);
        credentialsProvider.setLogins(logins);
        // END: get AWS credentials to access AWS resources

        DynamoDBClientAndMapper dynamoDb = new DynamoDBClientAndMapper(getApplicationContext(), credentialsProvider);
        mapper = dynamoDb.getMapper();



    }

    /*
     Use Weak Reference b/c:
     The inner class needs to be accessing the outside class during its entire lifetime.
     What happens when the Activity is destroyed? The AsyncTask is holding a reference to the
     Activity, and the Activity cannot be collected by the GC. We get a memory leak.

     a weak reference is a reference not strong enough to keep the object in memory, the object
     will be garbage-collected.
     When the Activity stops existing, since it is hold through a WeakReference, it can be collected.

     the Activity within the inner class is now referenced as WeakReference<ChatListActivity> mainActivity;
     */
    private static class TaskAddChatPostsToRemoteDb extends AsyncTask<String, Void, AmazonClientException> {

        private WeakReference<ChatListActivity> activityReference; // weak ref to our ChatListActivity.java
        private ArrayList<ChatPost> chatPostsToAdd;

        // only retain a weak reference to the activity
        private TaskAddChatPostsToRemoteDb(ChatListActivity context, ArrayList<ChatPost> chatPostsToAdd) {
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
                String conversationDateTime = DateFormat.getDateTimeInstance().format(new Date());

                // create chatPostId
                String conversationId = createUniqueId();
                // add the conversation to conversation table
                UserConversation conversation = new UserConversation();
                conversation.setConversationId(conversationId);
                conversation.setConversationDateTime(conversationDateTime);

                //conversation.setUserId(activityReference.get().uniqueId);

                // username is uuid geneated by aws cognito that is mapped to user's email and pass
                conversation.setUserId(CognitoHelper.getCurrSession().getUsername());


                // create chatpost list of strings to push to NoSql db
                ArrayList<String> tempChatPostsStringList = new ArrayList<String>();
                for (ChatPost chatPost : chatPostsToAdd) {
                    tempChatPostsStringList.add(USER_QUERY_PREFIX + chatPost.getUserQuery());
                    tempChatPostsStringList.add(MACHINE_RESPONSE_PREFIX + chatPost.getResponse());
                }
                conversation.setConversation(tempChatPostsStringList);
                // END: add conversation to User-Conversations Table

                // START: add conversationId to this User in the User Table
                /*
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

                */
                // END: add conversationId to this User in the User Table

                // save user with the updated conversationId
                // activityReference.get().mapper.save(selectedUser);

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

        private void onResult() {

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

        }
        private void onError(AmazonClientException ace) {

            Log.d(TAG, ace.toString());
            Toast.makeText(activityReference.get().getApplicationContext(),
                    "Internal error occurred communicating with DynamoDB\n" + "Error Message:  " + ace.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        private String createUniqueId() {
            // returns: 10/23/17 8:22AM
            String currentDateTimeString = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US).format(new Date());

            // format to 102317822AM
            currentDateTimeString = currentDateTimeString.replace("/", "").replace(" ", "").replace(":", "");

            // uuid + datetime
            String id = UUID.randomUUID().toString() + currentDateTimeString;

            return id;
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


    public void addToRemoteDbHelper() {

            // push them back to remoteDb.
            // then set pushedToRemoteDb value of each retrieved chatPost to true and run update transaction
            if (allLocalNotInRemoteChatPosts != null && !allLocalNotInRemoteChatPosts.isEmpty()) {
                // update db with stored chat history, on success update local (set pushedToRemoteDb to true)
                new TaskAddChatPostsToRemoteDb(this, allLocalNotInRemoteChatPosts).execute();
            }

    }

    public void selectWelcomeChatMessage(Boolean isFirstTime){

        Random r = new Random();
        int randNUm = r.nextInt(6 - 1) + 1; // 1-5

        String userQuery;
        String response;

        if (!isFirstTime) {
            switch (randNUm) {
                case 1:
                    userQuery = "Ahoy!";
                    response = "Hi! I'm Fiona. I’m sure we’ll get on really well.";
                    break;
                case 2:
                    userQuery = "I have returned!";
                    response = "Hello and welcome!";
                    break;
                case 3:
                    userQuery = "Ahoy! I'm back!";
                    response = "I was so bored. Thank you for saving me!";
                    break;
                case 4:
                    userQuery = "Hello! I'm back!";
                    response = "Great to see you! I was so bored.";
                    break;
                case 5:
                    userQuery = "I'm back!";
                    response = "Look who it is!";
                    break;
                default:
                    userQuery = "I'm back!";
                    response = "Hi! I'm Fiona";
            }

        } else {
            userQuery = "Hello Fiona!";
            response = "Welcome! I’m excited to meet you!";
        }

        ChatPost chatPost = new ChatPost();
        chatPost.setCreatedAt(DateFormat.getDateTimeInstance().format(new Date()));
        chatPost.setUserQuery(userQuery);
        chatPost.setResponse(response);
        chatPost.setPushedToRemoteDb(false);

        addChatPostsViewModel.addChatPosts(chatPost);
    }

    public void scroll() {
        if (mMessageRecycler.getAdapter().getItemCount() > 0) {
            mMessageRecycler.smoothScrollToPosition(
                    mMessageRecycler.getAdapter().getItemCount() - 1);
        }
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
            startActivity(new Intent(ChatListActivity.this, TermsOfService.class));
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

            // not needed since we are observing live data, but to display right away we can call it now
            // since live data will only be updated when we have full ChatPost with response in AsyncTask
            // display message in RecyclerView
            // START: quickly display userQuery in RecyclerView
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMessage(messageText);
            chatMessage.setCreatedAt(DateFormat.getDateTimeInstance().format(new Date()));
            chatMessage.setIsMe(true);
            mMessageAdapter.add(chatMessage); // display in RecyclerView while waiting for response
            // END: quickly display userQuery in RecyclerView

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
    private static class TaskSendUserQueryToDialogFlow extends AsyncTask<String, Void, AIResponse> {

        private WeakReference<ChatListActivity> activityReference; // weak ref to our ChatListActivity.java
        private String userQuery;
        private AIError aiError;

        private TaskSendUserQueryToDialogFlow(ChatListActivity context, String userQuery) {
            activityReference = new WeakReference<ChatListActivity>(context);
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
                return activityReference.get().aiDataService.request(request, requestExtras);
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

        private void onResult(final AIResponse response) {

            Log.d(TAG, "onResult");
            //resultTextView.setText(gson.toJson(response));
            Log.i(TAG, "Received success response");

            // get speech from the result object
            final Result result = response.getResult();
            final String speech = result.getFulfillment().getSpeech();

            //Log.i(TAG, "Speech: " + speech);
            //Toast.makeText(getApplicationContext(),"Speech:" + speech , Toast.LENGTH_LONG).show();

            // add ChatPost to local Room persistent db
            ChatPost mChatPost = new ChatPost();
            mChatPost.setUserQuery(userQuery);
            mChatPost.setResponse(speech);
            mChatPost.setPushedToRemoteDb(false);
            mChatPost.setCreatedAt(DateFormat.getDateTimeInstance().format(new Date()));
            activityReference.get().addChatPostsViewModel.addChatPosts(mChatPost);



        }

        private void onError(final AIError error) {
            Log.d(TAG, error.toString());
            Toast.makeText(activityReference.get().getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
            //resultTextView.setText(error.toString());
        }

    }

    /*
    @Override
    public void onPause() {
        super.onPause();

        // use onPause because:
        // onStop may not always be called in low-memory situations,
        // as well as onDestroy, such as when Android is starved for
        // resources and cannot properly background the Activity.

        //  determine whether the activity is simply pausing or completely finishing.
        if (isFinishing()) {
            // save conversation to the db

        }
    }
    */

}
