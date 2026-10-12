package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** The cart is offline-first: Room is the source of truth, synced in the background. */
@Entity(tableName = "cart_lines")
data class CartLineEntity(
    @PrimaryKey val sku: String,
    val name: String,
    val unitPriceMinor: Long,
    val currency: String,
    val quantity: Int,
)

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_lines ORDER BY name")
    fun observeLines(): Flow<List<CartLineEntity>>

    @Query("UPDATE cart_lines SET quantity = :quantity WHERE sku = :sku")
    suspend fun updateQuantity(sku: String, quantity: Int): Int

    @Query("DELETE FROM cart_lines WHERE sku = :sku")
    suspend fun delete(sku: String): Int
}
