package io.github.veeshostak.aichat.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

// start api.ai imports
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.RequestExtras;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.android.AIService;

import ai.api.model.AIContext;
import ai.api.model.AIError;
import ai.api.model.AIEvent;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import io.github.veeshostak.aichat.adapters.ChatAdapter;
import io.github.veeshostak.aichat.models.ChatMessage;
import io.github.veeshostak.aichat.network.DynamoDBClientAndMapper;
import io.github.veeshostak.aichat.utils.Installation;
import io.github.veeshostak.aichat.R;
import io.github.veeshostak.aichat.models.User;
import io.github.veeshostak.aichat.models.UserConversation;
// end api.ai imports


import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


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


import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;



public class MainActivity extends AppCompatActivity implements AIListener, View.OnClickListener {

    private static final String TAG = "ChatActivity";
    private final int REQUEST_INTERNET = 1;

    public static final String DB_CHAT_HISTORY = "dbChatHistory";
    public static final String LOCAL_CHAT_HISTORY = "localChatHistory";


    private String uniqueId;

    private EditText userMessage;
    private ListView messagesContainer;
    private Button sendBtn;

    private ChatAdapter adapter;
    private ArrayList<ChatMessage> chatHistory;

    private AIService aiService;
    private AIDataService aiDataService;

    //aws
    private AmazonDynamoDBClient ddbClient;
    private DynamoDBMapper mapper;

    // push back conversation to dynamodb.
    // each string in List is prefixed with
    // u: or m: for user or machine
    private ArrayList<String> chatHistoryForDb;
    String user = "u:";
    String machine = "m:";

    private SharedPreferences dbSharedPref;
    private SharedPreferences chatHistorySharedPref;

    private boolean clearChatSelected;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        final AIConfiguration config = new AIConfiguration("16ef009735374933b80a27d199edc8de",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        // permissions for listening
//        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_INTERNET);
//        }

//        aiService = AIService.getService(this, config);
//        aiService.setListener(this);

        aiDataService = new AIDataService(this, config);

        initControls();
    }

    private void initControls() {

        clearChatSelected = false;

        chatHistory = new ArrayList<ChatMessage>();
        chatHistoryForDb = new ArrayList<String>();


        messagesContainer = (ListView) findViewById(R.id.messagesContainer);
        userMessage = (EditText) findViewById(R.id.userMessageField);

        adapter = new ChatAdapter(MainActivity.this, new ArrayList<ChatMessage>());
        messagesContainer.setAdapter(adapter);

        sendBtn = (Button) findViewById(R.id.sendMessageButton);
        sendBtn.setOnClickListener(this);

        TextView machineName = (TextView) findViewById(R.id.machineName);
        machineName.setText("Fiona");
        machineName.setTextColor(Color.BLACK);

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


        // shared prefs for DB and for chat history

        // shared prefs for db:
        // user, has conersation, exits app (convo saved to shared prefs).
        // starts app again-> get the last conversation
        // from sharedPrefs and push to the db.
        // reset shared prefs file

        // shared prefs for chat history:
        // maintain persistence until user clears conversation
        // maintain by: Before delete db shared prefs, copy to chat history prefs


        // open db shared prefs. see if data exists. if exists push it back to db,
        // and clear shared prefs on success


        // TODO: use SQLite instead of shared prefs
        // =====
        // START shard prefs
        Context context = this;
        dbSharedPref = context.getSharedPreferences(DB_CHAT_HISTORY, Context.MODE_PRIVATE);
        chatHistorySharedPref = context.getSharedPreferences(LOCAL_CHAT_HISTORY, Context.MODE_PRIVATE);



        // NOTE: need to guarantee strings (user messages, ai responses) do not contain ;-`;

        // get the last conversation
        // from sharedPrefs and push to the db.
        // reset shared prefs file

        String serialized = dbSharedPref.getString("conversation", null);

        if (serialized != null) {
            // START The user had a new conversation last time, push to db

            List<String> storedDbChatHistory = Arrays.asList(TextUtils.split(serialized, ";-`;"));

            chatHistoryForDb = new ArrayList<String>(storedDbChatHistory);

            if (chatHistoryForDb.size() > 0) {
                // update db with stored chat history
                // reset db prefs
                addConversationToDb();
            }
            // END The user had a new conversation last time, push to db
        } else {

            // START display chatHistorySharedPref conversation

            String serializedLocalHist = chatHistorySharedPref.getString("conversation", null);
            ArrayList<String> tempLocalHistory;

            if (serializedLocalHist != null) {
                // if past convo exists, get it
                List<String> storedDbChatHistory = Arrays.asList(TextUtils.split(serializedLocalHist, ";-`;"));

                if (storedDbChatHistory.size() > 0) {
                    tempLocalHistory = new ArrayList<String>(storedDbChatHistory);
                } else {
                    tempLocalHistory = new ArrayList<String>();
                }




            } else {
                // if past convo does not exists, initialize
                tempLocalHistory = new ArrayList<String>();

            }

            // display persisted conversation to the user
            if (tempLocalHistory.size() > 0) {
                for (String i : tempLocalHistory) {
                    ChatMessage tempMsg = new ChatMessage();

                    // length of i guranteed to be at least 2 (u: or m:)
                    if (i.charAt(0) == 'u') {
                        tempMsg.setMe(true);
                    } else {
                        tempMsg.setMe(false);
                    }
                    tempMsg.setMessage(i.substring(i.indexOf(':') + 1));

                    displayMessage(tempMsg);
                }
            }
            // END display chatHistorySharedPref conversation

        }


        // =====
        // END shared prefs





        // randomly select a welcome message
        //selectWelcomeChatMessage();


    }

