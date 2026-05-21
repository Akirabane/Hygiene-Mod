package com.hygienemod.items;

import com.hygienemod.HygieneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

public class VikingSoapItem extends Item {

    public VikingSoapItem() {
        super(new Properties().stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            if (isInQualifyingWater(level, player)) {
                HygieneManager.applySoapBonus(player.getUUID());
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.sendSystemMessage(Component.literal("§8§oVous vous lavez avec le savon viking... L'odeur de cendre et de suif disparaît."));
                return InteractionResultHolder.consume(stack);
            } else {
                player.sendSystemMessage(Component.literal("§8§oVous devez être immergé dans une étendue d'eau suffisante pour utiliser le savon."));
                return InteractionResultHolder.fail(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
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
