package com.godjiismp.weapons;

import com.godjiismp.weapons.*;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class LegendaryItems {
 private LegendaryItems(){}
 public static final Item[] ITEMS=new Item[15];
 private static final String[] VISIBLE={"skeleton_king_blade","stormcaller_spear","earthbreaker_hammer","crimson_moon_scythe","frostbite_bow","emberfang_axe"};
 private static final String[] ICONS={"skill_icon_attack","skill_icon_defense","skill_icon_mining","skill_icon_farming","skill_icon_fishing","skill_icon_exploration","skill_point","skill_locked","skill_unlocked"};
 public static Item get(int i){return ITEMS[i];}
 public static void init(){
  WeaponsConfig.load();
  for(int i=0;i<VISIBLE.length;i++){
   Item item=i==4?new FrostbiteBow(new Item.Properties().durability(WeaponsConfig.durability[i]).attributes(attributes(i))):new LegendaryWeapon(i,new Item.Properties().durability(WeaponsConfig.durability[i]).attributes(attributes(i)));
   ITEMS[i]=register(VISIBLE[i],item);
  }
  for(int i=0;i<ICONS.length;i++)ITEMS[6+i]=register(ICONS[i],new Item(new Item.Properties()));
  Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,ResourceLocation.fromNamespaceAndPath("godjiismp","legendary_weapons"),FabricItemGroup.builder()
   .title(Component.translatable("itemGroup.godjiismp.legendary_weapons"))
   .icon(()->new ItemStack(ITEMS[0]))
   .displayItems((parameters,output)->{for(int i=0;i<VISIBLE.length;i++)output.accept(ITEMS[i]);})
   .build());
 }
 private static ItemAttributeModifiers attributes(int i){
  ResourceLocation damage=ResourceLocation.fromNamespaceAndPath("godjiismp_newdays",VISIBLE[i]+"_damage"),speed=ResourceLocation.fromNamespaceAndPath("godjiismp_newdays",VISIBLE[i]+"_speed");
  return ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,new AttributeModifier(damage,WeaponsConfig.damage[i]-1.0,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).add(Attributes.ATTACK_SPEED,new AttributeModifier(speed,WeaponsConfig.speed[i]-4.0,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build();
 }
 private static Item register(String name,Item item){ResourceLocation id=ResourceLocation.fromNamespaceAndPath("godjiismp",name);return Registry.register(BuiltInRegistries.ITEM,id,item);}
}