    private void addConversationToDb() {
        final AsyncTask<String, Void, Void> taskUpdateDb = new AsyncTask<String, Void, Void>() {


            @Override
            protected Void doInBackground(final String... params) {

                String conversationDateTime = String.valueOf(System.currentTimeMillis() / 100); // get seconds

                // create conversationId
                String conversationId = UUID.randomUUID().toString();

                // add the conversation to conversation table
                UserConversation conversation = new UserConversation();
                conversation.setConversationId(conversationId);
                conversation.setConversationDateTime(conversationDateTime); // todo: use AWS Lambda to set dt

                conversation.setUserId(uniqueId);
                conversation.setConversation(chatHistoryForDb);


                // = START add conversation Id to User Table
                // (User To Conversations: @One To Many)

                HashSet<String> conversationIds; // to obtain stored conversations ids
                User selectedUser = mapper.load(User.class, uniqueId);

                // get existing conversations ids, if they exist
                if (selectedUser.getConversationIds() != null) {
                    conversationIds = new HashSet<String>(selectedUser.getConversationIds());
                } else {
                    conversationIds = new HashSet<String>();
                }
                // add new conversation id
                conversationIds.add(conversationId);

                selectedUser.setConversationIds(conversationIds);

                // = END add conversation Id to User Table

                mapper.save(selectedUser);
                mapper.save(conversation);

                return null;


            }

            @Override
            protected void onPostExecute(Void hi) {

                // maintain persistence by: Before deleting dbSharedPrefs, add the data to chatHistory prefs

                // =====
                // START
                // get past stored conversation
                // to it, add on this conversation, then delete dbArray and dbSharedPrefs
                // =====

                String serializedLocalHist = chatHistorySharedPref.getString("conversation", null);

                ArrayList<String> tempLocalHistory;
                if (serializedLocalHist != null) {
                    // if past convo exists, get it
                    List<String> storedDbChatHistory = Arrays.asList(TextUtils.split(serializedLocalHist, ";-`;"));
                    tempLocalHistory = new ArrayList<String>(storedDbChatHistory);

                    // to it, add on this conversation
                    tempLocalHistory.addAll(chatHistoryForDb);

                } else {
                    // if past convo does not exists,
                    // initialize and add this conversation
                    tempLocalHistory = new ArrayList<String>();
                    tempLocalHistory.addAll(chatHistoryForDb);
                }

                // if clear conversation menu button is pressed, do not save, but clear
                if (clearChatSelected) {
                    // clear
                    SharedPreferences.Editor editorLocalHist = chatHistorySharedPref.edit();
                    editorLocalHist.remove("conversation");
                    editorLocalHist.apply();

                    adapter.clearMessages();
                    adapter.notifyDataSetChanged();

                    clearChatSelected = false;

                } else {
                    // save the conversation to persist
                    // (before clearing dbChatHistory, copy it to chatHistoryPrefs)
                    SharedPreferences.Editor editorLocalHist = chatHistorySharedPref.edit();
                    editorLocalHist.putString("conversation", TextUtils.join(";-`;", tempLocalHistory));
                    editorLocalHist.apply();

                    // display persisted conversation to the user
                    for (String i:tempLocalHistory ) {
                        ChatMessage tempMsg = new ChatMessage();

                        // length of i guranteed to be at least 2 (u: or m:)
                        if(i.charAt(0) == 'u') {
                            tempMsg.setMe(true);
                        } else {
                            tempMsg.setMe(false);
                        }
                        tempMsg.setMessage(i.substring(i.indexOf(':') + 1));

                        displayMessage(tempMsg);
                    }

                }

                // clear history
                chatHistoryForDb.clear();
                // clear db prefs
                SharedPreferences.Editor editorDbHist = dbSharedPref.edit();
                editorDbHist.remove("conversation");
                editorDbHist.apply();

                // =====
                // END
                // get past stored conversation
                // to it, add on this conversation, then delete dbArray and dbSharedPrefs
                // =====

                if (hi != null) {
                    //onResult(response);
                    String test = "success";
                } else {
                    //onError(aiError);
                }
            }
        };

        // EXECUTE
        taskUpdateDb.execute();
    }


