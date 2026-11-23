package com.bisc.flauncher.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.bisc.flauncher.data.model.Category
import com.bisc.flauncher.data.model.CategoryMember
import com.bisc.flauncher.data.model.GridItemType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY orderIndex ASC, id ASC")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM category_members ORDER BY orderIndex ASC, id ASC")
    fun observeMembers(): Flow<List<CategoryMember>>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM categories")
    suspend fun maxOrderIndex(): Int

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Update
    suspend fun updateAll(categories: List<Category>)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM category_members WHERE categoryId = :categoryId")
    suspend fun maxMemberOrder(categoryId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMember(member: CategoryMember): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMembers(members: List<CategoryMember>)

    @Query("DELETE FROM category_members WHERE categoryId = :categoryId AND type = :type AND payload = :payload")
    suspend fun deleteMember(categoryId: Long, type: GridItemType, payload: String)

    @Query("DELETE FROM category_members WHERE type = :type AND payload = :payload")
    suspend fun deleteEverywhere(type: GridItemType, payload: String)

    @Query("DELETE FROM category_members WHERE categoryId = :categoryId")
    suspend fun clearMembers(categoryId: Long)

    @Query("UPDATE category_members SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun setMemberOrder(id: Long, orderIndex: Int)

    @Transaction
    suspend fun reorderMembers(ordered: List<CategoryMember>) {
        ordered.forEachIndexed { index, member -> setMemberOrder(member.id, index) }
    }

    @Transaction
    suspend fun replaceMembers(categoryId: Long, members: List<CategoryMember>) {
        clearMembers(categoryId)
        insertMembers(members)
    }
}
