package com.godjiismp.client.ui;

import com.godjiismp.skills.*;
import com.godjiismp.weapons.LegendaryItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class SkillsScreen extends Screen {
 private static final ResourceLocation BG=ResourceLocation.fromNamespaceAndPath("godjiismp","textures/gui/skill_window.png"),NODES=ResourceLocation.fromNamespaceAndPath("godjiismp","textures/gui/skill_nodes.png"),WIDGETS=ResourceLocation.fromNamespaceAndPath("godjiismp","textures/gui/skill_widgets.png");
 private static final String[] LINES={"attack","defense","mining","farming","fishing","exploration"};private static final String[] LIGHT={"#FF7A66","#8CBCFF","#DCECF8","#A2EC7E","#8BE6FF","#FFE27A"};private static final int[][] CENTERS={{70,82},{108,82},{146,82},{184,82},{222,82},{222,122},{184,122},{146,122},{108,122},{70,122}};
 private static SkillPayload data;private int left,top,line=0,selected=1;
 public SkillsScreen(){super(Component.literal("Skills"));}
 public static void receive(SkillPayload payload){data=payload;Minecraft mc=Minecraft.getInstance();if(mc.screen instanceof SkillsScreen s)s.clampSelection();}
 public static void open(){Minecraft.getInstance().setScreen(new SkillsScreen());}
 private void clampSelection(){if(data!=null)selected=Math.max(1,Math.min(10,data.levels()[line]+1));}
 @Override protected void init(){left=(width-256)/2;top=(height-200)/2;if(data!=null)clampSelection();}
 @Override public void render(GuiGraphics g,int mouseX,int mouseY,float delta){
  // Avoid Screen.renderBackground: it applies a strong world blur.
  g.fill(0,0,width,height,0x55000000);
  left=(width-256)/2;top=(height-200)/2;g.blit(BG,left,top,0,0,256,200,256,256);drawString(g,"SKILLS",left+14,top+11,0xFFF6CF6A,true);
  if(data==null){drawString(g,"Loading...",left+70,top+95,0xFFFFFFFF,false);super.render(g,mouseX,mouseY,delta);return;}
  g.blit(ResourceLocation.fromNamespaceAndPath("godjiismp","textures/item/skill_point.png"),left+190,top+6,0,0,14,14,16,16);String points="Points: "+data.points();g.drawString(font,points,left+244-font.width(points),top+11,0xFFF6CF6A,true);
  for(int i=0;i<6;i++){int x=left+7,y=top+28+i*28;int state=i==line?2:(mouseX>=x&&mouseX<x+32&&mouseY>=y&&mouseY<y+26?1:0);blit(g,WIDGETS,x,y,state*32,0,32,26);g.renderItem(new ItemStack(LegendaryItems.get(6+i)),x+8,y+5);}
  String upper=LINES[line].toUpperCase(java.util.Locale.ROOT);int color=rgb(LIGHT[line]);drawString(g,upper,left+52,top+33,color,true);String lv="Level "+data.levels()[line]+"/10";g.drawString(font,lv,left+240-font.width(lv),top+33,0xFFE4DDF5,true);
  blit(g,WIDGETS,left+52,top+46,0,52,180,8);int fill=Math.round(180f*data.levels()[line]/10f);if(fill>0)blit(g,WIDGETS,left+52,top+46,0,62+line*10,fill,8);
  for(int i=0;i<Math.max(0,data.levels()[line]-1);i++)line(g,left+CENTERS[i][0],top+CENTERS[i][1],left+CENTERS[i+1][0],top+CENTERS[i+1][1],color);
  int available=data.levels()[line]+1;for(int i=0;i<10;i++){int cx=CENTERS[i][0],cy=CENTERS[i][1],state=i<data.levels()[line]?2:(i==available-1?1:0),u=(state+(i==9?3:0))*24,v=line*24;int x=left+cx-12,y=top+cy-12;blit(g,NODES,x,y,u,v,24,24);if((mouseX>=x&&mouseX<x+24&&mouseY>=y&&mouseY<y+24)||selected==i+1)blit(g,WIDGETS,left+cx-14,top+cy-14,200,0,28,28);String n=Integer.toString(i+1);int numColor=state==0?0xFF6A6585:0xFFFFFFFF;g.drawString(font,n,left+cx-font.width(n)/2,top+cy-8,numColor,true);}
  int index=selected-1;String[] split=data.perks()[line][index].split("\n",2);drawString(g,split[0],left+54,top+147,0xFFF6CF6A,true);if(split.length>1){int row=0;for(var wrapped:font.split(Component.literal(split[1]),126)){g.drawString(font,wrapped,left+54,top+158+row*10,0xFFCFC8E8,false);row++;}}
  int cost=data.costs()[index];boolean next=selected==data.levels()[line]+1,enabled=next&&data.points()>=cost,maxed=data.levels()[line]>=10;int bx=left+180,by=top+148,uv=enabled?((mouseX>=bx&&mouseX<bx+64&&mouseY>=by&&mouseY<by+20)?64:0):128;blit(g,WIDGETS,bx,by,uv,28,64,20);String label=maxed?"Maxed":"Upgrade";g.drawString(font,label,bx+32-font.width(label)/2,by+6,0xFFFFE7A1,true);String costText="Cost: "+cost+" pt";g.drawString(font,costText,left+212-font.width(costText)/2,top+173,data.points()>=cost?0xFF8CF29B:0xFFFF6B6B,false);
  if(mouseX>=left+7&&mouseX<left+39)for(int i=0;i<6;i++){int y=top+28+i*28;if(mouseY>=y&&mouseY<y+26){g.renderTooltip(font,Component.literal(LINES[i]+" (Lv "+data.levels()[i]+"/10)"),mouseX,mouseY);break;}}
  super.render(g,mouseX,mouseY,delta);
 }
 @Override public boolean mouseClicked(double mx,double my,int button){if(button!=0)return super.mouseClicked(mx,my,button);int x=(int)mx-left,y=(int)my-top;for(int i=0;i<6;i++)if(x>=7&&x<39&&y>=28+i*28&&y<54+i*28){line=i;selected=Math.min(10,(data==null?0:data.levels()[line])+1);return true;}for(int i=0;i<10;i++){int cx=CENTERS[i][0],cy=CENTERS[i][1];if(Math.abs(x-cx)<=12&&Math.abs(y-cy)<=12){selected=i+1;return true;}}if(data!=null&&x>=180&&x<244&&y>=148&&y<168&&selected==data.levels()[line]+1&&data.points()>=data.costs()[selected-1]){net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new SkillUpgradePayload(line));return true;}return super.mouseClicked(mx,my,button);}
 @Override public boolean isPauseScreen(){return false;}
 private void drawString(GuiGraphics g,String s,int x,int y,int color,boolean shadow){g.drawString(font,s,x,y,color,shadow);}
 private static void blit(GuiGraphics g,ResourceLocation tex,int x,int y,int u,int v,int w,int h){g.blit(tex,x,y,u,v,w,h,256,256);}
 private static void line(GuiGraphics g,int x1,int y1,int x2,int y2,int color){if(x1==x2)g.fill(x1-1,Math.min(y1,y2),x1+1,Math.max(y1,y2)+1,color);else{int x=Math.min(x1,x2),w=Math.abs(x2-x1),y=Math.min(y1,y2);for(int i=0;i<w;i++){int yy=y+(int)Math.round((double)i*(y2-y1)/(x2-x1));g.fill(x+i,yy,x+i+1,yy+2,color);}}}
 private static int rgb(String s){return 0xFF000000|Integer.parseInt(s.substring(1),16);}
}