package com.pixelnx.eacademy.ui.live;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.pixelnx.eacademy.R;
import com.pixelnx.eacademy.model.modellogin.ModelLogin;
import com.pixelnx.eacademy.utils.AppConsts;
import com.pixelnx.eacademy.utils.ProjectUtils;
import com.pixelnx.eacademy.utils.sharedpref.SharedPref;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.Key;
import java.util.Date;
import java.util.List;

//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.security.Keys;
import us.zoom.sdk.JoinMeetingOptions;
import us.zoom.sdk.JoinMeetingParams;
import us.zoom.sdk.MeetingParameter;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.MeetingServiceListener;
import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.MeetingViewsOptions;
import us.zoom.sdk.ZoomAppLocal;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomSDKInitParams;
import us.zoom.sdk.ZoomSDKInitializeListener;
import us.zoom.sdk.ZoomSDKRawDataMemoryMode;

public class ActivityLive extends AppCompatActivity implements ZoomSDKInitializeListener, MeetingServiceListener {
    Context mContext;
    MeetingService meetingService;
    ZoomSDKInitParams initParams;
    String domain = "zoom.us";//can't change it
    String sdkKey = "";
    static String secretKey = "";
    ZoomSDK sdk;
    SharedPref sharedPref;
    ModelLogin modelLogin;
    String numberMeeting = "";
    String passwordMeeting = "";
    String disName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        mContext = ActivityLive.this;
        sharedPref = SharedPref.getInstance(mContext);
        modelLogin = sharedPref.getUser(AppConsts.STUDENT_DATA);

        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(mContext, getResources().getString(R.string.Please_allow_permissions), Toast.LENGTH_SHORT).show();
            requestPermission();
        }
        if (getIntent().hasExtra("meetingPassword")) {
            passwordMeeting = getIntent().getStringExtra("meetingPassword");
            numberMeeting = getIntent().getStringExtra("meetingId");
            sdkKey = getIntent().getStringExtra("sdkKey");
            secretKey = getIntent().getStringExtra("sdkSecret");
        }

        if (numberMeeting.isEmpty()) {
            apiMeetingData();
        }
        Log.v("saloni_bugcheck","saloni 1 ---  "+passwordMeeting+"  "+numberMeeting+"  "+sdkKey+"  "+secretKey);
        sdk = ZoomSDK.getInstance();

        Log.v("saloni_bugcheck","saloni 2 ---  "+passwordMeeting+"  "+numberMeeting+"  "+sdkKey+"  "+secretKey);
        if (sdk.isInitialized()) {
            ZoomSDK.getInstance().getMeetingService().addListener(this);
            ZoomSDK.getInstance().getMeetingSettingsHelper().enable720p(true);
            Log.e("right","0");
        }else {
            Log.e("not","1");
        }

        Log.v("saloni_bugcheck","saloni 3 ---  "+passwordMeeting+"  "+numberMeeting+"  "+sdkKey+"  "+secretKey);
        String jwtToken = JwtGenerator.createToken(sdkKey,secretKey,disName);

        initParams = new ZoomSDKInitParams();


        initParams.jwtToken = jwtToken;
        initParams.domain = "zoom.us";
        initParams.autoRetryVerifyApp = true;
        initParams.enableLog = true;
        initParams.logSize = 50;
        initParams.enableGenerateDump = true;
        initParams.appLocal = ZoomAppLocal.ZoomLocale_CN;
        initParams.audioRawDataMemoryMode = ZoomSDKRawDataMemoryMode.ZoomSDKRawDataMemoryModeStack;
        initParams.videoRawDataMemoryMode = ZoomSDKRawDataMemoryMode.ZoomSDKRawDataMemoryModeStack;
        initParams.shareRawDataMemoryMode = ZoomSDKRawDataMemoryMode.ZoomSDKRawDataMemoryModeStack;

//        initParams.enableLog = true;
//        initParams.enableGenerateDump = true;
//
//        initParams.logSize = 50;
//        initParams.domain = "" + domain;
//        initParams.jwtToken = jwtToken;
//      //  Log.e("hi",""+ generateJwtToken(sdkKey,secretKey));
//
//        initParams.videoRawDataMemoryMode = ZoomSDKRawDataMemoryMode.ZoomSDKRawDataMemoryModeStack;

        sdk.initialize(mContext, this, initParams);

    }

    private void requestPermission() {

        Dexter.withActivity((Activity) mContext)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                        ,Manifest.permission.BLUETOOTH
                        ,Manifest.permission.BLUETOOTH_CONNECT)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.isAnyPermissionPermanentlyDenied()) {

                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }

                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(mContext, getResources().getString(R.string.Try_again), Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();

    }

    void apiMeetingData() {

        ProjectUtils.showProgressDialog(mContext, false, getResources().getString(R.string.Loading___));
        AndroidNetworking.post(AppConsts.BASE_URL + AppConsts.API_LIVE_CLASS_DATA)
                .addBodyParameter(AppConsts.BATCH_ID, modelLogin.getStudentData().getBatchId())
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        ProjectUtils.pauseProgressDialog();
                        try {
                            JSONObject jsonObject = new JSONObject(response);

                            if (AppConsts.TRUE.equals("" + jsonObject.getString("status"))) {
                                JSONArray jsonArray = new JSONArray("" + jsonObject.getString("data"));
                                JSONObject jsonObject1 = new JSONObject("" + jsonArray.get(0));

                                numberMeeting = "" + jsonObject1.getString("meetingId");
                                passwordMeeting = "" + jsonObject1.getString("meetingPassword");
                                sdkKey = "" + jsonObject1.getString("sdkKey");
                                secretKey = "" + jsonObject1.getString("sdkSecret");

                            } else {
                                Toast.makeText(mContext, getResources().getString(R.string.NoClassAvailable), Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onError(ANError anError) {
                        ProjectUtils.pauseProgressDialog();

                    }
                });
    }


    @Override
    public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {


        meetingService = sdk.getMeetingService();

        JoinMeetingParams params = new JoinMeetingParams();
        disName = "" + modelLogin.getStudentData().getFullName();
        params.displayName = "" + disName;
        params.meetingNo = numberMeeting;
        params.password = passwordMeeting;
        Log.e("",""+numberMeeting+ "  " + passwordMeeting+" "+disName);


        JoinMeetingOptions opts = new JoinMeetingOptions();

        opts.no_driving_mode = true;
        opts.no_invite = true;
        opts.meeting_views_options = MeetingViewsOptions.NO_TEXT_PASSWORD
                + MeetingViewsOptions.NO_TEXT_MEETING_ID;

        if (meetingService != null) {
            meetingService.joinMeetingWithParams(this, params, opts);
            Log.e("fdsfda","fdsafasd");
        }

        if(errorCode==2){
            Toast.makeText(mContext, getResources().getString(R.string.NoClassAvailable)+"\nContact admin for right credentials", Toast.LENGTH_SHORT).show();
        }
        Log.e("fdsfda","fdsafasd   " + errorCode+ internalErrorCode);
        onBackPressed();


    }


    @Override
    public void onZoomAuthIdentityExpired() {

    }


    @Override
    public void onMeetingStatusChanged(MeetingStatus meetingStatus, int errorCode,
                                       int internalErrorCode) {
        Log.e("error ","error code"+errorCode+","+ meetingStatus+"      internalError"+ internalErrorCode);

    }

    @Override
    public void onMeetingParameterNotification(MeetingParameter meetingParameter) {

    }


}
