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

package my.home.lehome.asynctask;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import my.home.common.Constants;
import my.home.lehome.R;
import my.home.lehome.activity.MainActivity;
import my.home.lehome.fragment.ChatFragment;
import my.home.lehome.helper.DBHelper;
import my.home.lehome.helper.MessageHelper;
import my.home.model.entities.ChatItem;

//import org.zeromq.ZMQ;


public class SendCommandAsyncTask extends AsyncTask<Void, String, String> {

    private static final String TAG = "SendCommandAsyncTask";
    private String mCmdString = "";
    private ChatFragment mFragment;
    private Context mContext;
    private ChatItem mCurrentItem;

    public SendCommandAsyncTask(Context context, String cmdString) {
        if (context instanceof MainActivity) {
            this.mFragment = ((MainActivity) context).getChatFragment();
        } else {
            this.mFragment = null;
        }
        this.mCurrentItem = null;
        this.mContext = context;
        this.mCmdString = cmdString;
    }

    public SendCommandAsyncTask(Context context, ChatItem chatItem) {
        if (context instanceof MainActivity) {
            this.mFragment = ((MainActivity) context).getChatFragment();
        } else {
            this.mFragment = null;
        }
        this.mContext = context;
        this.mCmdString = chatItem.getContent();
        this.mCurrentItem = chatItem;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (this.mCurrentItem != null) {
            mCurrentItem.setState(Constants.CHATITEM_STATE_PENDING);
            if (this.mFragment != null) {
                mFragment.getAdapter().notifyDataSetChanged();
            }
        } else {
            mCurrentItem = new ChatItem();
            mCurrentItem.setContent(mCmdString);
            mCurrentItem.setIsMe(true);
            mCurrentItem.setState(Constants.CHATITEM_STATE_ERROR); // set ERROR
            mCurrentItem.setDate(new Date());
            DBHelper.addChatItem(this.mContext, mCurrentItem);

            mCurrentItem.setState(Constants.CHATITEM_STATE_PENDING); // set PENDING temporary

            if (this.mFragment != null) {
                mFragment.getAdapter().add(mCurrentItem);
                mFragment.getAdapter().notifyDataSetChanged();
                ((MainActivity) mFragment.getAdapter().getContext()).getChatFragment().scrollMyListViewToBottom();
            }
        }
    }

    @Override
    protected String doInBackground(Void... cmd) {
        Log.d(TAG, "sending: " + mCmdString);

        if (TextUtils.isEmpty(MessageHelper.DEVICE_ID)) {
            return this.mContext.getResources().getString(R.string.msg_no_deviceid);
        }

        String message = MessageHelper.getFormatMessage(mCmdString);
        String targetURL = MessageHelper.getServerURL(message);

        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        JSONObject repObject = new JSONObject();
        try {
            response = httpclient.execute(new HttpGet(targetURL));
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                responseString = out.toString();
            } else {
                //Closes the connection.
                response.getEntity().getContent().close();
                try {
                    repObject.put("code", 400);
                    repObject.put("desc", mContext.getString(R.string.chat_error_conn));
                } catch (JSONException je) {
                }
                responseString = repObject.toString();
            }
        } catch (ClientProtocolException e) {
            try {
                repObject.put("code", 400);
                repObject.put("desc", mContext.getString(R.string.chat_error_protocol_error));
            } catch (JSONException je) {
            }
            responseString = repObject.toString();
        } catch (IOException e) {
            try {
                repObject.put("code", 400);
                repObject.put("desc", mContext.getString(R.string.chat_error_http_error));
            } catch (JSONException je) {
            }
            responseString = repObject.toString();
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        int rep_code = -1;
        String desc;
        try {
            JSONObject jsonObject = new JSONObject(result);
            rep_code = jsonObject.getInt("code");
            desc = jsonObject.getString("desc");
        } catch (JSONException e) {
            e.printStackTrace();
            desc = mContext.getString(R.string.chat_error_json);
        }

        Log.d(TAG, "send cmd finish: " + rep_code + " " + desc);

        if (rep_code == 200) {
            mCurrentItem.setState(Constants.CHATITEM_STATE_SUCCESS);
            DBHelper.updateChatItem(this.mContext, mCurrentItem);
            mFragment.getAdapter().notifyDataSetChanged();
        } else {
            if (rep_code == 415) {
                mCurrentItem.setState(Constants.CHATITEM_STATE_SUCCESS);
            } else {
                mCurrentItem.setState(Constants.CHATITEM_STATE_ERROR);
            }
            DBHelper.updateChatItem(this.mContext, mCurrentItem);

            ChatItem newItem = new ChatItem();
            newItem.setContent(desc);
            newItem.setIsMe(false);
            newItem.setState(Constants.CHATITEM_STATE_ERROR); // always set true
            newItem.setDate(new Date());
            DBHelper.addChatItem(this.mContext, newItem);

            if (this.mFragment != null) {
                mFragment.getAdapter().add(newItem);
                mFragment.getAdapter().notifyDataSetChanged();
                ((MainActivity) mFragment.getAdapter().getContext()).getChatFragment().scrollMyListViewToBottom();
            }
        }
    }

}
