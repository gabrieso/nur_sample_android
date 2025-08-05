package example.nordicid.com.nursampleandroid.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ScanEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ScanDao scanDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "scan_database")
                            .allowMainThreadQueries() // Simple for now; later move to background
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
