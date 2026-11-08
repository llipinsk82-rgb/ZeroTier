package net.kaaass.zerotierfix.ui.tv;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** TV-safe export that does not depend on an installed document picker. */
public final class TvBackupExporter {
    private TvBackupExporter() { }

    public static String filename(Date date) {
        return "blackserv-zerotier-tv-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT)
                .format(date) + ".json";
    }

    public static String save(Context context) throws Exception {
        String name = filename(new Date());
        byte[] bytes = TvBackup.exportSettings(context).getBytes(StandardCharsets.UTF_8);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri target = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (target == null) throw new IOException("System nie udostępnił folderu Pobrane");
            try (OutputStream out = context.getContentResolver().openOutputStream(target, "w")) {
                if (out == null) throw new IOException("Nie można otworzyć pliku w Pobranych");
                out.write(bytes);
            } catch (Exception failure) {
                context.getContentResolver().delete(target, null, null);
                throw failure;
            }
            return "Pobrane / " + name;
        }

        File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (directory == null) throw new IOException("Brak pamięci na kopię ustawień");
        File backup = new File(directory, name);
        try (OutputStream out = new FileOutputStream(backup)) {
            out.write(bytes);
        }
        return backup.getAbsolutePath();
    }
}
