package com.example.data.model

data class CoinPackage(
    val id: String,
    val name: String,
    val baseCoins: Int,
    val bonusCoins: Int,
    val priceUsd: Double,
    val tag: String? = null,
    val isPopular: Boolean = false,
    val isBestValue: Boolean = false
) {
    val totalCoins: Int get() = baseCoins + bonusCoins
    val formattedPrice: String get() = "$${String.format("%.2f", priceUsd)}"

    companion object {
        const val PAYPAL_RECEIVER_EMAIL = "juan8191327@gmail.com"

        val PACKAGES = listOf(
            CoinPackage(
                id = "pack_starter",
                name = "Starter Booster",
                baseCoins = 500,
                bonusCoins = 0,
                priceUsd = 0.99,
                tag = "Quick Start"
            ),
            CoinPackage(
                id = "pack_popular",
                name = "Influencer Pack",
                baseCoins = 2500,
                bonusCoins = 300,
                priceUsd = 3.99,
                tag = "+12% BONUS",
                isPopular = true
            ),
            CoinPackage(
                id = "pack_growth",
                name = "Pro Creator Pack",
                baseCoins = 6500,
                bonusCoins = 1000,
                priceUsd = 7.99,
                tag = "+15% BONUS"
            ),
            CoinPackage(
                id = "pack_vip",
                name = "VIP Superstar Pack",
                baseCoins = 15000,
                bonusCoins = 3500,
                priceUsd = 14.99,
                tag = "BEST VALUE",
                isBestValue = true
            ),
            CoinPackage(
                id = "pack_viral",
                name = "Mega Viral Rocket",
                baseCoins = 50000,
                bonusCoins = 15000,
                priceUsd = 39.99,
                tag = "+30% MEGA BONUS"
            )
        )
    }
}
