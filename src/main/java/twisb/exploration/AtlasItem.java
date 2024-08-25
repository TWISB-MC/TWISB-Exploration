package twisb.exploration;

//import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.*;
import net.minecraft.item.map.MapState;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.apache.commons.lang3.math.Fraction;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Predicate;

public class AtlasItem extends NetworkSyncedItem {

    // Closest map finding doesn't necessarily work across dimensions

    private static final Logger LOGGER = LoggerFactory.getLogger("twisb-exploration");

    public AtlasItem(Item.Settings settings) {
        super(settings);
    }

    private static final int ITEM_BAR_COLOR = MathHelper.packRgb(0.4F, 0.4F, 1.0F);

    @Nullable
    public Packet<?> createSyncPacket(ItemStack atlas, World world, PlayerEntity player) {
        MapIdComponent mapIdComponent = (MapIdComponent) atlas.get(DataComponentTypes.MAP_ID);
        MapState mapState = FilledMapItem.getMapState(mapIdComponent, world);
        return mapState != null ? mapState.getPlayerMarkerPacket(mapIdComponent, player) : null;
    }

    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient) {
            if (entity instanceof PlayerEntity playerEntity) {
                ItemStack mapItemStack = updateRelevantMap(stack, world, playerEntity);
                if (mapItemStack == null) {
                    stack.set(DataComponentTypes.MAP_ID, null);

                } else if (mapItemStack.isOf(Items.FILLED_MAP)){
                    FilledMapItem mapItem = (FilledMapItem) mapItemStack.getItem();
                    // Tick the relevant map
                    mapItem.inventoryTick(mapItemStack, world, entity, slot, selected);
                    // Set own MAP_ID data component
                    stack.set(DataComponentTypes.MAP_ID, (MapIdComponent) mapItemStack.get(DataComponentTypes.MAP_ID));
                } else {
                    LOGGER.info("Illegal item in atlas");
                }
            }
        }
    }
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack atlasItem = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.pass(atlasItem);
        } else {
            ItemStack emptyMaps = findEmptyMapItem(user);
            BundleContentsComponent bundleContentsComponent = (BundleContentsComponent) atlasItem.get(DataComponentTypes.BUNDLE_CONTENTS);
            MapIdComponent mapId = atlasItem.get(DataComponentTypes.MAP_ID);
            MapState mapState = mapId == null ? null : FilledMapItem.getMapState(mapId, world);
            if (emptyMaps == null) {
                // If no empty maps in inventory fail
                return TypedActionResult.fail(atlasItem);
            } else if(bundleContentsComponent == null) {
                // If item doesn't have bundle component fail
                return TypedActionResult.fail(atlasItem);
            } else if(mapState != null && isMapOnPlayer(mapState, user, world)) {
                // If current location already has map fail
                return TypedActionResult.fail(atlasItem);
            } else if (bundleContentsComponent.getOccupancy().equals(Fraction.ONE)) {
                // If atlas full fail
                return TypedActionResult.fail(atlasItem);
            } else {
                BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(bundleContentsComponent);
                emptyMaps.decrementUnlessCreative(1, user);
                user.incrementStat(Stats.USED.getOrCreateStat(emptyMaps.getItem()));
                user.getWorld().playSoundFromEntity((PlayerEntity)null, user, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, user.getSoundCategory(), 1.0F, 1.0F);
                ItemStack filledMap = FilledMapItem.createMap(world, user.getBlockX(), user.getBlockZ(), (byte)0, true, false);
                builder.add(filledMap);
                atlasItem.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
                if (emptyMaps.isEmpty()) {
                    return TypedActionResult.pass(atlasItem);
                } else {
                    return TypedActionResult.pass(atlasItem);
                }
            }
        }
    }
    public static final Predicate<ItemStack> IS_EMPTY_MAP = (stack) -> stack.isOf(Items.MAP);
    private static ItemStack findEmptyMapItem(PlayerEntity player) {
        Inventory inv = player.getInventory();
        for(int i = 0; i < inv.size(); ++i) {
            ItemStack stack = inv.getStack(i);
            if (IS_EMPTY_MAP.test(stack)) {
                return stack;
            }
        }
        return null;
    }

    private static ItemStack updateRelevantMap(ItemStack atlas, World world, PlayerEntity player) {
        BundleContentsComponent bundleContentsComponent = (BundleContentsComponent)atlas.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundleContentsComponent != null && !bundleContentsComponent.isEmpty()) {
            List<ItemStack> content = (List<ItemStack>) bundleContentsComponent.iterate();
            ItemStack stack;
            MapState mapState;
            double minDist = Double.MAX_VALUE;
            ItemStack closestMap = content.get(0);
            for (int i = 0; i < content.size(); i++) {
                stack = content.get(i);
                if (stack.isOf(Items.FILLED_MAP)) {
                    mapState = FilledMapItem.getMapState(stack, world);
                    if (mapState == null) continue;
                    double thisDist = mapDistanceToPlayer(mapState, player, world);
                    if (thisDist >= 0 && thisDist < minDist) { // check > 0 to ensure same dimension
                        minDist = thisDist;
                        closestMap = stack;
                    }
                }
            }
            return closestMap;
        } else {
            return null;
        }
    }

    private static ItemStack updateRelevantMapOld(ItemStack atlas, World world, PlayerEntity player) {
        BundleContentsComponent bundleContentsComponent = (BundleContentsComponent)atlas.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundleContentsComponent != null && !bundleContentsComponent.isEmpty()) {
            List<ItemStack> content = (List<ItemStack>) bundleContentsComponent.iterate();
            ItemStack stack;
            MapState mapState;
            for (int i = 0; i < content.size(); i++) {
                stack = content.get(i);
                if (stack.isOf(Items.FILLED_MAP)) {
                    mapState = FilledMapItem.getMapState(stack, world);
                    if (mapState == null) continue;
                    if (isMapOnPlayer(mapState, player, world)) {
                        return stack;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isMapOnPlayer(MapState mapState, PlayerEntity player, World world) {
        // Map properties
        int mapX = mapState.centerX;
        int mapZ = mapState.centerZ;
        RegistryKey<World> dim = mapState.dimension;
        byte scale = mapState.scale;
        // Player location
        int scaleFactor = 128 * (1 << scale);
        int thisX = MathHelper.floor((player.getX() + 64.0) / (double)scaleFactor);
        int thisZ = MathHelper.floor((player.getZ() + 64.0) / (double)scaleFactor);
        thisX = thisX * scaleFactor + scaleFactor / 2 - 64;
        thisZ = thisZ * scaleFactor + scaleFactor / 2 - 64;
        RegistryKey<World> thisDim = world.getRegistryKey();
        return mapX == thisX && mapZ == thisZ && dim.equals(thisDim);
    }

    private static double mapDistanceToPlayer(MapState mapState, PlayerEntity player, World world) {
        // Map properties
        int mapX = mapState.centerX;
        int mapZ = mapState.centerZ;
        RegistryKey<World> dim = mapState.dimension;
        // Player location
        int thisX = (int) player.getX();
        int thisZ = (int) player.getZ();
        RegistryKey<World> thisDim = world.getRegistryKey();
        if (!dim.equals(thisDim)) {
            return -1; // Signifies wrong dimension
        } else {
            return Math.sqrt(Math.pow(mapX - thisX, 2) + Math.pow(mapZ - thisZ, 2));
        }
    }

    // ====== Modified bundle code below ======

    // Code run when you have atlas selected and click other stack
    public boolean onStackClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player) {
        if (clickType != ClickType.RIGHT) {
            return false;
        } else {
            BundleContentsComponent bundleContentsComponent = (BundleContentsComponent)stack.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (bundleContentsComponent == null) {
                return false;
            } else {
                ItemStack otherStack = slot.getStack();
                BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(bundleContentsComponent);
                if (otherStack.isEmpty()) {
                    this.playRemoveOneSound(player);
                    ItemStack itemStack2 = builder.removeFirst();
                    if (itemStack2 != null) {
                        ItemStack itemStack3 = slot.insertStack(itemStack2);
                        builder.add(itemStack3);
                    }
                    // When atlas is selected, inventory_tick does not execute so we update the map manually
                    // But only if the atlas is now empty
                    if (builder.getOccupancy().compareTo(Fraction.ZERO) == 0) {
                        stack.set(DataComponentTypes.MAP_ID, null);
                    }
                } else if (otherStack.isOf(Items.FILLED_MAP) && mapCompatible(stack, otherStack, player.getWorld(), player)) { // Changed to only allow maps, no need to check if can be nested
                    // When atlas is selected, inventory_tick does not execute so we update the map manually
                    // But only if the state was previously null
                    if (stack.get(DataComponentTypes.MAP_ID) == null) {
                        stack.set(DataComponentTypes.MAP_ID, (MapIdComponent) otherStack.get(DataComponentTypes.MAP_ID));
                    }
                    int i = builder.add(slot, player);
                    if (i > 0) {
                        this.playInsertSound(player);
                    }
                }
                stack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
                return true;
            }
        }
    }

    // Code run when you have another stack selected and right click on atlas/bundle
    public boolean onClicked(ItemStack atlas, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        BundleContentsComponent bundleContentsComponent = (BundleContentsComponent)atlas.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (clickType != ClickType.RIGHT) {
            // Only right click considered
            return false;
        } else if (bundleContentsComponent == null) {
            return false;
        }
        BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(bundleContentsComponent);
        if (otherStack.isEmpty()) {
            ItemStack itemStack = builder.removeFirst();
            if (itemStack != null) {
                this.playRemoveOneSound(player);
                cursorStackReference.set(itemStack);
            }
            atlas.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
            return true;
        } else if (otherStack.isOf(Items.FILLED_MAP)) {
            if (mapCompatible(atlas, otherStack, player.getWorld(), player)) {
                int i = builder.add(otherStack);
                if (i > 0) {
                    this.playInsertSound(player);
                }
                atlas.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
                return true;
            } else {
                return true;
            }
        } else if (!otherStack.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
    public static boolean mapCompatible(ItemStack atlas, ItemStack filledMap, World world, PlayerEntity player) {
//        LOGGER.info("mapCompatible called by " + (world.isClient() ? "client" : "server"));
        // Map can only be inserted in atlas if same scale and dimension
        //player.sendMessage(Text.translatable("block.minecraft.bed.occupied"), true);
        MapIdComponent atlasMapId = atlas.get(DataComponentTypes.MAP_ID);
        MapState atlasMapState = atlasMapId == null ? null : FilledMapItem.getMapState(atlasMapId, world);
        if (atlasMapState == null) return true; // If no map in atlas then always true
        MapIdComponent filledMapId = filledMap.get(DataComponentTypes.MAP_ID);
        MapState filledMapState = filledMapId == null ? null : FilledMapItem.getMapState(filledMapId, world);
        if (atlasMapState.scale != filledMapState.scale) {
            player.sendMessage(Text.translatable("item.twisb-exploration.atlas.wrong_scale"), false);
            return false;
        } else if (!atlasMapState.dimension.equals(filledMapState.dimension)) {
            player.sendMessage(Text.translatable("item.twisb-exploration.atlas.wrong_dimension"), false);
            return false;
        } else {
//            LOGGER.info(String.valueOf(atlasMapState.dimension) + String.valueOf(filledMapState.dimension));
            return true;
        }
    }

    // Tooltip
//    public Optional<TooltipData> getTooltipData(ItemStack stack) {
//        // Shows bundle content visually
//        return !stack.contains(DataComponentTypes.HIDE_TOOLTIP) && !stack.contains(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP) ? Optional.ofNullable((BundleContentsComponent)stack.get(DataComponentTypes.BUNDLE_CONTENTS)).map(BundleTooltipData::new) : Optional.empty();
//    }
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        // Writes occupancy
        BundleContentsComponent bundleContentsComponent = (BundleContentsComponent)stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundleContentsComponent != null) {
            int i = MathHelper.multiplyFraction(bundleContentsComponent.getOccupancy(), 64);
            tooltip.add(Text.translatable("item.twisb-exploration.atlas.fullness", new Object[]{i, 64}).formatted(Formatting.GRAY));
            MapIdComponent mapIdComponent = (MapIdComponent)stack.get(DataComponentTypes.MAP_ID);
            MapState mapState = mapIdComponent != null ? context.getMapState(mapIdComponent) : null;
            if (mapState != null) {
                tooltip.add(Text.translatable("item.twisb-exploration.atlas.scale", new Object[]{1 << mapState.scale}).formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("item.twisb-exploration.atlas.level", new Object[]{mapState.scale, 4}).formatted(Formatting.GRAY));
            }
        }
    }

    // Fullness bar things
    public boolean isItemBarVisible(ItemStack stack) {
        BundleContentsComponent bundleContentsComponent = (BundleContentsComponent)stack.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT);
        return bundleContentsComponent.getOccupancy().compareTo(Fraction.ZERO) > 0;
    }
    public int getItemBarStep(ItemStack stack) {
        BundleContentsComponent bundleContentsComponent = (BundleContentsComponent)stack.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT);
        return Math.min(1 + MathHelper.multiplyFraction(bundleContentsComponent.getOccupancy(), 12), 13);
    }
    public int getItemBarColor(ItemStack stack) {
        return ITEM_BAR_COLOR;
    }

    // Sound things
    private void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.getWorld().getRandom().nextFloat() * 0.4F);
    }
    private void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.ITEM_BUNDLE_INSERT, 0.8F, 0.8F + entity.getWorld().getRandom().nextFloat() * 0.4F);
    }
}
