package example.nordicid.com.nursampleandroid.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ScanDao {
    @Insert
    void insert(ScanEntity scan);
    @Query("DELETE FROM scan_table")
    void clearAll();

    @Query("SELECT * FROM scan_table WHERE wagon=:wagon AND section=:section")
    List<ScanEntity> getScansForSection(String wagon, String section);

    @Query("DELETE FROM scan_table WHERE wagon=:wagon AND section=:section")
    void clearScansForSection(String wagon, String section);
    @Query("SELECT COUNT(*) FROM scan_table WHERE wagon=:wagon AND section=:section AND isExpected=1")
    int getFoundExpectedCount(String wagon, String section);


}
