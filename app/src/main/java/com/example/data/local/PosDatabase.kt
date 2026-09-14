package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppSettingsEntity
import com.example.data.model.CashShiftEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        CashShiftEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun posDao(): PosDao

    companion object {
        @Volatile
        private var INSTANCE: PosDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): PosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PosDatabase::class.java,
                    "pos_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(PosDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class PosDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.posDao())
                }
            }
        }

        suspend fun populateInitialData(dao: PosDao) {
            // Initial settings (Bolivia LatAm default)
            dao.saveSettings(
                AppSettingsEntity(
                    id = 1,
                    businessName = "Comercial & Retail Bolivia S.R.L.",
                    businessTaxId = "1023456789", // NIT Bolivia
                    businessAddress = "Av. 6 de Agosto #2450, Sopocachi, La Paz",
                    businessPhone = "+591 2 2441234",
                    businessEmail = "ventas@comerciobolivia.bo",
                    currencySymbol = "Bs.",
                    currencyCode = "BOB",
                    defaultTaxRate = 0.13, // 13% IVA Bolivia
                    invoicePrefix = "FAC-",
                    nextFolioNumber = 1001,
                    isOfflineSimulated = false,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    thermalPaperWidthMm = 80
                )
            )

            // Initial open shift
            dao.insertShift(
                CashShiftEntity(
                    cashierName = "Carlos Méndez (Cajero Principal)",
                    openingTime = System.currentTimeMillis() - 3600000,
                    initialCash = 500.0, // Bs. 500
                    expectedCash = 500.0,
                    isOpen = true
                )
            )

            // Initial sample catalog with barcodes, categories, stock and prices in Bs.
            val initialProducts = listOf(
                ProductEntity(
                    barcode = "750105530123",
                    name = "Café Americano 12oz",
                    category = "Bebidas",
                    costPrice = 6.00,
                    salePrice = 15.00,
                    stock = 45,
                    minStock = 10,
                    taxRate = 0.13
                ),
                ProductEntity(
                    barcode = "750105530124",
                    name = "Capuchino Vainilla 16oz",
                    category = "Bebidas",
                    costPrice = 9.00,
                    salePrice = 22.00,
                    stock = 30,
                    minStock = 8,
                    taxRate = 0.13
                ),
                ProductEntity(
                    barcode = "750105530125",
                    name = "Agua Mineral 600ml",
                    category = "Bebidas",
                    costPrice = 3.50,
                    salePrice = 8.00,
                    stock = 60,
                    minStock = 15,
                    taxRate = 0.13
                ),
                ProductEntity(
                    barcode = "750223450001",
                    name = "Sándwich Pavo & Queso Gouda",
                    category = "Alimentos",
                    costPrice = 12.00,
                    salePrice = 25.00,
                    stock = 18,
                    minStock = 5,
                    taxRate = 0.13
                ),
                ProductEntity(
                    barcode = "750223450002",
                    name = "Croissant Mantequilla",
                    category = "Alimentos",
                    costPrice = 5.00,
                    salePrice = 12.00,
                    stock = 25,
                    minStock = 6,
                    taxRate = 0.13
                ),
                ProductEntity(
                    barcode = "750334560001",
                    name = "Papas Artesanales con Sal 80g",
                    category = "Snacks",
                    costPrice = 4.50,
                    salePrice = 10.00,
                    stock = 40,
                    minStock = 10,
                    taxRate = 0.13
                ),
                ProductEntity(
                    barcode = "750334560002",
                    name = "Barra Energética Almendra & Miel",
                    category = "Snacks",
                    costPrice = 4.00,
                    salePrice = 9.00,
                    stock = 3, // Low stock example
                    minStock = 8,
                    taxRate = 0.13
                ),
                ProductEntity(
                    barcode = "750445670001",
                    name = "Cable USB-C Carga Rápida 2m",
                    category = "Tecnología",
                    costPrice = 20.00,
                    salePrice = 45.00,
                    stock = 14,
                    minStock = 5,
                    taxRate = 0.13
                ),
                ProductEntity(
                    barcode = "750445670002",
                    name = "Cargador Pared 30W GaN",
                    category = "Tecnología",
                    costPrice = 45.00,
                    salePrice = 95.00,
                    stock = 9,
                    minStock = 4,
                    taxRate = 0.13
                ),
                ProductEntity(
                    barcode = "750556780001",
                    name = "Gel Antibacterial 250ml",
                    category = "Cuidado Personal",
                    costPrice = 6.00,
                    salePrice = 15.00,
                    stock = 32,
                    minStock = 10,
                    taxRate = 0.13
                ),
                ProductEntity(
                    barcode = "750556780002",
                    name = "Cuaderno Profesional Rayas",
                    category = "Papelería",
                    costPrice = 8.00,
                    salePrice = 18.00,
                    stock = 20,
                    minStock = 6,
                    taxRate = 0.13
                )
            )
            dao.insertProducts(initialProducts)
        }
    }
}
