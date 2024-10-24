package com.ftechiz.iscftechizz.ui.library;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.androidnetworking.interfaces.ParsedRequestListener;
import com.ftechiz.iscftechizz.R;
import com.ftechiz.iscftechizz.model.modellogin.ModelLogin;
import com.ftechiz.iscftechizz.ui.home.ActivityHome;
import com.ftechiz.iscftechizz.utils.AppConsts;
import com.ftechiz.iscftechizz.utils.ProjectUtils;
import com.ftechiz.iscftechizz.utils.sharedpref.SharedPref;
import com.ftechiz.iscftechizz.utils.widgets.CustomTextExtraBold;
import com.ftechiz.iscftechizz.utils.widgets.CustomeTextRegular;
import com.ftechiz.iscftechizz.utils.widgets.CustomeTextRegularBold;
import com.github.barteksc.pdfviewer.PDFView;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ActivityPdf extends AppCompatActivity {
    Context context;
    RecyclerView rvListPdf;
    ArrayList<String> list;
    ImageView backIv, noResult;
    SharedPref sharedPref;
    ModelLogin modelLogin;
    CustomTextExtraBold tvHeader;
    TextView downloadPercent;
    PDFView pdfView;
    static String tag = "";

    ProgressBar progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);
        context = ActivityPdf.this;
        sharedPref = SharedPref.getInstance(context);
        modelLogin = sharedPref.getUser(AppConsts.STUDENT_DATA);
        if (!ProjectUtils.checkConnection(context)) {
            Toast.makeText(context, getResources().getString(R.string.NoInternetConnection), Toast.LENGTH_SHORT).show();
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        } else {
            init();
        }

    }

    private void requestPermission() {

        Dexter.withActivity(ActivityPdf.this)
                .withPermissions(
                        Manifest.permission.READ_PHONE_STATE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {

                        if (report.areAllPermissionsGranted()) {
                            init();
                        } else {
                            Toast.makeText(context, getResources().getString(R.string.Please_allow_permissions), Toast.LENGTH_SHORT).show();
                        }


                        if (report.isAnyPermissionPermanentlyDenied()) {
                            init();
                            openSettingsDialog();
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
                        Toast.makeText(context, getResources().getString(R.string.ErrorOccurred), Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();

    }

    private void openSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getResources().getString(R.string.Please_allow_permissions));
        builder.setMessage(getResources().getString(R.string.This_app_needs_permission));
        builder.setPositiveButton(getResources().getString(R.string.GOTO_SETTINGS), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, 101);
            }
        });

        builder.setNegativeButton(getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

    private void init() {
        rvListPdf = findViewById(R.id.rvListpdf);
        backIv = findViewById(R.id.ivBack);
        pdfView = findViewById(R.id.pdfView);
        downloadPercent = findViewById(R.id.downloadPercent);
        noResult = findViewById(R.id.noResult);
        progress = findViewById(R.id.progressBar);
        tvHeader = (CustomTextExtraBold) findViewById(R.id.tvHeader);
        if (getIntent().hasExtra("from")) {
            if (getIntent().getStringExtra("from").equalsIgnoreCase("books")) {
                tvHeader.setText("" + getResources().getString(R.string.Books));
                tag = "books";
            }
            if (getIntent().getStringExtra("from").equalsIgnoreCase("notes")) {
                tvHeader.setText("" + getResources().getString(R.string.Notes));
                tag = "notes";
            }
            if (getIntent().getStringExtra("from").equalsIgnoreCase("oldpaper")) {

                tvHeader.setText("" + getResources().getString(R.string.old_papers));
                tag = "oldpapers";
            }
        }

        backIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        AndroidNetworking.post(AppConsts.BASE_URL + AppConsts.API_NOTES_PDF).
                addBodyParameter(AppConsts.BATCH_ID, "" + modelLogin.getStudentData().getBatchId()).
                addBodyParameter(AppConsts.STUDENT_ID, "" + modelLogin.getStudentData().getStudentId())
                .build().getAsObject(ModelNotes.class, new ParsedRequestListener<ModelNotes>() {
                    @Override
                    public void onResponse(ModelNotes response) {

                        if (response.getStatus().equalsIgnoreCase("true")) {

                            rvListPdf.setLayoutManager(new GridLayoutManager(context, 1));
                            AdapterCustom adapterCustom = new AdapterCustom(response, tag);
                            rvListPdf.setAdapter(adapterCustom);
                        }


                    }

                    @Override
                    public void onError(ANError anError) {
                        Toast.makeText(context, getResources().getString(R.string.Something_went_wrong), Toast.LENGTH_SHORT).show();
                    }
                });


    }


    void apiDownload(String baseurl, String fileName) {
        boolean found = false;  // Moved inside to avoid global conflict
        if (ProjectUtils.checkConnection(context)) {
            progress.setVisibility(View.VISIBLE);
            File fileFolder = new File(context.getExternalFilesDir(null).getAbsolutePath());
            if (fileFolder.isDirectory()) {
                File[] files = fileFolder.listFiles();
                for (File file : files) {
                    if (file.getName().equalsIgnoreCase(fileName)) {
                        downloadPercent.setVisibility(View.GONE);
                        Log.v("Download", "Local file found: " + fileName);
                        pdfView.fromFile(file).load();
                        pdfView.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.GONE);
                        found = true;
                        break;
                    }
                }
                if (!found) {

                    downLoadApi(baseurl, fileName);
                }
            } else {
                downLoadApi(baseurl, fileName);
            }
        } else {
            Toast.makeText(context, context.getString(R.string.NoInternetConnection), Toast.LENGTH_SHORT).show();
        }
    }

    void downLoadApi(String baseurl, String fileName) {
        File root = new File(context.getExternalFilesDir(null).getAbsolutePath());
        if (!root.exists()) {
            root.mkdirs();
        }

        downloadPercent.setVisibility(View.VISIBLE);
        AndroidNetworking.download(baseurl, root.getPath(), fileName)
                .setTag("downloadTest")
                .setPriority(Priority.HIGH)
                .build()
                .setDownloadProgressListener((bytesDownloaded, totalBytes) -> {
                    int pro = (int) (bytesDownloaded * 100 / totalBytes);
                    downloadPercent.setText(context.getString(R.string.Downloading___) + pro + " %");
                })
                .startDownload(new DownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        ProjectUtils.pauseProgressDialog();
                        downloadPercent.setVisibility(View.GONE);
                        pdfView.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.GONE);
                        apiDownload(baseurl, fileName); // Load the file once downloaded
                    }

                    @Override
                    public void onError(ANError error) {
                        downloadPercent.setVisibility(View.GONE);
                        ProjectUtils.pauseProgressDialog();
                        Toast.makeText(context, context.getString(R.string.Downloadingfailed), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onBackPressed() {

        if (getIntent().hasExtra("firebase/notice")) {
            startActivity(new Intent(getApplicationContext(), ActivityHome.class));
            finish();
        } else {

            if (rvListPdf.getVisibility() == View.GONE) {
                rvListPdf.setVisibility(View.VISIBLE);
                pdfView.setVisibility(View.GONE);
                if (getIntent().hasExtra("from")) {
                    if (getIntent().getStringExtra("from").equalsIgnoreCase("books")) {
                        tvHeader.setText("" + getResources().getString(R.string.Books));
                    }
                    if (getIntent().getStringExtra("from").equalsIgnoreCase("notes")) {
                        tvHeader.setText("" + getResources().getString(R.string.Notes));
                    }
                    if (getIntent().getStringExtra("from").equalsIgnoreCase("oldpaper")) {

                        tvHeader.setText("" + getResources().getString(R.string.old_papers));
                    }
                }

            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    class AdapterCustom extends RecyclerView.Adapter<AdapterCustom.holderclass> {
        ModelNotes list;
        String tag;

        public AdapterCustom(ModelNotes list, String tag) {
            this.list = list;
            this.tag = tag;
        }

        @NonNull
        @Override
        public holderclass onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Inflate a new view for each item
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_pdf, parent, false);
            return new holderclass(view);
        }

        @Override
        public void onBindViewHolder(@NonNull holderclass holder, int position) {
            holder.textTv.setTextSize(13f);
            holder.titleTv.setTextSize(15f);

            // Update views based on the tag and position
            if (tag.equalsIgnoreCase("books")) {
                ModelNotes.Book book = list.getBookPdf().get(position);
                holder.textTv.setText(holder.itemView.getContext().getString(R.string.subject) + " - " + book.getSubject());
                holder.titleTv.setText(book.getTitle());
            } else if (tag.equalsIgnoreCase("notes")) {
                ModelNotes.Notes notes = list.getNotesPdf().get(position);
                holder.textTv.setText(holder.itemView.getContext().getString(R.string.subject) + " - " + notes.getSubject() + " (" + notes.getTopic() + ")");
                holder.titleTv.setText(notes.getTitle());
            } else {
                ModelNotes.OldPapers oldPapers = list.getOldPapers().get(position);
                holder.textTv.setText(holder.itemView.getContext().getString(R.string.subject) + " - " + oldPapers.getSubject());
                holder.titleTv.setText(oldPapers.getTitle());
            }

            // Set up the click listener for each item
            holder.itemView.setOnClickListener(view -> {
                rvListPdf.setVisibility(View.GONE);
                pdfView.setVisibility(View.GONE);

                if (tag.equalsIgnoreCase("books")) {
                    String fileName = list.getBookPdf().get(position).getFileName();
                    String fileUrl = list.getBookPdf().get(position).getUrl() + fileName;
                    tvHeader.setText(list.getBookPdf().get(position).getTitle());
                    apiDownload(fileUrl, fileName);
                } else if (tag.equalsIgnoreCase("notes")) {
                    String fileName = list.getNotesPdf().get(position).getFileName();
                    String fileUrl = list.getNotesPdf().get(position).getUrl() + fileName;
                    tvHeader.setText(list.getNotesPdf().get(position).getTitle());
                    apiDownload(fileUrl, fileName);
                } else {
                    String fileName = list.getOldPapers().get(position).getFileName();
                    String fileUrl = list.getOldPapers().get(position).getUrl() + fileName;
                    tvHeader.setText(list.getOldPapers().get(position).getTitle());
                    apiDownload(fileUrl, fileName);
                }
            });
        }

        @Override
        public int getItemCount() {
            if (tag.equalsIgnoreCase("books")) {
                if (list.getBookPdf().isEmpty()) {
                    noResult.setVisibility(View.VISIBLE);
                }
                return list.getBookPdf().size();
            } else if (tag.equalsIgnoreCase("notes")) {
                if (list.getNotesPdf().isEmpty()) {
                    noResult.setVisibility(View.VISIBLE);
                }
                return list.getNotesPdf().size();
            } else {
                if (list.getOldPapers().isEmpty()) {
                    noResult.setVisibility(View.VISIBLE);
                }
                return list.getOldPapers().size();
            }
        }

        // ViewHolder class
        class holderclass extends RecyclerView.ViewHolder {
            CustomeTextRegular textTv;
            CustomeTextRegularBold titleTv;

            public holderclass(@NonNull View itemView) {
                super(itemView);
                textTv = itemView.findViewById(R.id.textTv);
                titleTv = itemView.findViewById(R.id.titleTv);
            }
        }
    }

}