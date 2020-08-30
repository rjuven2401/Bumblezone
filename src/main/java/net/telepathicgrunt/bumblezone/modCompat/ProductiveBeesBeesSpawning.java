package net.telepathicgrunt.bumblezone.modCompat;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.telepathicgrunt.bumblezone.Bumblezone;

@Mod.EventBusSubscriber(modid = Bumblezone.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ProductiveBeesBeesSpawning
{
	@Mod.EventBusSubscriber(modid = Bumblezone.MODID)
	private static class ForgeEvents
	{
		/*
		 * Manual spawning of Beesourceful Bees so it can be disabled real time by config.
		 * works by making 1/15th of bees spawning also spawn beesourceful bees
		 */
		@SubscribeEvent
		public static void MobSpawnEvent(LivingSpawnEvent.CheckSpawn event)
		{
			if(ModChecker.productiveBeesPresent)
			{
				MobEntity entity = (MobEntity)event.getEntity();
				if(Bumblezone.BzConfig.spawnProductiveBeesBeesMob.get() && 
					entity.world.getServer().getRegistryManager().getDimensionTypes().getKey(entity.world.getDimension()) == Bumblezone.MOD_DIMENSION_ID &&
					entity.world.getRandom().nextInt(15) == 0 &&
					entity.getType() == EntityType.BEE) 
				{
				    ProductiveBeesRedirection.PBMobSpawnEvent(event);
				}
			}
		}
	}
}