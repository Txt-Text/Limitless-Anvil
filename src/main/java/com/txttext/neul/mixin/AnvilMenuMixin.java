package com.txttext.neul.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Map;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    public AnvilMenuMixin(@Nullable MenuType<?> menuType, int i, Inventory inventory, ContainerLevelAccess containerLevelAccess) {
        super(menuType, i, inventory, containerLevelAccess);
    }//这个构造器不会工作

    int nowLevel;

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

    //、111
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

    /*修复高等级合并附魔会被降级*/
//    @ModifyVariable(
//            method = "createResult",
//            at = @At(
//                    value = "STORE",
//                    ordinal = 13 // ← 要精确找到 `r = q == r ? r + 1 : Math.max(r, q);`
//            ),
//            index = 0, // 实际要根据r在局部变量表的 index来定
//            name = "r"
//    )
//    private int modifyRValue(int r) {
//        // 你可以在这里随意改 r
//        if (r > 5) return 5;
//        return r;
//    }
//    @Redirect(
//            method = "createResult",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I",
//                    ordinal = 0
//            )
//    )
//    private int redirectMaxLevel(Enchantment enchantment) {
//        //重复计算，相当于拿到itemStack2, itemStack3，q，r
//        ItemStack itemStack = this.inputSlots.getItem(0);
//        ItemStack itemStack2 = itemStack.copy();
//        ItemStack itemStack3 = this.inputSlots.getItem(1);
//        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(itemStack2);
//        Map<Enchantment, Integer> map2 = EnchantmentHelper.getEnchantments(itemStack3);
//        //int original = enchantment.getMaxLevel();
//        int q = map.getOrDefault(enchantment, 0);
//        int r = map2.getOrDefault(enchantment, 0);
//        r = (q == r) ? r + 1 : Math.max(q, r);
//        nowLevel = r;//将当前等级存入全局变量
//
//        int originalMaxLevel = enchantment.getMaxLevel();//原始等级上限
//        if (r > originalMaxLevel && q > enchantment.getMaxLevel()) {
//            return 255;
//        }//永远大于最大可用附魔等级，使 r > enchantment.getMaxLevel() 判断失效，从而使高等级附魔可正常合并
//        else{
//            return originalMaxLevel;
//        }
//    }

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
}

