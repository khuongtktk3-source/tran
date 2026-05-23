package com.example.data.database

import androidx.room.*
import com.example.model.AutomationStep
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val stepsJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    val imageBase64: String,
    val width: Int,
    val height: Int,
    val createdAt: Long = System.currentTimeMillis()
)

class DatabaseConverters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val stepsType = Types.newParameterizedType(List::class.java, AutomationStep::class.java)
    private val adapter = moshi.adapter<List<AutomationStep>>(stepsType)

    @TypeConverter
    fun fromStepsList(steps: List<AutomationStep>?): String {
        return if (steps == null) "[]" else adapter.toJson(steps)
    }

    @TypeConverter
    fun toStepsList(stepsJson: String?): List<AutomationStep> {
        if (stepsJson.isNullOrEmpty()) return emptyList()
        return try {
            adapter.fromJson(stepsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@Dao
interface MacroDao {
    @Query("SELECT * FROM macros ORDER BY createdAt DESC")
    fun getAllMacros(): Flow<List<MacroEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: MacroEntity)

    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun deleteMacroById(id: Int)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY createdAt DESC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Int)

    @Query("DELETE FROM templates WHERE name = :name")
    suspend fun deleteTemplateByName(name: String)
}

@Database(entities = [MacroEntity::class, TemplateEntity::class], version = 1, exportSchema = false)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun macroDao(): MacroDao
    abstract fun templateDao(): TemplateDao
}
