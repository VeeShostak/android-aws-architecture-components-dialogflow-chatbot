package io.github.veeshostak.aichat;

import android.content.Intent;
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
import io.github.veeshostak.aichat.Models.User;
import io.github.veeshostak.aichat.Models.UserConversation;
// end api.ai imports


import java.util.Calendar;
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;


import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;



public class MainActivity extends AppCompatActivity implements AIListener, View.OnClickListener {

    private static final String TAG = "ChatActivity";
    private final int REQUEST_INTERNET = 1;

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
        messagesContainer = (ListView) findViewById(R.id.messagesContainer);
        userMessage = (EditText) findViewById(R.id.userMessageField);

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

        // randomly select a welcome message
        selectWelcomeChatMessage();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
        }

        return super.onOptionsItemSelected(item);
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

                // clear history
                chatHistoryForDb.clear();

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


        chatHistory = new ArrayList<ChatMessage>();
        chatHistoryForDb = new ArrayList<String>();

        int unicode = 0x1F60A;
        String smile = new String(Character.toChars(unicode));

        Calendar cal = Calendar.getInstance();
        // remove next line if you're always using the current time.
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, -1);
        Date minBack0 = cal.getTime();

        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, +1);
        Date minBack1 = cal.getTime();

        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, +3);
        Date minBack2 = cal.getTime();

        ChatMessage msg = new ChatMessage();
        msg.setId(1);
        msg.setMe(true);
        msg.setMessage("Do you like chatting?");
        msg.setDate(DateFormat.getDateTimeInstance().format(minBack0));
        chatHistory.add(msg);

        ChatMessage msg1 = new ChatMessage();
        msg1.setId(2);
        msg1.setMe(false);
        msg1.setMessage("Only if it's with you" + smile);
        msg1.setDate(DateFormat.getDateTimeInstance().format(new Date()));
        chatHistory.add(msg1);
        // ==
        ChatMessage msg2 = new ChatMessage();
        msg2.setId(3);
        msg2.setMe(true);
        msg2.setMessage("Who are you?");
        msg2.setDate(DateFormat.getDateTimeInstance().format(minBack1));
        chatHistory.add(msg2);

        ChatMessage msg3 = new ChatMessage();
        msg3.setId(4);
        msg3.setMe(false);
        msg3.setMessage("Today a name, tomorrow a legend");
        msg3.setDate(DateFormat.getDateTimeInstance().format(minBack1));
        chatHistory.add(msg3);
        // ==

        ChatMessage msg4 = new ChatMessage();
        msg4.setId(5);
        msg4.setMe(true);
        msg4.setMessage("Haha! Your funny!");
        msg4.setDate(DateFormat.getDateTimeInstance().format(minBack2));
        chatHistory.add(msg4);

        ChatMessage msg5 = new ChatMessage();
        msg5.setId(6);
        msg5.setMe(false);
        msg5.setMessage("*You’re.\nThe grammar police are on their way, I just called them");
        msg5.setDate(DateFormat.getDateTimeInstance().format(minBack2));
        chatHistory.add(msg5);

        adapter = new ChatAdapter(MainActivity.this, new ArrayList<ChatMessage>());
        messagesContainer.setAdapter(adapter);

        for(int i=0; i<chatHistory.size(); i++) {
            ChatMessage message = chatHistory.get(i);
            displayMessage(message);
        }

//        chatHistoryForDb.add(machine+"Hi");
//        chatHistoryForDb.add(machine+"How r u doing??");
//        chatHistoryForDb.add(user+"great, thanks for asking");

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
                chatMessage.setId(122);//dummy
                chatMessage.setMessage(speech);
                chatMessage.setDate(DateFormat.getDateTimeInstance().format(new Date()));
                chatMessage.setMe(false);


                displayMessage(chatMessage);

                chatHistoryForDb.add(machine+speech);

                addConversationToDb();

            }

        });


    }

    @Override
    public void onError(final AIError error) {
        Log.d(TAG, error.toString());
        Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
        //resultTextView.setText(error.toString());
    }

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
            addConversationToDb();
        }




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
            chatMessage.setId(123);//dummy
            chatMessage.setMessage(messageText);
            chatMessage.setDate(DateFormat.getDateTimeInstance().format(new Date()));
            chatMessage.setMe(true);

            userMessage.setText(""); // clear
            displayMessage(chatMessage);

            chatHistoryForDb.add(user+messageText);

            sendRequest(messageText);

            //aiService.startListening();
        }
    }


}
