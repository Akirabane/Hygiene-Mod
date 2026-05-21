package com.hygienemod.items;

import com.hygienemod.HygieneManager;
import com.hygienemod.HygieneManager.BathType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

public class VikingSoapItem extends Item {

    private static final ResourceLocation WASHING_TUB_ID = new ResourceLocation("conquest", "wooden_washing_tub");

    public VikingSoapItem() {
        super(new Properties().stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            BathType context = null;
            if (isOnFilledTub(level, player)) {
                context = BathType.TUB;
            } else if (isInQualifyingWater(level, player)) {
                context = BathType.RIVER;
            }

            if (context != null) {
                HygieneManager.applySoapBonus(player.getUUID(), context);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                if (context == BathType.TUB) {
                    player.sendSystemMessage(Component.literal("§8§oVous vous frottez avec le savon viking dans le baquet. Une odeur de cendre et de suif s'échappe."));
                } else {
                    player.sendSystemMessage(Component.literal("§8§oVous vous frottez avec le savon viking dans l'eau froide. L'odeur de suif disparaît légèrement."));
                }
                return InteractionResultHolder.consume(stack);
            } else {
                player.sendSystemMessage(Component.literal("§8§oVous devez être dans l'eau ou sur un baquet rempli pour utiliser le savon."));
                return InteractionResultHolder.fail(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    private boolean isOnFilledTub(Level level, Player player) {
        BlockPos below = player.blockPosition().below();
        BlockState state = level.getBlockState(below);
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (!WASHING_TUB_ID.equals(key)) return false;
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("level")) {
                return Integer.parseInt(state.getValue(prop).toString()) > 0;
            }
        }
        return false;
    }

    private boolean isInQualifyingWater(Level level, Player player) {
        BlockPos feet = player.blockPosition();
        if (!isWater(level, feet) || !isWater(level, feet.above())) return false;

        int x = feet.getX(), y = feet.getY(), z = feet.getZ();
        int[][] anchors = {{0, 0}, {-1, 0}, {0, -1}, {-1, -1}};
        for (int[] a : anchors) {
            if (is2x2x2Water(level, x + a[0], y, z + a[1])) return true;
        }
        return false;
    }

    private boolean is2x2x2Water(Level level, int x, int y, int z) {
        for (int dx = 0; dx <= 1; dx++)
            for (int dy = 0; dy <= 1; dy++)
                for (int dz = 0; dz <= 1; dz++)
                    if (!isWater(level, new BlockPos(x + dx, y + dy, z + dz))) return false;
        return true;
    }

    private boolean isWater(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(Fluids.WATER);
    }
}
