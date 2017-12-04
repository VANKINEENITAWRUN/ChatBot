package com.example.tawrun.chatbot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

public class MainActivity extends AppCompatActivity implements AIListener {

    private RecyclerView recyclerView;
    private ImageView mSendButtonImage;
    private RelativeLayout mSendButton;
    private EditText mMessage;
    private int MESSAGE_DEFAULT_LENGTH=25;
    private boolean hasNoChar=true;
    private FirebaseDatabase database;
    private DatabaseReference msgRef;

    private AIService aiService;
    private AIConfiguration aiConfig;

    AIDataService aiDataService;
    AIRequest aiRequest;

    private FirebaseRecyclerAdapter mMessagesAdapter;
   LinearLayoutManager linearLayoutManager ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        assignViews();
        addListeners();
        database = FirebaseDatabase.getInstance();
        msgRef = database.getReference();
        msgRef.keepSynced(true);

        Query query= msgRef.child("chat")
                .limitToLast(150);
        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        
        FirebaseRecyclerOptions<MessageModel> options =
                new FirebaseRecyclerOptions.Builder<MessageModel>()
                        .setQuery(query, MessageModel.class)
                        .build();
        mMessagesAdapter = new FirebaseRecyclerAdapter<MessageModel, MessageReceived>(options) {


            @Override
            public MessageReceived onCreateViewHolder(ViewGroup parent, int viewType) {
                // Create a new instance of the ViewHolder, in this case we are using a custom
                // layout called R.layout.message for each item
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item, parent, false);

                return new MessageReceived(view);
            }

            @Override
            protected void onBindViewHolder(MessageReceived holder, int position, MessageModel model) {

                if (model.getIsUser()) {


                    holder.mUserText.setText(model.getMessage());

                    holder.mUserText.setVisibility(View.VISIBLE);
                    holder.mBotText.setVisibility(View.GONE);
                } else {
                    holder.mBotText.setText(model.getMessage());

                    holder.mBotText.setVisibility(View.VISIBLE);
                    holder.mUserText.setVisibility(View.GONE);
                }

            }

        };

        mMessagesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                int msgCount = mMessagesAdapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();

                if (lastVisiblePosition == -1 ||
                        (positionStart >= (msgCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    recyclerView.scrollToPosition(positionStart);
                }

            }
        });

        recyclerView.setAdapter(mMessagesAdapter);
        mMessagesAdapter.startListening();

      aiConfig = new AIConfiguration("74658a690df8421a8e43d211d9a87cea",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(this, aiConfig);
        aiService.setListener(this);
        aiDataService = new AIDataService(this,aiConfig);

         aiRequest = new AIRequest();
    }

    private void assignViews() {

        recyclerView =(RecyclerView) findViewById(R.id.recycle_view);
        recyclerView.setHasFixedSize(true);
         linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        mSendButton=(RelativeLayout)findViewById(R.id.addBtn);
//        mSendButton.setEnabled(false);
        mSendButtonImage=(ImageView)findViewById(R.id.fab_img);
        mMessage=(EditText)findViewById(R.id.message);

    }

    private void addListeners() {

        mSendButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {

                String messge=mMessage.getText().toString().trim();
                if(!messge.isEmpty()){
                    MessageModel model=new MessageModel(messge,true);
                    msgRef.child("chat").push().setValue(model);
                    Log.d("ddffd",messge);

                    mMessage.setText("");

                    aiRequest.setQuery(messge);
                    new AsyncTask<AIRequest,Void,AIResponse>(){

                        @Override
                        protected AIResponse doInBackground(AIRequest... aiRequests) {
                            final AIRequest request = aiRequests[0];
                            try {
                                final AIResponse response = aiDataService.request(aiRequest);
                                return response;
                            } catch (AIServiceException e) {
                            }
                            return null;
                        }
                        @Override
                        protected void onPostExecute(AIResponse response) {
                            if (response != null) {

                                Result result = response.getResult();
                                String reply = result.getFulfillment().getSpeech();
                                MessageModel chatMessage = new MessageModel(reply, false);
                                msgRef.child("chat").push().setValue(chatMessage);
                            }
                        }
                    }.execute(aiRequest);

                }else{ Log.d("ddffd","messge");

                }
            }
        });
        mMessage.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MESSAGE_DEFAULT_LENGTH)});

        mMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                Bitmap img = BitmapFactory.decodeResource(getResources(),R.drawable.ic_send_black_24dp);
                Bitmap img1 = BitmapFactory.decodeResource(getResources(),R.drawable.ic_mic_white_24dp);

                if(!charSequence.toString().trim().isEmpty() && hasNoChar ){
                    hasNoChar=false;


                    ImageViewAnimatedChange(MainActivity.this,mSendButtonImage,img);

                }else if(charSequence.toString().trim().isEmpty()){
                    Log.d("xcc","csc");
                    hasNoChar=true;

                    ImageViewAnimatedChange(MainActivity.this,mSendButtonImage,img1);

                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });




    }

    public void ImageViewAnimatedChange(Context c, final ImageView v, final Bitmap new_image) {
        final Animation anim_out = AnimationUtils.loadAnimation(c, R.anim.zoom_out);
        final Animation anim_in  = AnimationUtils.loadAnimation(c, R.anim.zoom_in);
        anim_out.setAnimationListener(new Animation.AnimationListener()
        {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation)
            {
                v.setImageBitmap(new_image);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationRepeat(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) {}
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }

    @Override
    public void onResult(ai.api.model.AIResponse response) {



        Result result = response.getResult();
        String message = result.getResolvedQuery();
        MessageModel chatMessage0 = new MessageModel(message, true);
        msgRef.child("chat").push().setValue(chatMessage0);


        String reply = result.getFulfillment().getSpeech();
        MessageModel chatMessage = new MessageModel(reply, false);
        msgRef.child("chat").push().setValue(chatMessage);


    }

    @Override
    public void onError(ai.api.model.AIError error) {

    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }
}