package com.example.barrioalerta;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_REQUEST = 10;
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private ReportDb db;
    private LinearLayout reportList;
    private TextView locationText, weatherText, emptyText;
    private Double latitude, longitude;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        db = new ReportDb(this);
        reportList = findViewById(R.id.report_list);
        locationText = findViewById(R.id.location_text);
        weatherText = findViewById(R.id.weather_text);
        emptyText = findViewById(R.id.empty_text);
        findViewById(R.id.add_button).setOnClickListener(v -> showForm());
        showReports();
    }

    private void showForm() {
        View form = getLayoutInflater().inflate(R.layout.dialog_report, null);
        EditText title = form.findViewById(R.id.title_input);
        EditText description = form.findViewById(R.id.description_input);
        Spinner category = form.findViewById(R.id.category_input);
        String[] categories = {"Alumbrado", "Basuras", "Vías", "Agua", "Otro"};
        category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories));
        TextView formLocation = form.findViewById(R.id.form_location);
        formLocation.setText(latitude == null ? "Se guardará sin coordenadas."
                : String.format(Locale.US, "Ubicación: %.5f, %.5f", latitude, longitude));
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Nuevo reporte")
                .setView(form)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(-1).setOnClickListener(v -> {
            String t = title.getText().toString().trim();
            String d = description.getText().toString().trim();
            if (t.length() < 4) {
                title.setError("Escribe al menos 4 caracteres");
                return;
            }
            if (d.length() < 10) {
                description.setError("Escribe al menos 10 caracteres");
                return;
            }
            long id = db.add(t, category.getSelectedItem().toString(), d, latitude, longitude);
            if (id < 0) {
                Toast.makeText(this, "No se pudo guardar", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            showReports();
            Toast.makeText(this, "Reporte guardado", Toast.LENGTH_SHORT).show();
        }));
        dialog.show();
    }

    private void showReports() {
        reportList.removeAllViews();
        List<ReportDb.Report> reports = db.all();
        emptyText.setVisibility(reports.isEmpty() ? View.VISIBLE : View.GONE);
        for (ReportDb.Report report : reports) {
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.bottomMargin = dp(12);
            card.setLayoutParams(cardParams);
            card.setRadius(dp(16));
            card.setCardElevation(dp(1));
            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(16), dp(14), dp(16), dp(14));
            TextView title = new TextView(this);
            title.setText(report.title);
            title.setTextSize(18);
            title.setTextColor(0xff123d35);
            title.setTypeface(null, 1);
            content.addView(title);
            TextView detail = new TextView(this);
            detail.setText(report.category + " · " + report.status + "\n" +
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(report.createdAt)));
            detail.setTextColor(0xff52635d);
            detail.setPadding(0, dp(6), 0, 0);
            content.addView(detail);
            card.addView(content);
            reportList.addView(card);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        network.shutdownNow();
        db.close();
        super.onDestroy();
    }
}