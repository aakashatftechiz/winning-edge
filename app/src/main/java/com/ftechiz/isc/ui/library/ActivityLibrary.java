package com.ftechiz.isc.ui.library;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ftechiz.isc.R;
import com.ftechiz.isc.model.modellogin.ModelLogin;
import com.ftechiz.isc.utils.AppConsts;
import com.ftechiz.isc.utils.ProjectUtils;
import com.ftechiz.isc.utils.sharedpref.SharedPref;
import com.ftechiz.isc.utils.widgets.CustomTextExtraBold;

import java.util.Locale;

public class ActivityLibrary extends AppCompatActivity {
    Context context;
    ImageView ivBack;
    CustomTextExtraBold tvHeader;
    RelativeLayout book, notes, oldPaper;
    ModelLogin modelLogin;
    SharedPref sharedPref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = SharedPref.getInstance(this);
        modelLogin = sharedPref.getUser(AppConsts.STUDENT_DATA);
        if(modelLogin != null) {
            if (modelLogin.getStudentData() != null) {
                if (modelLogin.getStudentData().getLanguageName().equalsIgnoreCase("arabic")) {
                    //manually set Directions

                    getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                    Configuration configuration = getResources().getConfiguration();
                    configuration.setLayoutDirection(new Locale("fa"));
                    getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
                    String languageToLoad = "ar"; // your language
                    Locale locale = new Locale(languageToLoad);
                    Locale.setDefault(locale);
                    Configuration config = new Configuration();
                    config.locale = locale;
                    getBaseContext().getResources().updateConfiguration(config,
                            getBaseContext().getResources().getDisplayMetrics());
                }

                if (modelLogin.getStudentData().getLanguageName().equalsIgnoreCase("french")) {
                    String languageToLoad = "fr"; // your language
                    Locale locale = new Locale(languageToLoad);
                    Locale.setDefault(locale);
                    Configuration config = new Configuration();
                    config.locale = locale;

                    getBaseContext().getResources().updateConfiguration(config,
                            getBaseContext().getResources().getDisplayMetrics());


                }
                if (modelLogin.getStudentData().getLanguageName().equalsIgnoreCase("english")) {
                    String languageToLoad = "en"; // your language
                    Locale locale = new Locale(languageToLoad);
                    Locale.setDefault(locale);
                    Configuration config = new Configuration();
                    config.locale = locale;
                    getBaseContext().getResources().updateConfiguration(config,
                            getBaseContext().getResources().getDisplayMetrics());
                }

            }
        }
        setContentView(R.layout.activity_library);
        context = ActivityLibrary.this;
        initial();
    }

    void initial() {
        ivBack = findViewById(R.id.ivBack);
        tvHeader = (CustomTextExtraBold) findViewById(R.id.tvHeader);
        book = findViewById(R.id.book);
        notes = findViewById(R.id.note);
        oldPaper = findViewById(R.id.oldPapers);
        if (!ProjectUtils.checkConnection(context)) {
            Toast.makeText(context, getResources().getString(R.string.NoInternetConnection), Toast.LENGTH_SHORT).show();
        }

            tvHeader.setText("" + getResources().getString(R.string.Library));
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        book.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, ActivityPdf.class).putExtra("from","books"));
            }
        });
        notes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, ActivityPdf.class).putExtra("from","notes"));
            }
        });
        oldPaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, ActivityPdf.class).putExtra("from","oldpaper"));
            }
        });
    }
}