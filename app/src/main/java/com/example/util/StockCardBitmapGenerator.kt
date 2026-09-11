package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.entity.ProductEntity
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CardStyle(
    val title: String,
    val bgStartColor: Int,
    val bgEndColor: Int,
    val cardBgColor: Int,
    val primaryTextColor: Int,
    val accentColor: Int,
    val priceTextColor: Int
) {
    OCEAN(
        title = "Морской бриз",
        bgStartColor = Color.parseColor("#E0F7FA"),
        bgEndColor = Color.parseColor("#B2EBF2"),
        cardBgColor = Color.parseColor("#FFFFFF"),
        primaryTextColor = Color.parseColor("#004F58"),
        accentColor = Color.parseColor("#00838F"),
        priceTextColor = Color.parseColor("#006874")
    ),
    DARK_PREMIUM(
        title = "Тёмный Премиум",
        bgStartColor = Color.parseColor("#121415"),
        bgEndColor = Color.parseColor("#1E232A"),
        cardBgColor = Color.parseColor("#262D37"),
        primaryTextColor = Color.parseColor("#FFFFFF"),
        accentColor = Color.parseColor("#4DD8EC"),
        priceTextColor = Color.parseColor("#81C784")
    ),
    EMERALD(
        title = "Сочный изумруд",
        bgStartColor = Color.parseColor("#E8F5E9"),
        bgEndColor = Color.parseColor("#C8E6C9"),
        cardBgColor = Color.parseColor("#FFFFFF"),
        primaryTextColor = Color.parseColor("#1B5E20"),
        accentColor = Color.parseColor("#2E7D32"),
        priceTextColor = Color.parseColor("#2E7D32")
    ),
    PURPLE(
        title = "Лавандовый",
        bgStartColor = Color.parseColor("#F3E5F5"),
        bgEndColor = Color.parseColor("#E1BEE7"),
        cardBgColor = Color.parseColor("#FFFFFF"),
        primaryTextColor = Color.parseColor("#4A148C"),
        accentColor = Color.parseColor("#7B1FA2"),
        priceTextColor = Color.parseColor("#6A1B9A")
    )
}

object StockCardBitmapGenerator {

