/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package my.home.lehome.fragment;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import my.home.lehome.R;
import my.home.lehome.adapter.MessageAdapter;
import my.home.lehome.mvp.presenters.SendMessagePresenter;
import my.home.lehome.mvp.views.SendMessageView;
import my.home.lehome.util.Constants;
import my.home.model.entities.MessageItem;

public class MessageFragment extends Fragment implements SendMessageView {
    public static final String TAG = "MessageFragment";

    SendMessagePresenter mSendMessagePresenter;

    private int mScreenWidth;
    private int mScreenHeight;
    private ListView mMessagesListView;
    private Button mSendButton;
    private MessageViewHandler mHandler;
    private MessageAdapter mMessageAdapter;

    public MessageFragment() {
    }

    public static MessageFragment newInstance() {
        MessageFragment fragment = new MessageFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupData();
    }

    @Override
    public void onStart() {
        super.onStart();
        mSendMessagePresenter.start();
    }

    @Override
    public void onStop() {
        mSendMessagePresenter.stop();
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mScreenWidth = size.x;
        mScreenHeight = size.y;

        View contentView = inflater.inflate(R.layout.fragment_send_message, container, false);
        setupViews(contentView);
        return contentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);
        registerForContextMenu(mMessagesListView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int id = item.getItemId();
        switch (id) {
            case R.id.clean_up_messages:
                mSendMessagePresenter.cleanMessages();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupData() {
        mSendMessagePresenter = new SendMessagePresenter(this);
        mHandler = new MessageViewHandler();
    }

    @Override
    public void setupViews(View rootView) {
        mSendButton = (Button) rootView.findViewById(R.id.send_message_btn);
        mSendButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mSendMessagePresenter.startRecording();
                    mSendButton.setText(getString(R.string.message_release_to_send));
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawY() / mScreenHeight <= Constants.DIALOG_CANCEL_Y_PERSENT) {
                        mSendMessagePresenter.cancelRecording();
                    } else {
                        mSendMessagePresenter.finishRecording();
                    }
                    mSendButton.setText(getString(R.string.message_press_to_speak));
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (event.getRawY() / mScreenHeight <= Constants.DIALOG_CANCEL_Y_PERSENT) {
                        mSendButton.setText(getString(R.string.message_release_to_cancel));
                    } else {
                        mSendButton.setText(getString(R.string.message_release_to_send));
                    }
                    return true;
                }
                return false;
            }
        });

        mMessageAdapter = new MessageAdapter(getActivity());
        mMessageAdapter.addAll(mSendMessagePresenter.getAllMessages());
        mMessagesListView = (ListView) rootView.findViewById(R.id.message_listview);
        mMessagesListView.setAdapter(mMessageAdapter);

        setRetainInstance(true);
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void onAddMsgItem(MessageItem msgItem) {
        mMessageAdapter.add(msgItem);
        mMessagesListView.smoothScrollToPosition(mMessageAdapter.getCount() - 1);
    }

    @Override
    public void onDeleteAllMessages() {
        mMessageAdapter.clear();
    }

    private class MessageViewHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }
}
