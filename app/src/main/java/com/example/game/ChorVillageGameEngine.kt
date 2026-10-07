package com.example.game

import com.example.data.model.CollectibleItemConfig
import com.example.data.model.DefaultGameCatalog
import com.example.data.model.GameSettings
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

enum class AlertState(val labelBn: String, val labelEn: String, val badgeEmoji: String) {
    SAFE("নিরাপদ", "SAFE", "🟢"),
    ALERT("সতর্ক!", "ALERT", "🟡"),
    CAUGHT("ধরা পড়েছো!", "CAUGHT!", "🔴")
}

enum class PlayerAnimState {
    IDLE,
    SNEAK_WALK,
    RUN,
    COLLECTING,
    SURPRISED,
    CELEBRATING
}

enum class StructureType {
    HOUSE,
    SHOP,
    COW_SHED,
    MANGO_TREE,
    COCONUT_TREE,
    GARDEN,
    FIELD,
    POND
}

data class VillageStructure(
    val id: String,
    val titleBn: String,
    val type: StructureType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val roofColorHex: Long = 0xFFD97706
)

data class WorldCollectible(
    val uid: String,
    val config: CollectibleItemConfig,
    val spawnX: Float,
    val spawnY: Float,
    var x: Float = spawnX,
    var y: Float = spawnY,
    var isAvailable: Boolean = true,
    var respawnTimerSec: Float = 0f,
    var isFollowingPlayer: Boolean = false,
    var followTimerSec: Float = 0f,
    var isBonusSpawn: Boolean = false
)

data class VillageNpc(
    val id: String,
    val nameBn: String,
    val shirtColorHex: Long,
    val waypoints: List<Pair<Float, Float>>,
    var currentWaypointIndex: Int = 0,
    var x: Float = waypoints.first().first,
    var y: Float = waypoints.first().second,
    var facingX: Float = 1f,
    var facingY: Float = 0f,
    var alertProgress: Float = 0f,
    var speechBubble: String? = null,
    var speechTimerSec: Float = 0f,
    var cooldownSec: Float = 0f
)

data class FloatingPopup(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val colorHex: Long,
    var ageSec: Float = 0f,
    val durationSec: Float = 1.3f
)

enum class RandomEventType(val titleBn: String, val subtitleBn: String, val colorHex: Long) {
    LUCKY_MOMENT("🍀 Lucky Moment!", "পরের আইটেমে 2x কয়েন পাওয়া যাবে!", 0xFF00E676),
    NPC_ALERT("🚨 NPC Alert!", "কয়েকজন গ্রামবাসী চারপাশ খুঁজছে! সাবধানে থাকো!", 0xFFFFAB00),
    SPEED_MODE("⚡ Speed Mode!", "চোরের চলাচলের গতি ৮ সেকেন্ডের জন্য বেড়ে গেছে!", 0xFF00E5FF),
    TREASURE_APPEARED("💰 Treasure Appeared!", "গ্রামে একটি বিরল গুপ্তধন (💎 +250) দেখা গেছে!", 0xFFFFC837)
}

data class FootstepParticle(
    val x: Float,
    val y: Float,
    var ageSec: Float = 0f,
    val maxAgeSec: Float = 0.55f
)

