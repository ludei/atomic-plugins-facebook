package com.ludei.facebook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.internal.WebDialog;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.Sharer;
import com.facebook.share.model.GameRequestContent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.GameRequestDialog;
import com.facebook.share.widget.ShareDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class FacebookService  {

    public interface SessionCallback {
        void onComplete(Session session, Error error);
    }

    public interface CompletionCallback {
        void onComplete(JSONObject result, Error error);
    }


    private CallbackManager _fbCallbackManager;
    private SessionCallback _onLoginChangeCallback;
    private ProfileTracker _profileTracker;
    private Runnable _profileChangeTask;
    private ArrayList<SessionCallback> _sessionCallbacks = new ArrayList<SessionCallback>();
    private JSONObject _params;

    public FacebookService(Context ctx) {
        FacebookSdk.sdkInitialize(ctx.getApplicationContext());
        _fbCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(_fbCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                FacebookService.this.processSessionChange(loginResult.getAccessToken(), null);
            }

            @Override
            public void onCancel() {
                FacebookService.this.processSessionChange(null, null);
            }

            @Override
            public void onError(FacebookException error) {
                FacebookService.this.processSessionChange(null, new Error(error.getLocalizedMessage(), 0));
            }
        });

        _profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile profile, Profile profile2) {
                if (_profileChangeTask != null) {
                    _profileChangeTask.run();
                    _profileChangeTask = null;
                }
            }
        };
        _profileTracker.startTracking();
    }

    public void initialize(JSONObject params)
    {
        this._params = params;
        if (this.isLoggedIn()) {
            this.processSessionChange(AccessToken.getCurrentAccessToken(), null);
        }

    }

    public void setOnLoginChangeListener(SessionCallback callback)
    {
        this._onLoginChangeCallback = callback;
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
        return _fbCallbackManager.onActivityResult(requestCode, resultCode, intent);
    }

    public boolean isLoggedIn() {
        AccessToken token = AccessToken.getCurrentAccessToken();
        return token != null && token.getToken() != null && token.getToken().length() > 0;
    }

    public void loginWithReadPermissions(Collection<String> permissions, Activity fromActivity, SessionCallback callback)
    {
        this.requestAdditionalPermissions("read", permissions, fromActivity, callback);
    }

    public void getLoginStatus(boolean force, SessionCallback callback) {
        callback.onComplete(new Session(AccessToken.getCurrentAccessToken()), null);
    }

    public void logout() {
        LoginManager.getInstance().logOut();
    }

    public void requestAdditionalPermissions(String type, Collection<String> permissions, Activity fromActivity, SessionCallback callback) {

        boolean ready = this.isLoggedIn();
        if (ready && permissions != null && permissions.size() > 0) {
            Set<String> currentPermissions = AccessToken.getCurrentAccessToken().getPermissions();
            for (String str: permissions) {
                if (currentPermissions == null || !currentPermissions.contains(str)) {
                    ready = false;
                    break;
                }
            }
        }

        if (ready) {
            if (callback != null) {
                callback.onComplete(new Session(AccessToken.getCurrentAccessToken()), null);
            }
        }
        else {

            if (callback != null) {
                _sessionCallbacks.add(callback);
            }
            if ("publish".equals(type)) {
                LoginManager.getInstance().logInWithPublishPermissions(fromActivity, permissions);
            }
            else {
                LoginManager.getInstance().logInWithReadPermissions(fromActivity, permissions);
            }
        }

    }

    public void api(String graph, String httpMethod, JSONObject params, final CompletionCallback callback)
    {
        GraphRequest request = GraphRequest.newGraphPathRequest(AccessToken.getCurrentAccessToken(), graph, new GraphRequest.Callback() {

            @Override
            public void onCompleted(GraphResponse response) {

                if (callback == null) {
                    return;
                }

                Error error = null;
                if (response.getError() != null) {
                    error = new Error(response.getError().getErrorMessage(), response.getError().getErrorCode());
                }
                JSONObject result = response.getJSONObject();
                callback.onComplete(result, error);
            }
        });
        if (httpMethod.equalsIgnoreCase("POST")) {
            request.setHttpMethod(HttpMethod.POST);
        }
        else if (httpMethod.equalsIgnoreCase("DELETE")) {
            request.setHttpMethod(HttpMethod.DELETE);
        }
        else {
            request.setHttpMethod(HttpMethod.GET);
        }

        if (params != null) {
            try {
                request.setParameters(jsonToBundle(params));
            }
            catch (JSONException e) {
                if (callback != null) {
                    callback.onComplete(null, new Error(e.getLocalizedMessage(), 0));
                }
                return;
            }
        }

        request.executeAsync();
    }

    public void ui(final String method, final JSONObject params, final Activity fromActivity, final CompletionCallback callback) throws JSONException {
        if (method == null) {
            callback.onComplete(null, new Error("No method provided", 0));
        }

        if (method.equalsIgnoreCase("apprequests")) {
            final GameRequestDialog requestDialog = new GameRequestDialog(fromActivity);
            requestDialog.registerCallback(_fbCallbackManager, new FacebookCallback<GameRequestDialog.Result>() {
                    public void onSuccess(GameRequestDialog.Result values) {
                        if (callback == null)
                            return;

                        JSONObject result = new JSONObject();
                        Error error = null;
                        try {
                            result.put("request", values.getRequestId());
                            result.put("to", new JSONArray(values.getRequestRecipients()));

                        } catch (JSONException ex) {
                            error = new Error(ex.getLocalizedMessage(), 0);
                        }
                        callback.onComplete(result, error);
                    }
                    public void onCancel() {
                        callback.onComplete(null, new Error("Canceled by user", 0));
                    }
                    public void onError(FacebookException error) {
                        callback.onComplete(null, new Error(error.getLocalizedMessage(), 0));
                    }
                }
            );
            final GameRequestContent.Builder builder = new GameRequestContent.Builder();
            if (params.has("message"))
                builder.setMessage(params.getString("message"));
            List<String> list = new ArrayList<String>();
            if (params.get("to") instanceof JSONArray) {
                JSONArray jArray = params.getJSONArray("to");
                if (jArray != null) {
                    for (int i=0;i<jArray.length();i++){
                        list.add(jArray.get(i).toString());
                    }
                }

            } else if (params.get("to") instanceof String) {
                list.add(params.getString("to"));
            }
            if (params.has("to"))
                builder.setRecipients(list);
            if (params.has("data"))
                builder.setData(params.getString("data"));
            if (params.has("title"))
                builder.setTitle(params.getString("title"));
            if (params.has("object_id"))
                builder.setObjectId(params.getString("object_id"));
            if (params.has("action_type")) {
                try {
                    final GameRequestContent.ActionType actionType = GameRequestContent.ActionType.valueOf(params.getString("action_type").toUpperCase());
                    builder.setActionType(actionType);
                } catch (IllegalArgumentException e) {
                    Log.w(FacebookService.class.getCanonicalName(), "Discarding invalid argument actionType");
                }
            }
            if (params.has("filters")) {
                try {
                    GameRequestContent.Filters filters = null;
                    if (params.get("filters") instanceof String) {
                        filters = GameRequestContent.Filters.valueOf(params.getString("filters"));

                    } else if(params.get("filters") instanceof JSONArray) {
                        JSONArray set = params.getJSONArray("filters");
                        if (set.length() > 0)
                            filters = GameRequestContent.Filters.valueOf(set.getString(0));
                    }
                    builder.setFilters(filters);
                } catch (IllegalArgumentException e) {
                    Log.w(FacebookService.class.getCanonicalName(), "Discarding invalid argument filters");
                }
            }

            fromActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        requestDialog.show(builder.build());

                    } catch (IllegalArgumentException e) {
                        callback.onComplete(null, new Error(e.getLocalizedMessage(), 0));
                    }
                }
            });

        } else {
            fromActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final WebDialog dialog = new WebDialog(fromActivity, method, jsonToBundle(params), FacebookSdk.getWebDialogTheme(), new WebDialog.OnCompleteListener() {
                            @Override
                            public void onComplete(Bundle values, FacebookException fbError) {
                                if (callback == null) {
                                    return;
                                }

                                JSONObject result = bundleToJson(values);
                                Error error = null;
                                if (fbError != null) {
                                    error = new Error(fbError.getLocalizedMessage(), 0);
                                }
                                callback.onComplete(result, error);

                            }
                        });

                        dialog.show();

                    } catch (JSONException e) {
                        callback.onComplete(null, new Error(e.getLocalizedMessage(), 0));
                    }
                }
            });
        }
    }

    public void showShareDialog(JSONObject params, Activity fromActivity, final CompletionCallback callback) {

        final ShareLinkContent.Builder builder = new ShareLinkContent.Builder();
        builder.setContentDescription(params.optString("description",""));
        builder.setContentTitle(params.optString("name",""));
        if (params.has("link")) {
            builder.setContentUrl(Uri.parse(params.optString("link","")));
        }
        if (params.has("image")) {
            builder.setImageUrl(Uri.parse(params.optString("image","")));
        }

        final ShareDialog dialog = new ShareDialog(fromActivity);
        if (callback != null) {
            dialog.registerCallback(_fbCallbackManager, new FacebookCallback<Sharer.Result>() {
                @Override
                public void onSuccess(Sharer.Result result) {
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("post_id", result.getPostId());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    callback.onComplete(obj, null);
                }

                @Override
                public void onCancel() {
                    callback.onComplete(null, new Error("Canceled by user", 0));
                }

                @Override
                public void onError(FacebookException error) {
                    callback.onComplete(null, new Error(error.getLocalizedMessage(), 0));
                }
            });
        }

        fromActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show(builder.build());
            }
        });
    }

    public void uploadPhoto(String filePath, JSONObject params, final CompletionCallback callback) {
        String path = filePath.replace("file://", "") ;
        Bitmap image =  BitmapFactory.decodeFile(path);
        if (image == null) {
            if (callback != null) {
                callback.onComplete(null, new Error("Can't decode image from file: " + filePath, 0));
            }
            return;
        }
        Bundle bundle = null;
        try {
            bundle = jsonToBundle(params);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        GraphRequest request = GraphRequest.newUploadPhotoRequest(AccessToken.getCurrentAccessToken(), "me/photos", image, null, bundle, new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                if (callback == null) {
                    return;
                }

                Error error = null;
                if (response.getError() != null) {
                    error = new Error(response.getError().getErrorMessage(), response.getError().getErrorCode());
                }
                JSONObject result = response.getJSONObject();
                callback.onComplete(result, error);
            }
        });
        request.executeAsync();

    }

    protected void processSessionChange(final AccessToken token, Error error) {
        if (error != null) {
            //error
            notifyOnSessionChange(null, error);
        }
        else if (token != null) {
            //logged in
            Profile profile = Profile.getCurrentProfile();
            if (profile != null) {
                notifyOnSessionChange(new Session(token), null);
            }
            else {
                _profileChangeTask = new Runnable() {
                    @Override
                    public void run() {
                        notifyOnSessionChange(new Session(token), null);
                    }
                };
            }
        }
        else {
            //logged out or user cancelled login
            notifyOnSessionChange(null, null);
        }
    }

    protected void notifyOnSessionChange(Session session, Error error) {

        //notify generic listener
        if (_onLoginChangeCallback != null) {
            _onLoginChangeCallback.onComplete(session, error);
        }

        //Notify func callbacks
        ArrayList<SessionCallback> callbacksCopy = new ArrayList<SessionCallback>();
        callbacksCopy.addAll(_sessionCallbacks);
        for (SessionCallback callback: callbacksCopy) {
            callback.onComplete(session, error);
        }
        _sessionCallbacks.clear();
    }


    private static JSONObject bundleToJson(Bundle bundle){
        JSONObject json = new JSONObject();
        if (bundle != null) {
            Set<String> keys = bundle.keySet();
            for (String key : keys) {
                try {
                    json.put(key, bundle.get(key));
                } catch(JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return json;
    }

    private static Bundle jsonToBundle(JSONObject jsonObject) throws JSONException {
        Bundle bundle = new Bundle();
        if (jsonObject != null) {
            Iterator iter = jsonObject.keys();
            while(iter.hasNext()){
                String key = (String)iter.next();
                String value = jsonObject.getString(key);
                bundle.putString(key,value);
            }
        }
        return bundle;
    }


    public enum SessionState {
        Open,
        NotAuthorized,
        Closed
    }
    public static class Session {

        public String accessToken;
        public JSONObject user;
        public String[] permissions;
        public long expirationDate;
        public SessionState state = SessionState.Closed;


        public Session(AccessToken token)
        {
            if (token != null) {
                this.accessToken = token.getToken();
                if (token.getPermissions() != null) {
                    this.permissions = new String[token.getPermissions().size()];
                    token.getPermissions().toArray(this.permissions);
                }
                if (token.getExpires() != null) {
                    this.expirationDate = token.getExpires().getTime();
                }
                if (this.accessToken != null && this.accessToken.length() > 0) {
                    state = SessionState.Open;
                }
                else {
                    state = SessionState.NotAuthorized;
                }
            }

            Profile profile = Profile.getCurrentProfile();
            if (profile != null){
                JSONObject obj = new JSONObject();
                try
                {   obj.putOpt("id", profile.getId());
                    obj.putOpt("first_name", profile.getFirstName());
                    obj.putOpt("last_name", profile.getLastName());
                    obj.putOpt("name", profile.getName());
                    obj.putOpt("link", profile.getLinkUri() != null ? profile.getLinkUri().toString() : "");
                    this.user = obj;
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }

        public JSONObject toJSON()
        {
            JSONObject result = new JSONObject();
            try
            {   result.putOpt("accessToken", accessToken);
                result.putOpt("state", this.state.ordinal());
                result.putOpt("expirationDate", expirationDate);
                if (user != null) {
                    result.putOpt("user", user);
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
            }

            return result;
        }
    }
    public class Error {
        public String message;
        public int code;

        Error(String msg, int code) {
            this.message = msg;
            this.code = code;
        }

        public JSONObject toJSON()
        {
            JSONObject obj = new JSONObject();
            try
            {   obj.putOpt("code", code);
                obj.putOpt("message", message);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }

            return obj;
        }
    }

}
