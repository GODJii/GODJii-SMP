package com.godjiismp.skills;
import net.minecraft.server.level.ServerPlayer;
import java.util.concurrent.ThreadLocalRandom;
/** Public bridge for daily and future major quests. */
public final class SkillApi {private SkillApi(){}public static void awardPoints(ServerPlayer player,int points){Skills.awardPoints(player,points);}public static void awardRegularQuest(ServerPlayer player){awardPoints(player,SkillsConfig.normalQuestPoints);}public static void awardMajorQuest(ServerPlayer player){awardPoints(player,ThreadLocalRandom.current().nextInt(SkillsConfig.majorQuestMin,SkillsConfig.majorQuestMax+1));}}