    public void displayMessage(ChatMessage message) {
        adapter.add(message);
        adapter.notifyDataSetChanged();
        scroll();
    }

    private void scroll() {
        messagesContainer.setSelection(messagesContainer.getCount() - 1);
    }

    private void selectWelcomeChatMessage(){

        // randomly select random greeting message
        // add to dbprefs

        int unicode = 0x1F60A;
        String smile = new String(Character.toChars(unicode));



        ChatMessage msg = new ChatMessage();
        msg.setMe(true);
        msg.setMessage("Do you like chatting?" + smile);
        chatHistory.add(msg);

        for(int i=0; i<chatHistory.size(); i++) {
            ChatMessage message = chatHistory.get(i);
            displayMessage(message);
        }


    }

    // start ai
    @Override
    public void onListeningStarted() {}

    @Override
    public void onListeningCanceled() {}

    @Override
    public void onListeningFinished() {}

    @Override
    public void onAudioLevel(final float level) {}
    // end ai


    // send request to api.ai
    /*
    * AIRequest should have query OR event
    */
    private void sendRequest(String query) {

        final String queryString = query;
        final String eventString = null;
        final String contextString = null;


        final AsyncTask<String, Void, AIResponse> task = new AsyncTask<String, Void, AIResponse>() {

            private AIError aiError;

            @Override
            protected AIResponse doInBackground(final String... params) {

                final AIRequest request = new AIRequest();
                String query = params[0];
                String event = params[1];

                if (!TextUtils.isEmpty(query))
                    request.setQuery(query);
                if (!TextUtils.isEmpty(event))
                    request.setEvent(new AIEvent(event));

                final String contextString = params[2];
                RequestExtras requestExtras = null;


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
        };

        // EXECUTE
        task.execute(queryString, eventString, contextString);
    }

    public void onResult(final AIResponse response) {

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

                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setMessage(speech);

                chatMessage.setMe(false);


                displayMessage(chatMessage);

                chatHistoryForDb.add(machine+speech);

                // add to shared prefs
                SharedPreferences.Editor editor = dbSharedPref.edit();
                editor.putString("conversation", TextUtils.join(";-`;", chatHistoryForDb));
                editor.apply();

                //addConversationToDb();

            }

        });


    }

    @Override
    public void onError(final AIError error) {
        Log.d(TAG, error.toString());
        Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
        //resultTextView.setText(error.toString());
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
//            addConversationToDb();
//        }
//
//    }


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
            startActivity(new Intent(MainActivity.this, TermsOfService.class));
            return true;
        } else if (id == R.id.action_delete_chat) {

            // if there is data in dbSharedPref there is a conversation has not been
            // pushed back to the server, push it to the server and delete
            // dbPrefs and chatHistoryPrefs

            String serialized = dbSharedPref.getString("conversation", null);

            if (serialized != null) {
                // START The user had a new conversation last time, push to db

                List<String> storedDbChatHistory = Arrays.asList(TextUtils.split(serialized, ";-`;"));

                chatHistoryForDb = new ArrayList<String>(storedDbChatHistory);

                if (chatHistoryForDb.size() > 0) {


                    //push untracked conversation to the server and delete
                    // dbPrefs and chatHistoryPrefs
                    clearChatSelected = true;
                    addConversationToDb();
                }
                // END The user had a new conversation last time, push to db
            }


            // START if there is no data in dbPrefs, do not push to db, just clear both shared prefs

            adapter.clearMessages();
            adapter.notifyDataSetChanged();

            SharedPreferences.Editor editorLocalHist = chatHistorySharedPref.edit();
            editorLocalHist.remove("conversation");
            editorLocalHist.apply();

            SharedPreferences.Editor editorDb = dbSharedPref.edit();
            editorDb.remove("conversation");
            editorDb.apply();

            // END if there is no data in dbPrefs, do not push to db, just clear both shared prefs


            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    // on click buttons
    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.sendMessageButton) {

            String messageText = userMessage.getText().toString();

            if (TextUtils.isEmpty(messageText)) {
                return;
            }

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMessage(messageText);
            chatMessage.setMe(true);

            userMessage.setText(""); // clear message field
            displayMessage(chatMessage);

            chatHistoryForDb.add(user+messageText);
            // add to shared prefs
            SharedPreferences.Editor editor = dbSharedPref.edit();
            editor.putString("conversation", TextUtils.join(";-`;", chatHistoryForDb));
            editor.apply();

            sendRequest(messageText);

            //aiService.startListening();
        }
    }


}