class ChorVillageGameEngine(
    val worldWidth: Float = 2200f,
    val worldHeight: Float = 2200f
) {
    // Player position & state
    var playerX: Float = 1100f
    var playerY: Float = 1150f
    var playerFacingX: Float = 0f
    var playerFacingY: Float = 1f
    var playerAnimState: PlayerAnimState = PlayerAnimState.IDLE
    var animTimeSec: Float = 0f

    // Movement & Action timers
    var collectAnimTimerSec: Float = 0f
    var surprisedTimerSec: Float = 0f
    var celebrateTimerSec: Float = 0f
    var isSprintPressed: Boolean = false
    var sprintStamina: Float = 1f // 0..1
    var specialCharge: Float = 0.5f // 0..1, full = can trigger Sneaky Mode

    // Active buffs & random events
    var speedBoostTimerSec: Float = 0f
    var sneakyModeTimerSec: Float = 0f
    var npcAlertEventTimerSec: Float = 0f
    var nextItemDoubleCoins: Boolean = false
    var activeEventBanner: RandomEventType? = null
    var activeEventBannerTimerSec: Float = 0f
    private var nextRandomEventCountdownSec: Float = 18f

    // Round stats
    var totalSecondsRemaining: Float = 150f
    var roundDurationSeconds: Int = 150
    var coinsEarned: Long = 0L
    var itemsCollected: Int = 0
    var currentCombo: Int = 1
    var bestCombo: Int = 1
    var comboTimerSec: Float = 0f
    val maxComboWindowSec: Float = 4.5f

    // Item counters for achievements & summary
    val itemCountsByType = mutableMapOf<String, Int>()
    val collectionTimestampsSec = mutableListOf<Float>()
    var reachedSpeedThiefInRound: Boolean = false
    var elapsedRoundSec: Float = 0f

    // Alert state
    var overallAlertState: AlertState = AlertState.SAFE
    var highestNpcAlertProgress: Float = 0f
    var activeWarningMessage: String? = null

    // World entities
    val structures = mutableListOf<VillageStructure>()
    val collectibles = mutableListOf<WorldCollectible>()
    val npcs = mutableListOf<VillageNpc>()
    val floatingPopups = mutableListOf<FloatingPopup>()
    val footsteps = mutableListOf<FootstepParticle>()

    private var popupIdCounter = 1L
    private var footstepSpawnAccum = 0f

    fun initializeRound(
        settings: GameSettings,
        itemCatalog: List<CollectibleItemConfig>
    ) {
        roundDurationSeconds = settings.roundDurationSeconds.toInt().coerceIn(30, 600)
        totalSecondsRemaining = roundDurationSeconds.toFloat()
        elapsedRoundSec = 0f
        coinsEarned = 0L
        itemsCollected = 0
        currentCombo = 1
        bestCombo = 1
        comboTimerSec = 0f
        itemCountsByType.clear()
        collectionTimestampsSec.clear()
        reachedSpeedThiefInRound = false

        playerX = 1100f
        playerY = 1150f
        playerFacingX = 0f
        playerFacingY = 1f
        playerAnimState = PlayerAnimState.IDLE
        collectAnimTimerSec = 0f
        surprisedTimerSec = 0f
        celebrateTimerSec = 0f
        sprintStamina = 1f
        specialCharge = 0.6f
        speedBoostTimerSec = 0f
        sneakyModeTimerSec = 0f
        npcAlertEventTimerSec = 0f
        nextItemDoubleCoins = false
        activeEventBanner = null
        activeEventBannerTimerSec = 0f
        nextRandomEventCountdownSec = 16f

        floatingPopups.clear()
        footsteps.clear()
        buildVillageStructures()
        buildVillageNpcs()
        spawnWorldCollectibles(itemCatalog.ifEmpty { DefaultGameCatalog.defaultItems })
    }

    private fun buildVillageStructures() {
        structures.clear()
        // Village Houses
        structures.add(VillageStructure("h1", "মোড়ল বাড়ি", StructureType.HOUSE, 520f, 480f, 210f, 150f, 0xFFE65100))
        structures.add(VillageStructure("h2", "রহিম চাচার ঘর", StructureType.HOUSE, 1580f, 460f, 195f, 145f, 0xFFC62828))
        structures.add(VillageStructure("h3", "মিনা খালার কুটির", StructureType.HOUSE, 460f, 1480f, 190f, 140f, 0xFFAD1457))
        structures.add(VillageStructure("h4", "করিম ভাইয়ের বাড়ি", StructureType.HOUSE, 1620f, 1540f, 205f, 145f, 0xFF6A1B9A))
        structures.add(VillageStructure("h5", "গৃহস্থ বাড়ি", StructureType.HOUSE, 1080f, 420f, 200f, 140f, 0xFFD84315))

        // Small Shops
        structures.add(VillageStructure("s1", "চায়ের দোকান", StructureType.SHOP, 860f, 940f, 150f, 115f, 0xFF00838F))
        structures.add(VillageStructure("s2", "মুদি দোকান", StructureType.SHOP, 1340f, 940f, 155f, 115f, 0xFF2E7D32))

        // Cow Shed / Animal Area
        structures.add(VillageStructure("cow1", "গোয়ালঘর", StructureType.COW_SHED, 1080f, 1740f, 230f, 150f, 0xFF8D6E63))

        // Pond
        structures.add(VillageStructure("pond1", "দিঘি", StructureType.POND, 1680f, 1020f, 270f, 210f, 0xFF0288D1))

        // Vegetable Gardens & Crop Fields
        structures.add(VillageStructure("g1", "সবজি বাগান", StructureType.GARDEN, 480f, 980f, 220f, 170f, 0xFF388E3C))
        structures.add(VillageStructure("f1", "ধানক্ষেত", StructureType.FIELD, 580f, 1820f, 260f, 175f, 0xFF689F38))
        structures.add(VillageStructure("f2", "ফসলের মাঠ", StructureType.FIELD, 1580f, 1840f, 250f, 170f, 0xFF7CB342))

        // Mango Trees
        val mangoPositions = listOf(
            320f to 360f, 740f to 370f, 1360f to 360f, 1820f to 390f,
            330f to 1240f, 760f to 1320f, 1420f to 1320f, 1860f to 1320f
        )
        mangoPositions.forEachIndexed { idx, (mx, my) ->
            structures.add(VillageStructure("mt_$idx", "আম গাছ", StructureType.MANGO_TREE, mx, my, 110f, 110f))
        }

        // Coconut Trees
        val coconutPositions = listOf(
            340f to 720f, 820f to 680f, 1380f to 680f, 1860f to 740f,
            1500f to 1020f, 1860f to 1040f, 820f to 1640f, 1360f to 1660f
        )
        coconutPositions.forEachIndexed { idx, (cx, cy) ->
            structures.add(VillageStructure("ct_$idx", "ডাব গাছ", StructureType.COCONUT_TREE, cx, cy, 100f, 100f))
        }
    }

    private fun buildVillageNpcs() {
        npcs.clear()
        npcs.add(
            VillageNpc(
                id = "npc_rahim",
                nameBn = "রহিম চাচা",
                shirtColorHex = 0xFFFF7043,
                waypoints = listOf(420f to 620f, 780f to 620f, 780f to 420f, 420f to 420f)
            )
        )
        npcs.add(
            VillageNpc(
                id = "npc_karim",
                nameBn = "করিম ভাই",
                shirtColorHex = 0xFF26C6DA,
                waypoints = listOf(840f to 1080f, 1360f to 1080f, 1360f to 820f, 840f to 820f)
            )
        )
        npcs.add(
            VillageNpc(
                id = "npc_mina",
                nameBn = "মিনা খালা",
                shirtColorHex = 0xFFEC407A,
                waypoints = listOf(380f to 1360f, 720f to 1360f, 720f to 1650f, 380f to 1650f)
            )
        )
        npcs.add(
            VillageNpc(
                id = "npc_morol",
                nameBn = "মোড়ল কাকা",
                shirtColorHex = 0xFFFFCA28,
                waypoints = listOf(960f to 560f, 1320f to 560f, 1680f to 620f, 1320f to 560f)
            )
        )
        npcs.add(
            VillageNpc(
                id = "npc_ratan",
                nameBn = "গোয়ালা রতন",
                shirtColorHex = 0xFF66BB6A,
                waypoints = listOf(900f to 1620f, 1280f to 1620f, 1280f to 1880f, 900f to 1880f)
            )
        )
        npcs.add(
            VillageNpc(
                id = "npc_bhulu",
                nameBn = "পাহারাদার ভুলু",
                shirtColorHex = 0xFFAB47BC,
                waypoints = listOf(1480f to 1380f, 1820f to 1380f, 1820f to 1680f, 1480f to 1680f)
            )
        )
    }

    private fun spawnWorldCollectibles(catalog: List<CollectibleItemConfig>) {
        collectibles.clear()
        val byId = catalog.associateBy { it.itemId }
        val mango = byId["mango"] ?: DefaultGameCatalog.defaultItems[0]
        val coconut = byId["coconut"] ?: DefaultGameCatalog.defaultItems[1]
        val eggBasket = byId["egg_basket"] ?: DefaultGameCatalog.defaultItems[2]
        val cropBasket = byId["crop_basket"] ?: DefaultGameCatalog.defaultItems[3]
        val fruitBasket = byId["fruit_basket"] ?: DefaultGameCatalog.defaultItems[4]
        val bag = byId["bag"] ?: DefaultGameCatalog.defaultItems[5]
        val phone = byId["fictional_phone"] ?: DefaultGameCatalog.defaultItems[6]
        val coinBag = byId["coin_bag"] ?: DefaultGameCatalog.defaultItems[7]
        val moneyBag = byId["money_bag"] ?: DefaultGameCatalog.defaultItems[8]
        val goldenMango = byId["golden_mango"] ?: DefaultGameCatalog.defaultItems[9]
        val cow = byId["cow"] ?: DefaultGameCatalog.defaultItems[10]
        val mysteryBox = byId["mystery_box"] ?: DefaultGameCatalog.defaultItems[11]
        val treasure = byId["treasure"] ?: DefaultGameCatalog.defaultItems[12]

        val placements = listOf(
            // Mangoes under mango trees & near starting area
            Triple("c_mango_1", mango, 980f to 1120f),
            Triple("c_mango_2", mango, 360f to 430f),
            Triple("c_mango_3", mango, 760f to 440f),
            Triple("c_mango_4", mango, 1380f to 430f),
            Triple("c_mango_5", mango, 1800f to 460f),
            Triple("c_mango_6", mango, 370f to 1300f),
            Triple("c_mango_7", mango, 790f to 1380f),
            // Coconuts under coconut trees
            Triple("c_coco_1", coconut, 1220f to 1140f),
            Triple("c_coco_2", coconut, 380f to 780f),
            Triple("c_coco_3", coconut, 850f to 740f),
            Triple("c_coco_4", coconut, 1410f to 740f),
            Triple("c_coco_5", coconut, 1520f to 1120f),
            Triple("c_coco_6", coconut, 840f to 1700f),
            // Baskets in gardens & yards
            Triple("c_egg_1", eggBasket, 540f to 1580f),
            Triple("c_egg_2", eggBasket, 1600f to 560f),
            Triple("c_crop_1", cropBasket, 600f to 1740f),
            Triple("c_crop_2", cropBasket, 1580f to 1760f),
            Triple("c_fruit_1", fruitBasket, 520f to 1060f),
            Triple("c_fruit_2", fruitBasket, 1080f to 960f),
            // Bags & Phones near houses & shops
            Triple("c_bag_1", bag, 880f to 1020f),
            Triple("c_bag_2", bag, 600f to 580f),
            Triple("c_phone_1", phone, 1360f to 1020f),
            Triple("c_phone_2", phone, 1660f to 1640f),
            // Coin bags & Money bags
            Triple("c_coinbag_1", coinBag, 1120f to 540f),
            Triple("c_coinbag_2", coinBag, 1440f to 1400f),
            Triple("c_money_1", moneyBag, 500f to 560f),
            Triple("c_money_2", moneyBag, 1660f to 540f),
            // Special Items: Golden Mango, Mystery Box, Treasure
            Triple("c_goldmango_1", goldenMango, 720f to 340f),
            Triple("c_goldmango_2", goldenMango, 1840f to 1260f),
            Triple("c_mystery_1", mysteryBox, 1100f to 820f),
            Triple("c_mystery_2", mysteryBox, 440f to 1180f),
            Triple("c_treasure_1", treasure, 520f to 380f),
            Triple("c_treasure_2", treasure, 1720f to 1460f),
            // Cows in pasture & yards (Cow collection mechanic!)
            Triple("c_cow_1", cow, 1020f to 1580f),
            Triple("c_cow_2", cow, 1200f to 1640f),
            Triple("c_cow_3", cow, 680f to 1180f)
        )

        for ((uid, cfg, pos) in placements) {
            if (cfg.active) {
                collectibles.add(
                    WorldCollectible(
                        uid = uid,
                        config = cfg,
                        spawnX = pos.first,
                        spawnY = pos.second
                    )
                )
            }
        }
    }

    /**
     * Finds the nearest available collectible within interaction range (145 world units).
     */
    fun findNearestInteractable(maxRadius: Float = 155f): WorldCollectible? {
        return collectibles
            .filter { it.isAvailable && !it.isFollowingPlayer }
            .minByOrNull { hypot(it.x - playerX, it.y - playerY) }
            ?.takeIf { hypot(it.x - playerX, it.y - playerY) <= maxRadius }
    }

    /**
     * Finds the closest available collectible anywhere on the map (helpful for guiding the player).
     */
    fun findClosestAvailableAnywhere(): WorldCollectible? {
        return collectibles
            .filter { it.isAvailable && !it.isFollowingPlayer }
            .minByOrNull { hypot(it.x - playerX, it.y - playerY) }
    }

    fun activateSneakySpecial(): Boolean {
        if (specialCharge < 1f || sneakyModeTimerSec > 0f) return false
        specialCharge = 0f
        sneakyModeTimerSec = 8f
        addPopup("🛡️ SNEAKY MODE! (৮ সে.)", playerX, playerY - 40f, 0xFF00E5FF)
        return true
    }

    /**
     * Executes collection ("চুরি করো" or "গরু নিয়ে পালাও").
     * Returns pair of (coinsAwarded, collectedConfig) if immediate collection happened,
     * or (-1L to config) if Cow Chase started!
     */
    fun attemptCollect(
        settings: GameSettings,
        onSoundCollect: (Int) -> Unit,
        onSoundCowEscape: () -> Unit,
        onSoundCombo: (Int) -> Unit
    ): Pair<Long, CollectibleItemConfig>? {
        if (totalSecondsRemaining <= 0f || surprisedTimerSec > 0f) return null

        var target = findNearestInteractable(155f)
        if (target == null) {
            // If player tapped STEAL slightly outside range, check within 240f and step closer
            val nearby = findNearestInteractable(240f)
            if (nearby != null) {
                val dx = nearby.x - playerX
                val dy = nearby.y - playerY
                val dist = hypot(dx, dy).coerceAtLeast(1f)
                playerX = (playerX + (dx / dist) * 95f).coerceIn(100f, worldWidth - 100f)
                playerY = (playerY + (dy / dist) * 95f).coerceIn(100f, worldHeight - 100f)
                target = findNearestInteractable(165f)
            }
        }
        val item = target ?: return null

        // Trigger NPC reaction if an NPC is within 340 units and player is not in Sneaky Mode
        if (sneakyModeTimerSec <= 0f) {
            npcs.forEach { npc ->
                val d = hypot(npc.x - playerX, npc.y - playerY)
                if (d < 340f) {
                    npc.alertProgress = (npc.alertProgress + 0.38f).coerceAtMost(1f)
                    npc.speechBubble = if (item.config.itemId == "cow") {
                        "আরে! গরু নিয়ে পালাচ্ছে কে?!"
                    } else {
                        "এই! ওদিকে কী করছো?"
                    }
                    npc.speechTimerSec = 2.5f
                }
            }
        }

        // Check if this is the Cow ("গরু নিয়ে পালাও") mechanic!
        if (item.config.itemId == "cow" || item.config.specialEffect == "COW_CHASE") {
            item.isFollowingPlayer = true
            item.followTimerSec = 1.35f
            playerAnimState = PlayerAnimState.COLLECTING
            collectAnimTimerSec = 0.45f
            onSoundCowEscape()
            addPopup("🐄 গরু নিয়ে পালাও! 💨", item.x, item.y - 35f, 0xFFFFC837)
            return -1L to item.config
        }

        val awarded = finalizeItemCollection(item, settings, onSoundCollect, onSoundCombo)
        return awarded to item.config
    }

    private fun finalizeItemCollection(
        item: WorldCollectible,
        settings: GameSettings,
        onSoundCollect: (Int) -> Unit,
        onSoundCombo: (Int) -> Unit
    ): Long {
        item.isAvailable = false
        item.isFollowingPlayer = false
        val spawnFactor = (100f / settings.itemSpawnRate.coerceIn(50L, 200L).toFloat())
        item.respawnTimerSec = (item.config.respawnTimeSeconds * spawnFactor).coerceIn(3f, 90f)

        // Update Combo
        val maxCombo = maxOf(settings.maxComboMultiplier.toInt(), 10)
        if (comboTimerSec > 0f) {
            currentCombo = (currentCombo + 1).coerceAtMost(maxCombo)
            if (currentCombo > bestCombo) bestCombo = currentCombo
            onSoundCombo(currentCombo)
        } else {
            currentCombo = 1
        }
        comboTimerSec = maxComboWindowSec

        // Calculate virtual coins earned
        var earned = item.config.coinValue * currentCombo
        if (nextItemDoubleCoins) {
            earned *= 2L
            nextItemDoubleCoins = false
            addPopup("🍀 2x LUCKY BONUS!", playerX, playerY - 68f, 0xFF00E676)
        }

        coinsEarned = (coinsEarned + earned).coerceAtMost(50000L)
        itemsCollected += 1
        specialCharge = (specialCharge + 0.22f).coerceAtMost(1f)

        // Track item counts and 30-second speed thief window
        val currentCount = itemCountsByType[item.config.nameBn] ?: 0
        itemCountsByType[item.config.nameBn] = currentCount + 1

        collectionTimestampsSec.add(elapsedRoundSec)
        collectionTimestampsSec.removeAll { elapsedRoundSec - it > 30f }
        if (collectionTimestampsSec.size >= 20) {
            reachedSpeedThiefInRound = true
        }

        // Apply special item effects
        when (item.config.specialEffect) {
            "SPEED_BOOST" -> {
                speedBoostTimerSec = 10f
                addPopup("🔥 Speed Boost (১০ সে.)!", item.x, item.y - 55f, 0xFF00E5FF)
            }
            "TIME_BONUS" -> {
                totalSecondsRemaining = (totalSecondsRemaining + 5f).coerceAtMost(600f)
                addPopup("🕒 +5 সেকেন্ড Time Bonus!", item.x, item.y - 55f, 0xFF00E676)
            }
            "SNEAKY_MODE" -> {
                sneakyModeTimerSec = 8f
                addPopup("🛡️ Sneaky Mode!", item.x, item.y - 55f, 0xFFB388FF)
            }
            "JACKPOT" -> {
                celebrateTimerSec = 0.7f
                addPopup("💎 মেগা গুপ্তধন!", item.x, item.y - 55f, 0xFFFFE066)
            }
        }

        collectAnimTimerSec = 0.35f
        playerAnimState = if (earned >= 150L) PlayerAnimState.CELEBRATING else PlayerAnimState.COLLECTING
        onSoundCollect(currentCombo)

        val comboSuffix = if (currentCombo > 1) " (x$currentCombo)" else ""
        addPopup("+$earned 🪙$comboSuffix", item.x, item.y - 25f, 0xFFFFC837)
        return earned
    }

    fun stepFrame(
        dtSec: Float,
        joystickX: Float,
        joystickY: Float,
        settings: GameSettings,
        onSoundFootstep: () -> Unit,
        onSoundCollect: (Int) -> Unit,
        onSoundCombo: (Int) -> Unit,
        onSoundAlert: () -> Unit,
        onSoundCaught: () -> Unit,
        onSoundTick: (Boolean) -> Unit,
        onRoundFinished: () -> Unit
    ) {
        if (totalSecondsRemaining <= 0f) return
        val dt = dtSec.coerceIn(0.001f, 0.08f)
        animTimeSec += dt
        elapsedRoundSec += dt

        // 1. Update Countdown Timer
        val prevWholeSec = totalSecondsRemaining.toInt()
        totalSecondsRemaining = (totalSecondsRemaining - dt).coerceAtLeast(0f)
        val newWholeSec = totalSecondsRemaining.toInt()
        if (newWholeSec < prevWholeSec && newWholeSec in 1..30) {
            if (newWholeSec <= 10 || newWholeSec % 5 == 0) {
                onSoundTick(newWholeSec <= 10)
            }
        }
        if (totalSecondsRemaining <= 0f) {
            playerAnimState = PlayerAnimState.CELEBRATING
            onRoundFinished()
            return
        }

        // 2. Update Buffs & Combo Timers
        if (comboTimerSec > 0f) {
            comboTimerSec = (comboTimerSec - dt).coerceAtLeast(0f)
            if (comboTimerSec <= 0f) {
                currentCombo = 1
            }
        }
        if (speedBoostTimerSec > 0f) speedBoostTimerSec = (speedBoostTimerSec - dt).coerceAtLeast(0f)
        if (sneakyModeTimerSec > 0f) sneakyModeTimerSec = (sneakyModeTimerSec - dt).coerceAtLeast(0f)
        if (npcAlertEventTimerSec > 0f) npcAlertEventTimerSec = (npcAlertEventTimerSec - dt).coerceAtLeast(0f)
        if (activeEventBannerTimerSec > 0f) {
            activeEventBannerTimerSec = (activeEventBannerTimerSec - dt).coerceAtLeast(0f)
            if (activeEventBannerTimerSec <= 0f) activeEventBanner = null
        }

        // 3. Trigger Random Events periodically
        nextRandomEventCountdownSec -= dt
        if (nextRandomEventCountdownSec <= 0f) {
            nextRandomEventCountdownSec = Random.nextFloat() * 10f + 18f
            triggerRandomEvent()
        }

        // 4. Update Player Movement
        if (surprisedTimerSec > 0f) {
            surprisedTimerSec = (surprisedTimerSec - dt).coerceAtLeast(0f)
            playerAnimState = PlayerAnimState.SURPRISED
        } else {
            val mag = hypot(joystickX, joystickY)
            val isMoving = mag > 0.08f
            val sprinting = isSprintPressed && sprintStamina > 0.08f && isMoving
            if (sprinting) {
                sprintStamina = (sprintStamina - dt * 0.42f).coerceAtLeast(0f)
            } else {
                sprintStamina = (sprintStamina + dt * 0.25f).coerceAtMost(1f)
            }

            if (isMoving) {
                val normX = joystickX / mag.coerceAtLeast(1f)
                val normY = joystickY / mag.coerceAtLeast(1f)
                playerFacingX = normX
                playerFacingY = normY

                var baseSpeed = 250f * (settings.playerSpeedMultiplier.coerceIn(50L, 200L) / 100f)
                if (sprinting) baseSpeed *= 1.48f
                if (speedBoostTimerSec > 0f) baseSpeed *= 1.42f

                playerX = (playerX + normX * baseSpeed * dt).coerceIn(90f, worldWidth - 90f)
                playerY = (playerY + normY * baseSpeed * dt).coerceIn(90f, worldHeight - 90f)

                footstepSpawnAccum += dt
                if (footstepSpawnAccum >= if (sprinting) 0.14f else 0.24f) {
                    footstepSpawnAccum = 0f
                    footsteps.add(FootstepParticle(playerX, playerY + 22f))
                    onSoundFootstep()
                }
            }

            if (collectAnimTimerSec > 0f) {
                collectAnimTimerSec = (collectAnimTimerSec - dt).coerceAtLeast(0f)
                playerAnimState = PlayerAnimState.COLLECTING
            } else if (celebrateTimerSec > 0f) {
                celebrateTimerSec = (celebrateTimerSec - dt).coerceAtLeast(0f)
                playerAnimState = PlayerAnimState.CELEBRATING
            } else if (isMoving) {
                playerAnimState = if (sprinting || speedBoostTimerSec > 0f) {
                    PlayerAnimState.RUN
                } else {
                    PlayerAnimState.SNEAK_WALK
                }
            } else {
                playerAnimState = PlayerAnimState.IDLE
            }
        }

        // 5. Update Collectible Respawns & Following Cows ("গরু নিয়ে পালাও")
        val iter = collectibles.iterator()
        while (iter.hasNext()) {
            val item = iter.next()
            if (item.isFollowingPlayer) {
                // Smoothly follow behind the player character!
                val targetX = playerX - playerFacingX * 55f
                val targetY = playerY - playerFacingY * 55f
                item.x += (targetX - item.x) * (dt * 7.5f).coerceAtMost(1f)
                item.y += (targetY - item.y) * (dt * 7.5f).coerceAtMost(1f)
                item.followTimerSec -= dt
                if (item.followTimerSec <= 0f) {
                    finalizeItemCollection(item, settings, onSoundCollect, onSoundCombo)
                    item.x = item.spawnX
                    item.y = item.spawnY
                }
            } else if (!item.isAvailable) {
                item.respawnTimerSec -= dt
                if (item.respawnTimerSec <= 0f) {
                    if (item.isBonusSpawn) {
                        iter.remove()
                    } else {
                        item.isAvailable = true
                        item.x = item.spawnX
                        item.y = item.spawnY
                    }
                }
            }
        }

        // 6. Update NPCs & Alert Detection System
        val detectionScale = (settings.npcDetectionRadius.coerceIn(50L, 200L) / 100f) *
            (if (npcAlertEventTimerSec > 0f) 1.25f else 1.0f) *
            when (settings.difficulty) {
                "EASY" -> 0.8f
                "HARD" -> 1.2f
                else -> 1.0f
            }
        val baseDetectRadius = 215f * detectionScale

        var maxAlert = 0f
        var warningText: String? = null

        for (npc in npcs) {
            if (npc.cooldownSec > 0f) {
                npc.cooldownSec = (npc.cooldownSec - dt).coerceAtLeast(0f)
            }
            if (npc.speechTimerSec > 0f) {
                npc.speechTimerSec = (npc.speechTimerSec - dt).coerceAtLeast(0f)
                if (npc.speechTimerSec <= 0f) npc.speechBubble = null
            }

            // Patrol along waypoints (or move toward player briefly when high alert)
            val wp = npc.waypoints[npc.currentWaypointIndex % npc.waypoints.size]
            val distToPlayer = hypot(playerX - npc.x, playerY - npc.y)
            val targetPt = if (npc.alertProgress > 0.45f && sneakyModeTimerSec <= 0f && npc.cooldownSec <= 0f) {
                playerX to playerY
            } else {
                wp
            }
            val dx = targetPt.first - npc.x
            val dy = targetPt.second - npc.y
            val dist = hypot(dx, dy)
            val npcSpeed = if (npc.alertProgress > 0.45f) 125f else 85f
            if (dist > 8f) {
                npc.facingX = dx / dist
                npc.facingY = dy / dist
                npc.x += npc.facingX * npcSpeed * dt
                npc.y += npc.facingY * npcSpeed * dt
            } else {
                npc.currentWaypointIndex = (npc.currentWaypointIndex + 1) % npc.waypoints.size
            }

            // Detection check
            if (sneakyModeTimerSec <= 0f && npc.cooldownSec <= 0f && distToPlayer <= baseDetectRadius) {
                val prevAlert = npc.alertProgress
                val rate = if (distToPlayer < baseDetectRadius * 0.55f) 0.65f else 0.36f
                npc.alertProgress = (npc.alertProgress + dt * rate).coerceAtMost(1f)
                if (prevAlert < 0.35f && npc.alertProgress >= 0.35f) {
                    npc.speechBubble = "এই! ওদিকে কী করছো?"
                    npc.speechTimerSec = 2.4f
                    onSoundAlert()
                }
                warningText = "ধরা পড়ে গেলে কিন্তু শেষ!"

                if (npc.alertProgress >= 1f) {
                    // Player CAUGHT! Funny non-violent penalty
                    npc.alertProgress = 0f
                    npc.cooldownSec = 5.5f
                    npc.speechBubble = "ধরেছি চোর! হা হা!"
                    npc.speechTimerSec = 2.8f
                    surprisedTimerSec = 0.85f
                    currentCombo = 1
                    comboTimerSec = 0f

                    val coinPenalty = minOf(coinsEarned, 20L)
                    coinsEarned = (coinsEarned - coinPenalty).coerceAtLeast(0L)
                    onSoundCaught()
                    addPopup(
                        if (coinPenalty > 0L) "🔴 ধরা পড়েছো! -$coinPenalty 🪙" else "🔴 ধরা পড়েছো! সাবধানে!",
                        playerX,
                        playerY - 45f,
                        0xFFFF3D57
                    )
                    // Push player slightly away so they aren't stuck
                    val angle = atan2(playerY - npc.y, playerX - npc.x)
                    playerX = (playerX + cos(angle) * 110f).coerceIn(100f, worldWidth - 100f)
                    playerY = (playerY + sin(angle) * 110f).coerceIn(100f, worldHeight - 100f)
                }
            } else {
                npc.alertProgress = (npc.alertProgress - dt * 0.32f).coerceAtLeast(0f)
            }

            if (npc.alertProgress > maxAlert) {
                maxAlert = npc.alertProgress
            }
        }

        highestNpcAlertProgress = maxAlert
        overallAlertState = when {
            surprisedTimerSec > 0f -> AlertState.CAUGHT
            maxAlert >= 0.3f -> AlertState.ALERT
            else -> AlertState.SAFE
        }
        activeWarningMessage = if (overallAlertState == AlertState.ALERT) warningText else null

        // 7. Update Floating Popups & Footsteps
        val popIter = floatingPopups.iterator()
        while (popIter.hasNext()) {
            val p = popIter.next()
            p.ageSec += dt
            if (p.ageSec >= p.durationSec) popIter.remove()
        }

        val footIter = footsteps.iterator()
        while (footIter.hasNext()) {
            val f = footIter.next()
            f.ageSec += dt
            if (f.ageSec >= f.maxAgeSec) footIter.remove()
        }
    }

    private fun triggerRandomEvent() {
        val event = RandomEventType.entries.random()
        activeEventBanner = event
        activeEventBannerTimerSec = 4.5f
        when (event) {
            RandomEventType.LUCKY_MOMENT -> {
                nextItemDoubleCoins = true
            }
            RandomEventType.NPC_ALERT -> {
                npcAlertEventTimerSec = 8f
                npcs.forEach {
                    it.speechBubble = "কে যেন গ্রামে ঢুকেছে!"
                    it.speechTimerSec = 2.5f
                }
            }
            RandomEventType.SPEED_MODE -> {
                speedBoostTimerSec = 8f
            }
            RandomEventType.TREASURE_APPEARED -> {
                val treasureCfg = DefaultGameCatalog.defaultItems.last()
                val tx = (playerX + Random.nextInt(-280, 280)).coerceIn(220f, worldWidth - 220f)
                val ty = (playerY + Random.nextInt(-280, 280)).coerceIn(220f, worldHeight - 220f)
                collectibles.add(
                    WorldCollectible(
                        uid = "bonus_treasure_${System.currentTimeMillis()}",
                        config = treasureCfg,
                        spawnX = tx,
                        spawnY = ty,
                        isBonusSpawn = true
                    )
                )
            }
        }
    }

    fun mostCollectedItemName(): String {
        return itemCountsByType.maxByOrNull { it.value }?.key ?: "আম"
    }

    private fun addPopup(text: String, x: Float, y: Float, colorHex: Long) {
        floatingPopups.add(
            FloatingPopup(
                id = popupIdCounter++,
                text = text,
                x = x,
                y = y,
                colorHex = colorHex
            )
        )
    }
}
