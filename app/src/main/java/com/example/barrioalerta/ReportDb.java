package com.example.barrioalerta;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** Guarda reportes en el almacenamiento privado de la aplicación. */
public class ReportDb extends SQLiteOpenHelper {
    public static final String NAME = "barrio_alerta.db";

    public ReportDb(Context context) {
        super(context, NAME, null, 1);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE reportes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "titulo TEXT NOT NULL, categoria TEXT NOT NULL, " +
                "descripcion TEXT NOT NULL, latitud REAL, longitud REAL, " +
                "estado TEXT NOT NULL DEFAULT 'Pendiente', " +
                "creado_en INTEGER NOT NULL)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // La primera versión todavía no necesita migraciones.
    }

    public long add(String title, String category, String description, Double lat, Double lon) {
        ContentValues values = new ContentValues();
        values.put("titulo", title);
        values.put("categoria", category);
        values.put("descripcion", description);
        if (lat != null && lon != null) {
            values.put("latitud", lat);
            values.put("longitud", lon);
        }
        values.put("estado", "Pendiente");
        values.put("creado_en", System.currentTimeMillis());
        return getWritableDatabase().insert("reportes", null, values);
    }

    public List<Report> all() {
        List<Report> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("reportes", null, null, null,
                null, null, "creado_en DESC, id DESC")) {
            while (cursor.moveToNext()) {
                Report report = new Report();
                report.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                report.title = cursor.getString(cursor.getColumnIndexOrThrow("titulo"));
                report.category = cursor.getString(cursor.getColumnIndexOrThrow("categoria"));
                report.description = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"));
                int latIndex = cursor.getColumnIndexOrThrow("latitud");
                int lonIndex = cursor.getColumnIndexOrThrow("longitud");
                if (!cursor.isNull(latIndex) && !cursor.isNull(lonIndex)) {
                    report.lat = cursor.getDouble(latIndex);
                    report.lon = cursor.getDouble(lonIndex);
                }
                report.status = cursor.getString(cursor.getColumnIndexOrThrow("estado"));
                report.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("creado_en"));
                result.add(report);
            }
        }
        return result;
    }

    public void toggleStatus(long id, String currentStatus) {
        ContentValues values = new ContentValues();
        values.put("estado", currentStatus.equals("Pendiente") ? "Resuelto" : "Pendiente");
        getWritableDatabase().update("reportes", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public void delete(long id) {
        getWritableDatabase().delete("reportes", "id = ?", new String[]{String.valueOf(id)});
    }

    public static class Report {
        public long id, createdAt;
        public String title, category, description, status;
        public Double lat, lon;
    }
}
