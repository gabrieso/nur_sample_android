package example.nordicid.com.nursampleandroid.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "scan_table")
public class ScanEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String wagon;
    public String section;
    public String epc;
    public String serialAscii;
    public long timestamp;
    public boolean isExpected;
}