    fun generateSingleProductCard(
        product: ProductEntity,
        shopName: String = "Наш Магазин",
        contactInfo: String = "",
        customNote: String = "Товар в наличии!",
        style: CardStyle = CardStyle.OCEAN
    ): Bitmap {
        val width = 1080
        val height = 1200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Background Gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                style.bgStartColor, style.bgEndColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Main Center Card Container
        val cardMargin = 60f
        val cardRect = RectF(
            cardMargin,
            cardMargin + 40f,
            width - cardMargin,
            height - cardMargin - 40f
        )
        val cardBgPaint = Paint().apply {
            color = style.cardBgColor
            isAntiAlias = true
            setShadowLayer(20f, 0f, 10f, Color.parseColor("#20000000"))
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, cardBgPaint)

        // Inner Padding bounds
        val leftX = cardRect.left + 60f
        val rightX = cardRect.right - 60f
        var currentY = cardRect.top + 70f

        // 3. Shop Header Banner
        val shopTitlePaint = Paint().apply {
            color = style.accentColor
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(shopName.ifBlank { "Наш Магазин" }, leftX, currentY, shopTitlePaint)

        if (contactInfo.isNotBlank()) {
            val contactPaint = Paint().apply {
                color = style.primaryTextColor
                alpha = 180
                textSize = 28f
                isAntiAlias = true
            }
            canvas.drawText(contactInfo, leftX, currentY + 42f, contactPaint)
            currentY += 45f
        }

        currentY += 60f

        // Stock Badge ("В НАЛИЧИИ")
        val badgeText = "В НАЛИЧИИ (${product.currentStock.toInt()} ${product.unit})"
        val badgePaint = Paint().apply {
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val textWidth = badgePaint.measureText(badgeText)
        val badgePadH = 28f
        val badgePadV = 16f
        val badgeRect = RectF(
            leftX,
            currentY - 30f,
            leftX + textWidth + (badgePadH * 2),
            currentY + 16f
        )
        val badgeBgPaint = Paint().apply {
            color = Color.parseColor("#2E7D32")
            isAntiAlias = true
        }
        canvas.drawRoundRect(badgeRect, 20f, 20f, badgeBgPaint)

        badgePaint.color = Color.WHITE
        canvas.drawText(badgeText, leftX + badgePadH, currentY, badgePaint)

        currentY += 80f

        // Category Tag (If available)
        if (product.category.isNotBlank() && product.category != "Все") {
            val catPaint = Paint().apply {
                color = style.primaryTextColor
                alpha = 160
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("Категория: ${product.category}", leftX, currentY, catPaint)
            currentY += 50f
        }

        // Divider Line
        val dividerPaint = Paint().apply {
            color = style.primaryTextColor
            alpha = 40
            strokeWidth = 3f
        }
        canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)
        currentY += 80f

        // 4. Product Name (Large & Bold)
        val productNamePaint = Paint().apply {
            color = style.primaryTextColor
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Simple text wrap for product name if too long
        val maxTextWidth = rightX - leftX
        val words = product.name.split(" ")
        var line = ""
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (productNamePaint.measureText(testLine) > maxTextWidth) {
                canvas.drawText(line, leftX, currentY, productNamePaint)
                currentY += 70f
                line = word
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, leftX, currentY, productNamePaint)
            currentY += 80f
        }

        // 5. Price Tag Container
        val priceBoxY = currentY + 20f
        val priceBoxHeight = 160f
        val priceBoxRect = RectF(
            leftX,
            priceBoxY,
            rightX,
            priceBoxY + priceBoxHeight
        )
        val priceBoxBgPaint = Paint().apply {
            color = style.bgStartColor
            isAntiAlias = true
        }
        canvas.drawRoundRect(priceBoxRect, 28f, 28f, priceBoxBgPaint)

        val priceLabelPaint = Paint().apply {
            color = style.primaryTextColor
            alpha = 180
            textSize = 28f
            isAntiAlias = true
        }
        canvas.drawText("Цена:", leftX + 40f, priceBoxY + 55f, priceLabelPaint)

        val formattedPrice = formatCurrency(product.sellingPrice)
        val priceValuePaint = Paint().apply {
            color = style.priceTextColor
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("$formattedPrice ₽ / ${product.unit}", leftX + 40f, priceBoxY + 125f, priceValuePaint)

        currentY = priceBoxY + priceBoxHeight + 80f

        // 6. Custom Note / Footer
        if (customNote.isNotBlank()) {
            val notePaint = Paint().apply {
                color = style.primaryTextColor
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }
            canvas.drawText("💬 $customNote", leftX, currentY, notePaint)
        }

        // Watermark Date at Bottom
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val datePaint = Paint().apply {
            color = style.primaryTextColor
            alpha = 120
            textSize = 24f
            isAntiAlias = true
        }
        canvas.drawText("Актуально на $dateStr • Sale Manager POS", leftX, cardRect.bottom - 40f, datePaint)

        return bitmap
    }

    fun generateCatalogShowcaseCard(
        products: List<ProductEntity>,
        shopName: String = "Наш Магазин",
        contactInfo: String = "",
        style: CardStyle = CardStyle.OCEAN
    ): Bitmap {
        val width = 1080
        val itemsToShow = products.take(8)
        val height = 450 + (itemsToShow.size * 110)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                style.bgStartColor, style.bgEndColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Card Container
        val cardMargin = 50f
        val cardRect = RectF(cardMargin, cardMargin, width - cardMargin, height - cardMargin)
        val cardBgPaint = Paint().apply {
            color = style.cardBgColor
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 40f, 40f, cardBgPaint)

        val leftX = cardRect.left + 50f
        val rightX = cardRect.right - 50f
        var currentY = cardRect.top + 70f

        // Shop Title
        val shopTitlePaint = Paint().apply {
            color = style.accentColor
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(shopName.ifBlank { "Наш Магазин" }, leftX, currentY, shopTitlePaint)

        val badgeText = "В НАЛИЧИИ"
        val badgePaint = Paint().apply {
            color = Color.parseColor("#2E7D32")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(badgeText, rightX - 160f, currentY, badgePaint)

        currentY += 40f
        if (contactInfo.isNotBlank()) {
            val contactPaint = Paint().apply {
                color = style.primaryTextColor
                alpha = 180
                textSize = 28f
                isAntiAlias = true
            }
            canvas.drawText(contactInfo, leftX, currentY, contactPaint)
            currentY += 35f
        }

        currentY += 30f
        val dividerPaint = Paint().apply {
            color = style.primaryTextColor
            alpha = 40
            strokeWidth = 3f
        }
        canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)
        currentY += 50f

        // Product Items List
        val namePaint = Paint().apply {
            color = style.primaryTextColor
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val stockPaint = Paint().apply {
            color = style.primaryTextColor
            alpha = 160
            textSize = 26f
            isAntiAlias = true
        }

        val pricePaint = Paint().apply {
            color = style.priceTextColor
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        for (product in itemsToShow) {
            val displayName = if (product.name.length > 32) product.name.take(30) + "..." else product.name
            canvas.drawText(displayName, leftX, currentY, namePaint)

            val priceStr = "${formatCurrency(product.sellingPrice)} ₽"
            val priceWidth = pricePaint.measureText(priceStr)
            canvas.drawText(priceStr, rightX - priceWidth, currentY, pricePaint)

            canvas.drawText("${product.currentStock.toInt()} ${product.unit}", leftX, currentY + 34f, stockPaint)

            currentY += 80f
            canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)
            currentY += 30f
        }

        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val datePaint = Paint().apply {
            color = style.primaryTextColor
            alpha = 120
            textSize = 22f
            isAntiAlias = true
        }
        canvas.drawText("Актуальные цены и наличие на $dateStr", leftX, cardRect.bottom - 30f, datePaint)

        return bitmap
    }

    fun shareBitmap(context: Context, bitmap: Bitmap, caption: String = "Витрина товаров в наличии") {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "stock_showcase_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Поделиться карточкой с покупателем"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Boolean {
        return try {
            val filename = "StockCard_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SaleManager")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale("ru", "RU"))
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 2
        return format.format(amount)
    }
}
