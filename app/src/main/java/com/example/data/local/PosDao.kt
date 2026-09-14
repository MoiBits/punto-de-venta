package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppSettingsEntity
import com.example.data.model.CashShiftEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {

    // --- PRODUCTS ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :productId")
    suspend fun deductStock(productId: Long, quantity: Int)

    @Query("UPDATE products SET stock = :newStock, updatedAt = :timestamp WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Int, timestamp: Long = System.currentTimeMillis())

    // --- SALES ---
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: Long): SaleEntity?

    @Query("SELECT * FROM sales WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getSalesBetween(startTime: Long, endTime: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE isSynced = 0")
    suspend fun getUnsyncedSales(): List<SaleEntity>

    @Query("UPDATE sales SET isSynced = 1 WHERE id IN (:saleIds)")
    suspend fun markSalesAsSynced(saleIds: List<Long>)

    @Query("UPDATE sales SET isCancelled = 1 WHERE id = :saleId")
    suspend fun cancelSale(saleId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Update
    suspend fun updateSale(sale: SaleEntity)

    // --- SALE ITEMS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getSaleItems(saleId: Long): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItemsSync(saleId: Long): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items")
    fun getAllSaleItems(): Flow<List<SaleItemEntity>>

    // --- CASH SHIFTS ---
    @Query("SELECT * FROM cash_shifts WHERE isOpen = 1 ORDER BY openingTime DESC LIMIT 1")
    fun getActiveShift(): Flow<CashShiftEntity?>

    @Query("SELECT * FROM cash_shifts WHERE isOpen = 1 ORDER BY openingTime DESC LIMIT 1")
    suspend fun getActiveShiftSync(): CashShiftEntity?

    @Query("SELECT * FROM cash_shifts ORDER BY openingTime DESC")
    fun getAllShifts(): Flow<List<CashShiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: CashShiftEntity): Long

    @Update
    suspend fun updateShift(shift: CashShiftEntity)

    // --- SETTINGS ---
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettingsEntity)
}
