package com.example.data

interface ReceiptDao {
    fun insert(receipt: Receipt): Long
    fun getRecentReceipts(): List<Receipt>
    fun getReceiptById(id: Int): Receipt?
    fun getAllReceipts(): List<Receipt>
    fun getReceiptCount(): Int
}

class MockReceiptDao : ReceiptDao {
    private val receipts = mutableListOf<Receipt>()

    override fun insert(receipt: Receipt): Long {
        receipts.add(receipt)
        return receipts.size.toLong()
    }

    override fun getRecentReceipts(): List<Receipt> =
        receipts.sortedByDescending { it.timestamp }.take(10)

    override fun getReceiptById(id: Int): Receipt? =
        receipts.getOrNull(id)

    override fun getAllReceipts(): List<Receipt> =
        receipts.sortedByDescending { it.timestamp }

    override fun getReceiptCount(): Int = receipts.size
}
