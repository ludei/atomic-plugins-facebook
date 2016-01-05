package com.ludei.facebook.cordova;

import android.content.Intent;

import com.ludei.facebook.FacebookService;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;



public class FacebookPlugin extends CordovaPlugin implements FacebookService.SessionCallback {


    protected FacebookService _service;
    protected CallbackContext _sessionListener;

    @Override
    protected void pluginInitialize() {
        _service = new FacebookService(this.cordova.getActivity());
        _service.setOnLoginChangeListener(this);
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {

        try
        {
            Method method = this.getClass().getDeclaredMethod(action, CordovaArgs.class, CallbackContext.class);
            method.invoke(this, args, callbackContext);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        this._service.onActivityResult(requestCode, resultCode, intent);
    }


    @SuppressWarnings("unused")
    public void setListener(CordovaArgs args, CallbackContext ctx) throws JSONException {
        _sessionListener = ctx;
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        ctx.sendPluginResult(result);
    }

    @SuppressWarnings("unused")
    public void initialize(CordovaArgs args, CallbackContext ctx) throws JSONException {
        _service.initialize();
        ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, (String) null));
    }

    public void login(CordovaArgs args, final CallbackContext ctx) throws JSONException {
        JSONObject params = args.optJSONObject(0);
        ArrayList<String> permissions = null;
        if (params != null) {
            String scope = params.optString("scope", "");
            if (scope != null && scope.length() > 0) {
                permissions = new ArrayList<String>(Arrays.asList(scope.split(",")));
            }
        }
        cordova.setActivityResultCallback(this);
        _service.loginWithReadPermissions(permissions, this.cordova.getActivity(), new FacebookService.SessionCallback() {
            @Override
            public void onComplete(FacebookService.Session session, FacebookService.Error error) {
                notifyCallback(ctx, session != null ? session.toJSON() : null, error, false);
            }
        });
    }

    public void logout(CordovaArgs args, final CallbackContext ctx) throws JSONException {
        _service.logout();
        ctx.success();
    }

    public void requestAdditionalPermissions(CordovaArgs args, final CallbackContext ctx) throws JSONException {
        String type = args.optString(0);
        if (type == null) {
            type = "read";
        }
        JSONArray array = args.optJSONArray(1);
        ArrayList<String> permissions = new ArrayList<String>();
        if (array != null) {
            for (int i = 0; i < array.length(); ++i) {
                permissions.add(array.optString(i, ""));
            }
        }
        cordova.setActivityResultCallback(this);
        _service.requestAdditionalPermissions(type, permissions, cordova.getActivity(), new FacebookService.SessionCallback() {
            @Override
            public void onComplete(FacebookService.Session session, FacebookService.Error error) {
                notifyCallback(ctx, session != null ? session.toJSON() : null, error, false);
            }
        });
    }


    public void api(CordovaArgs args, final CallbackContext ctx) throws JSONException {
        String graph = args.getString(0);
        String method = args.optString(1);
        JSONObject params = args.optJSONObject(2);
        _service.api(graph, method, params, new FacebookService.CompletionCallback() {
            @Override
            public void onComplete(JSONObject result, FacebookService.Error error) {
                notifyCallback(ctx, result, error, false);
            }
        });
    }

    public void ui(CordovaArgs args, final CallbackContext ctx) throws JSONException {
        String method = args.getString(0);
        JSONObject params = args.optJSONObject(1);
        cordova.setActivityResultCallback(this);
        _service.ui(method, params, cordova.getActivity(), new FacebookService.CompletionCallback() {
            @Override
            public void onComplete(JSONObject result, FacebookService.Error error) {
                notifyCallback(ctx, result, error, false);
            }
        });
    }

    public void showShareDialog(CordovaArgs args, final CallbackContext ctx) throws JSONException {
        JSONObject params = args.getJSONObject(0);
        cordova.setActivityResultCallback(this);
        _service.showShareDialog(params, cordova.getActivity(), new FacebookService.CompletionCallback() {
            @Override
            public void onComplete(JSONObject result, FacebookService.Error error) {
                notifyCallback(ctx, result, error, false);
            }
        });
    }

    public void uploadPhoto(CordovaArgs args, final CallbackContext ctx) throws JSONException {
        String file = args.getString(0);
        JSONObject params = args.getJSONObject(1);
        _service.uploadPhoto(file, params, new FacebookService.CompletionCallback() {
            @Override
            public void onComplete(JSONObject result, FacebookService.Error error) {
                notifyCallback(ctx, result, error, false);
            }
        });
    }


    //Session Listener
    @Override
    public void onComplete(FacebookService.Session session, FacebookService.Error error)
    {
        if (_sessionListener != null) {
            notifyCallback(_sessionListener, session != null ? session.toJSON() : null, error, true);
        }

    }

    //Helpers
    private void notifyCallback(CallbackContext ctx, JSONObject response, FacebookService.Error error, boolean keep) {
        ArrayList<PluginResult> arguments = new ArrayList<PluginResult>();
        JSONObject json = response != null ? response : new JSONObject();
        if (error != null) {
            try {
                json.putOpt("error", error.toJSON());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        arguments.add(new PluginResult(PluginResult.Status.OK, json));
        if (error != null) {
            arguments.add(new PluginResult(PluginResult.Status.OK, error.toJSON()));
        }
        PluginResult result = new PluginResult(PluginResult.Status.OK, arguments);
        if (keep) {
            result.setKeepCallback(true);
        }
        ctx.sendPluginResult(result);
    }


}
