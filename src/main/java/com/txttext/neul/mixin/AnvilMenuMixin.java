package com.txttext.neul.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;
import java.util.Map;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    public AnvilMenuMixin(@Nullable MenuType<?> menuType, int i, Inventory inventory, ContainerLevelAccess containerLevelAccess) {
        super(menuType, i, inventory, containerLevelAccess);
    }//这个构造器不会工作

    /*取消过于昂贵*/
    @ModifyExpressionValue(
            method = "createResult",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/player/Abilities;instabuild:Z"
            )
    )
    private boolean cancelTooExpensive(boolean original){
        return true;//让this.player.getAbilities().instabuild 永远返回true，使游戏认为玩家处于创造模式
    }

    /*修复高等级附魔会被降级*/
    //原版的逻辑是：如果等级超过了正常最大等级，就会降级到最大等级
    //这样导致的问题是：如果我拿到一个超过最大等级的高等级附魔（比如游玩模组时），一旦在铁砧上做什么立马会被降级
    @Redirect(
            method = "createResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I",
                    ordinal = 1
            )
    )
    private int redirectMaxLevel(Enchantment enchantment) {
        //重复计算，相当于拿到itemStack2, itemStack3，q，r
        ItemStack itemStack = this.inputSlots.getItem(0);
        ItemStack itemStack2 = itemStack.copy();
        ItemStack itemStack3 = this.inputSlots.getItem(1);
        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(itemStack2);
        Map<Enchantment, Integer> map2 = EnchantmentHelper.getEnchantments(itemStack3);
        //int original = enchantment.getMaxLevel();
        int q = map.getOrDefault(enchantment, 0);
        int r = map2.getOrDefault(enchantment, 0);
        r = (q == r) ? r + 1 : Math.max(q, r);
        //End重复计算部分

        int originalMaxLevel = enchantment.getMaxLevel();//原始等级上限
        //r是将要变成的等级，原版逻辑在检查这个等级的合理性

        //超过最大等级时，如果当前武器等级本身就大于最大等级，降级为当前等级（相当于不做变化），否则正常降级
        if (r > originalMaxLevel && q > enchantment.getMaxLevel()) {
            return r - 1;
        }
        else{
            return originalMaxLevel;
        }
    }
    /*弃用方案：*/
    //花费永远不大于39
//    @Redirect(
//            method = "createResult",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/world/inventory/DataSlot;set(I)V",
//                    ordinal = 5
//            )
//    )
//    private void preventTooExpensiveSet(DataSlot costSlot, int cost) {
//        costSlot.set(Math.min(cost, 39));//花费永不大于39
//    }

    //不昂贵的另一个办法，注入第一个判断值，返回一个小于40的假值骗过判断
//    @ModifyExpressionValue(
//            method = "createResult",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/world/inventory/DataSlot;get()I"
//                    //不使用 ordinal，拦第一处 DataSlot.get() 就够了
//            )
//    )
//    private int modifyAnvilCostCheck(int originalCost) {
//        //让 cost 永远小于 40，避免触发 "Too Expensive"
//        return Math.min(originalCost, 39);
//    }
}

