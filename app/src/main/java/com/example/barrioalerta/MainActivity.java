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
        findViewById(R.id.location_button).setOnClickListener(v -> askForLocation());
        findViewById(R.id.add_button).setOnClickListener(v -> showForm());
        showReports();
    }

    private void askForLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        loadLocation();
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                                      @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                loadLocation();
            } else {
                locationText.setText("Permiso denegado. Puedes guardar reportes sin ubicación.");
            }
        }
    }

    private void loadLocation() {
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        String provider = fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
        if (!manager.isProviderEnabled(provider)) {
            locationText.setText("Activa la ubicación del dispositivo e inténtalo de nuevo.");
            return;
        }
        locationText.setText("Obteniendo ubicación…");
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                manager.getCurrentLocation(provider, new CancellationSignal(), getMainExecutor(), this::useLocation);
            } else {
                manager.requestSingleUpdate(provider, this::useLocation, null);
            }
        } catch (SecurityException e) {
            locationText.setText("No se pudo acceder a la ubicación.");
        }
    }

    private void useLocation(Location location) {
        if (location == null) {
            locationText.setText("Sin señal de ubicación. Inténtalo de nuevo.");
            return;
        }
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        locationText.setText(String.format(Locale.US, "Coordenadas: %.5f, %.5f", latitude, longitude));
        fetchWeather(latitude, longitude);
    }

    /** Consulta REST por HTTPS fuera del hilo principal para no congelar la interfaz. */
    private void fetchWeather(double lat, double lon) {
        weatherText.setText("Consultando Open-Meteo…");
        network.execute(() -> {
            HttpURLConnection connection = null;
            String message;
            try {
                String address = String.format(Locale.US,
                        "https://api.open-meteo.com/v1/forecast?latitude=%.5f&longitude=%.5f&current=temperature_2m,wind_speed_10m",
                        lat, lon);
                connection = (HttpURLConnection) new URL(address).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                if (connection.getResponseCode() != 200) throw new Exception("HTTP " + connection.getResponseCode());
                try (InputStream input = connection.getInputStream()) {
                    JSONObject current = new JSONObject(new String(input.readAllBytes(), StandardCharsets.UTF_8))
                            .getJSONObject("current");
                    message = String.format(Locale.getDefault(), "Clima actual: %.1f °C · viento %.1f km/h",
                            current.getDouble("temperature_2m"), current.getDouble("wind_speed_10m"));
                }
            } catch (Exception e) {
                message = "Clima no disponible. Los reportes siguen funcionando sin internet.";
            } finally {
                if (connection != null) connection.disconnect();
            }
            final String result = message;
            runOnUiThread(() -> weatherText.setText(result));
        });
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
            card.setOnClickListener(v -> showReportDetail(report));
            reportList.addView(card);
        }
    }

    private void showReportDetail(ReportDb.Report report) {
        String coordinates = report.lat == null ? "Sin coordenadas" :
                String.format(Locale.US, "%.5f, %.5f", report.lat, report.lon);
        new MaterialAlertDialogBuilder(this)
                .setTitle(report.title)
                .setMessage("Categoría: " + report.category + "\nEstado: " + report.status +
                        "\nUbicación: " + coordinates + "\n\n" + report.description)
                .setPositiveButton(report.status.equals("Pendiente") ? "Marcar resuelto" : "Reabrir", (d, w) -> {
                    db.toggleStatus(report.id, report.status);
                    showReports();
                })
                .setNeutralButton("Eliminar", (d, w) -> new MaterialAlertDialogBuilder(this)
                        .setTitle("Eliminar reporte")
                        .setMessage("Esta acción no se puede deshacer.")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Eliminar", (confirm, button) -> {
                            db.delete(report.id);
                            showReports();
                        }).show())
                .setNegativeButton("Cerrar", null).show();
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