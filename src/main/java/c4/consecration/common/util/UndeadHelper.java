/*
 * Copyright (c) 2018 <C4>
 *
 * This Java class is distributed as a part of Consecration.
 * Consecration is open source and licensed under the GNU General Public License v3.
 * A copy of the license can be found here: https://www.gnu.org/licenses/gpl.txt
 */

package c4.consecration.common.util;

import c4.consecration.Consecration;
import c4.consecration.common.capabilities.CapabilityUndying;
import c4.consecration.common.capabilities.IUndying;
import c4.consecration.common.config.ConfigHandler;
import c4.consecration.integrations.ModuleCompatibility;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.Level;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;

public class UndeadHelper {

    private static final Field ARMOR_MATERIAL_NAME = ReflectionHelper.findField(ItemArmor.ArmorMaterial.class,
            "name", "field_179243_f", "f");

    public static boolean isUndead(EntityLivingBase entityLivingBase) {
        return (ConfigHandler.undying.defaultUndead && entityLivingBase.isEntityUndead())
                || UndeadRegistry.getUndeadList().contains(EntityList.getKey(entityLivingBase));
    }

    public static boolean isSmote(@Nonnull EntityLivingBase entitylivingbase, @Nonnull IUndying undying) {
        return undying.isSmote() || hasHolyPotion(entitylivingbase);
    }

    public static boolean doSmite(EntityLivingBase target, DamageSource source) {

        //Check fire damage/burning
        if (!target.isImmuneToFire() && (source.isFireDamage() || target.isBurning())) {
            return true;
        }

        //Check handheld itemstack
        if (source.getImmediateSource() instanceof EntityLivingBase) {

            ItemStack stack = ((EntityLivingBase) source.getImmediateSource()).getHeldItemMainhand();

            //Check weapon/tool materials
            if (stack.getItem() instanceof ItemTool && isHolyMaterial(((ItemTool) stack.getItem()).getToolMaterialName())) {
                return true;
            } else if (stack.getItem() instanceof ItemSword && isHolyMaterial(((ItemSword) stack.getItem()).getToolMaterialName())) {
                return true;
            } else {
                for (ItemStack stack1 : UndeadRegistry.getHolyWeapons()) {
                    if (ItemStack.areItemsEqualIgnoreDurability(stack1, stack)) {
                        return true;
                    }
                }
            }

            //Check enchantments
            for (Enchantment ench : EnchantmentHelper.getEnchantments(stack).keySet()) {
                if (UndeadRegistry.getHolyEnchantments().contains(ench)) {
                    return true;
                }
            }
        }

        //Check holy damage type/entity
        if (isHolyDamage(source.damageType) || isHolyEntity(source.getImmediateSource())) {
            return true;
        }

        //Process compatibility checks
        for (ModuleCompatibility module : ModuleCompatibility.getLoadedMods().values()) {

            if (module.process(target, source)) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasHolyPotion(EntityLivingBase entityIn) {
        for (Potion potion : entityIn.getActivePotionMap().keySet()) {
            if (UndeadRegistry.getHolyPotions().contains(potion)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHolyEntity(Entity entity) {
        if (entity != null) {
            ResourceLocation rl = EntityList.getKey(entity);
            return rl != null && UndeadRegistry.getHolyEntities().contains(rl);
        }
        return false;
    }

    public static boolean isHolyArmor(ItemStack armor) {
        if (armor.getItem() instanceof ItemArmor) {
            ItemArmor armorItem = (ItemArmor)armor.getItem();
            boolean isHolyArmor = false;
            try {
                isHolyArmor = isHolyMaterial((String)ARMOR_MATERIAL_NAME.get(armorItem.getArmorMaterial()));
            } catch (IllegalAccessException e) {
                Consecration.logger.log(Level.ERROR, "Error retrieving name for armor material "
                        + armorItem.getArmorMaterial());
            }
            return isHolyArmor;
        }
        return false;
    }

    public static boolean isHolyDamage(String name) {
        return UndeadRegistry.getHolyDamage().contains(name);
    }

    public static boolean isHolyMaterial(String mat) {

        mat = mat.toLowerCase();
        for (String name : UndeadRegistry.getHolyMaterials()) {
            String pattern = "((.*[^a-z])|\\b)" + name + "((.*[^a-z])|\\b)(.*|\\b)";
            if (mat.matches(pattern)) {
                return true;
            }
        }
        return false;
    }
}