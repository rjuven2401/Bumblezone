package com.telepathicgrunt.the_bumblezone.world.dimension;

import com.google.common.primitives.Doubles;
import com.telepathicgrunt.the_bumblezone.Bumblezone;
import com.telepathicgrunt.the_bumblezone.capabilities.IPlayerPosAndDim;
import com.telepathicgrunt.the_bumblezone.capabilities.PlayerPositionAndDimension;
import com.telepathicgrunt.the_bumblezone.utils.BzPlacingUtils;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import org.apache.logging.log4j.Level;

public class BzPlayerPlacement {

    @CapabilityInject(IPlayerPosAndDim.class)
    public static Capability<IPlayerPosAndDim> PAST_POS_AND_DIM = null;

    public static void enteringBumblezone(Entity entity){
        //Note, the player does not hold the previous dimension oddly enough.
        Vector3d destinationPosition;

        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity playerEntity = ((ServerPlayerEntity) entity);
            PlayerPositionAndDimension cap = (PlayerPositionAndDimension) playerEntity.getCapability(PAST_POS_AND_DIM).orElseThrow(RuntimeException::new);
            cap.setNonBZDim(playerEntity.getCommandSenderWorld().dimension().location());
            cap.setNonBZYaw(playerEntity.yRot);
            cap.setNonBZPitch(playerEntity.xRot);
            cap.setNonBZPos(playerEntity.position());

            MinecraftServer minecraftServer = playerEntity.getServer(); // the server itself
            ServerWorld bumblezoneWorld = minecraftServer.getLevel(BzDimension.BZ_WORLD_KEY);

        // Prevent crash due to mojang bug that makes mod's json dimensions not exist upload first creation of world on server. A restart fixes this.
            if(bumblezoneWorld == null){
                Bumblezone.LOGGER.log(Level.INFO, "Bumblezone: Please restart the server. The Bumblezone dimension hasn't been made yet due to this bug: https://bugs.mojang.com/browse/MC-195468. A restart will fix this.");
                ITextComponent message = new StringTextComponent("Please restart the server. The Bumblezone dimension hasn't been made yet due to this bug: §6https://bugs.mojang.com/browse/MC-195468§f. A restart will fix this.");
                playerEntity.displayClientMessage(message, true);
                return;
            }

            destinationPosition = teleportByPearl(playerEntity, playerEntity.getLevel(), bumblezoneWorld);
            playerEntity.teleportTo(
                    bumblezoneWorld,
                    destinationPosition.x,
                    destinationPosition.y,
                    destinationPosition.z,
                    playerEntity.yRot,
                    playerEntity.xRot
            );
        }
    }


    public static void exitingBumblezone(Entity entity, ServerWorld destination){
        boolean upwardChecking = entity.getY() > 0;
        Vector3d destinationPosition;

        if (entity instanceof ServerPlayerEntity) {
            PlayerPositionAndDimension cap = (PlayerPositionAndDimension) entity.getCapability(PAST_POS_AND_DIM).orElseThrow(RuntimeException::new);
            destinationPosition = teleportByOutOfBounds((PlayerEntity) entity, destination, upwardChecking);

            ((ServerPlayerEntity)entity).teleportTo(
                    destination,
                    destinationPosition.x,
                    destinationPosition.y,
                    destinationPosition.z,
                    cap.getNonBZYaw(),
                    cap.getNonBZPitch()
            );
        }
    }


    private static Vector3d teleportByOutOfBounds(PlayerEntity playerEntity, ServerWorld destination, boolean checkingUpward) {
        //converts the position to get the corresponding position in non-bumblezone dimension
        double coordinateScale = playerEntity.getCommandSenderWorld().dimensionType().coordinateScale() / destination.dimensionType().coordinateScale();
        BlockPos finalSpawnPos;
        BlockPos validBlockPos = null;

        PlayerPositionAndDimension cap = (PlayerPositionAndDimension) playerEntity.getCapability(PAST_POS_AND_DIM).orElseThrow(RuntimeException::new);

        if(Bumblezone.BzDimensionConfig.teleportationMode.get() == 1){
            finalSpawnPos = new BlockPos(
                    Doubles.constrainToRange(playerEntity.position().x() * coordinateScale, -29999936D, 29999936D),
                    playerEntity.position().y(),
                    Doubles.constrainToRange(playerEntity.position().z() * coordinateScale, -29999936D, 29999936D));

            //Gets valid space in other world
            validBlockPos = validPlayerSpawnLocationByBeehive(destination, finalSpawnPos, 48, checkingUpward);
        }

        else if(Bumblezone.BzDimensionConfig.teleportationMode.get() == 2){
            if(cap.getNonBZPos() != null){
                validBlockPos = new BlockPos(cap.getNonBZPos());
            }
        }

        // Teleportaion mode 3
        else{
            finalSpawnPos = new BlockPos(
                    Doubles.constrainToRange(playerEntity.position().x() * coordinateScale, -29999936D, 29999936D),
                    playerEntity.position().y(),
                    Doubles.constrainToRange(playerEntity.position().z() * coordinateScale, -29999936D, 29999936D));

            //Gets valid space in other world
            validBlockPos = validPlayerSpawnLocationByBeehive(destination, finalSpawnPos, 48, checkingUpward);

            if(validBlockPos == null && cap.getNonBZPos() != null) {
                validBlockPos = new BlockPos(cap.getNonBZPos());
            }
        }

        // If all else fails, fallback to player pos
        finalSpawnPos = validBlockPos;
        if(finalSpawnPos == null) {
            finalSpawnPos = new BlockPos(playerEntity.position());
        }

        // Make sure spacing is safe if in mode 2 or mode 3 when doing forced teleportation when valid land isn't found.
        if (Bumblezone.BzDimensionConfig.teleportationMode.get() == 2 ||
            (Bumblezone.BzDimensionConfig.teleportationMode.get() == 3 && validBlockPos == null))
        {

            if (destination.getBlockState(finalSpawnPos.above()).canOcclude()) {
                destination.setBlock(finalSpawnPos, Blocks.AIR.defaultBlockState(), 3);
                destination.setBlock(finalSpawnPos.above(), Blocks.AIR.defaultBlockState(), 3);
            }
        }

        //use found location
        //teleportation spot finding complete. return spot
        return new Vector3d(
                finalSpawnPos.getX() + 0.5D,
                finalSpawnPos.getY() + 1,
                finalSpawnPos.getZ() + 0.5D
        );
    }

    private static Vector3d teleportByPearl(PlayerEntity playerEntity, ServerWorld originalWorld, ServerWorld bumblezoneWorld) {


        //converts the position to get the corresponding position in bumblezone dimension
        double coordinateScale = 1;
        if (Bumblezone.BzDimensionConfig.teleportationMode.get() != 2) {
            coordinateScale = originalWorld.dimensionType().coordinateScale() / bumblezoneWorld.dimensionType().coordinateScale();
        }
        BlockPos blockpos = new BlockPos(
                Doubles.constrainToRange(playerEntity.position().x() * coordinateScale, -29999936D, 29999936D),
                playerEntity.position().y(),
                Doubles.constrainToRange(playerEntity.position().z() * coordinateScale, -29999936D, 29999936D));


        //gets valid space in other world
        BlockPos validBlockPos = validPlayerSpawnLocation(bumblezoneWorld, blockpos, 10);


        //No valid space found around destination. Begin secondary valid spot algorithms
        if (validBlockPos == null) {
            //go down to first solid land with air above.
            validBlockPos = new BlockPos(
                    blockpos.getX(),
                    BzPlacingUtils.topOfSurfaceBelowHeightThroughWater(bumblezoneWorld, blockpos.getY(), 0, blockpos) + 1,
                    blockpos.getZ());

            //No solid land was found. Who digs out an entire chunk?!
            if (validBlockPos.getY() == 0) {
                validBlockPos = null;
            }
            //checks if spot is not two water blocks with air block able to be reached above
            else if (bumblezoneWorld.getBlockState(validBlockPos).getMaterial() == Material.WATER &&
                    bumblezoneWorld.getBlockState(validBlockPos.above()).getMaterial() == Material.WATER) {
                BlockPos.Mutable mutable = new BlockPos.Mutable(validBlockPos.getX(), validBlockPos.getY(), validBlockPos.getZ());

                //moves upward looking for air block while not interrupted by a solid block
                while (mutable.getY() < 255 && !bumblezoneWorld.isEmptyBlock(mutable) || bumblezoneWorld.getBlockState(mutable).getMaterial() == Material.WATER) {
                    mutable.move(Direction.UP);
                }
                if (bumblezoneWorld.getBlockState(mutable).getMaterial() != Material.AIR) {
                    validBlockPos = null; // No air found. Let's not place player here where they could drown
                } else {
                    validBlockPos = mutable; // Set player to top of water level
                }
            }
            //checks if spot is not a non-solid block with air block above
            else if ((!bumblezoneWorld.isEmptyBlock(validBlockPos) && bumblezoneWorld.getBlockState(validBlockPos).getMaterial() != Material.WATER) &&
                    bumblezoneWorld.getBlockState(validBlockPos.above()).getMaterial() != Material.AIR) {
                validBlockPos = null;
            }


            //still no valid position, time to force a valid location ourselves
            if (validBlockPos == null) {
                //We are going to spawn player at exact spot of scaled coordinates by placing air at the spot with honeycomb bottom
                //and honeycomb walls to prevent drowning
                //This is the last resort
                bumblezoneWorld.setBlockAndUpdate(blockpos, Blocks.AIR.defaultBlockState());
                bumblezoneWorld.setBlockAndUpdate(blockpos.above(), Blocks.AIR.defaultBlockState());

                bumblezoneWorld.setBlockAndUpdate(blockpos.below(), Blocks.HONEYCOMB_BLOCK.defaultBlockState());
                bumblezoneWorld.setBlockAndUpdate(blockpos.above().above(), Blocks.HONEYCOMB_BLOCK.defaultBlockState());

                bumblezoneWorld.setBlockAndUpdate(blockpos.north(), Blocks.HONEYCOMB_BLOCK.defaultBlockState());
                bumblezoneWorld.setBlockAndUpdate(blockpos.west(), Blocks.HONEYCOMB_BLOCK.defaultBlockState());
                bumblezoneWorld.setBlockAndUpdate(blockpos.east(), Blocks.HONEYCOMB_BLOCK.defaultBlockState());
                bumblezoneWorld.setBlockAndUpdate(blockpos.south(), Blocks.HONEYCOMB_BLOCK.defaultBlockState());
                bumblezoneWorld.setBlockAndUpdate(blockpos.north().above(), Blocks.HONEYCOMB_BLOCK.defaultBlockState());
                bumblezoneWorld.setBlockAndUpdate(blockpos.west().above(), Blocks.HONEYCOMB_BLOCK.defaultBlockState());
                bumblezoneWorld.setBlockAndUpdate(blockpos.east().above(), Blocks.HONEYCOMB_BLOCK.defaultBlockState());
                bumblezoneWorld.setBlockAndUpdate(blockpos.south().above(), Blocks.HONEYCOMB_BLOCK.defaultBlockState());
                validBlockPos = blockpos;
            }

        }

        // if player throws pearl at hive and then goes to sleep, they wake up
        if (playerEntity.isSleeping()) {
            playerEntity.stopSleeping();
        }

        // place hive block below player if they would've fallen out of dimension
        // because there's air all the way down to y = 0 below player
        int heightCheck = 0;
        while(heightCheck <= validBlockPos.getY() && bumblezoneWorld.getBlockState(validBlockPos.below(heightCheck)).isAir()){
            heightCheck++;
        }
        if(heightCheck >= validBlockPos.getY()){
            bumblezoneWorld.setBlockAndUpdate(validBlockPos.getY() == 0 ? validBlockPos : validBlockPos.below(), Blocks.HONEYCOMB_BLOCK.defaultBlockState());
        }

        // teleportation spot finding complete. return spot
        return new Vector3d(
                validBlockPos.getX() + 0.5D,
                validBlockPos.getY(),
                validBlockPos.getZ() + 0.5D
        );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Util


    private static BlockPos validPlayerSpawnLocationByBeehive(World world, BlockPos position, int maximumRange, boolean checkingUpward) {

        // Gets the height of highest block over the area so we aren't checking an
        // excessive amount of area above that doesn't need checking.
        int maxHeight = 0;
        int halfRange = maximumRange / 2;
        BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable();
        for (int x = -halfRange; x < halfRange; x++) {
            for (int z = -halfRange; z < halfRange; z++) {
                mutableBlockPos.set(position.getX() + x, 0, position.getZ() + z);
                if (!world.hasChunk(mutableBlockPos.getX() >> 4, mutableBlockPos.getZ() >> 4)) {
                    //make game generate chunk so we can get max height of blocks in it
                    world.getChunk(mutableBlockPos);
                }
                maxHeight = Math.max(maxHeight, world.getHeight(Heightmap.Type.MOTION_BLOCKING, mutableBlockPos.getX(), mutableBlockPos.getZ()));
            }
        }
        maxHeight = Math.min(maxHeight, world.getMaxBuildHeight() - 1); //cannot place user at roof of other dimension

        //snaps the coordinates to chunk origin and then sets height to minimum or maximum based on search direction
        mutableBlockPos.set(position.getX(), checkingUpward ? 0 : maxHeight, position.getZ());


        //scans range from y = 0 to dimension max height for a bee_nest
        //Does it by checking each y layer at a time
        while (mutableBlockPos.getY() >= 0 && mutableBlockPos.getY() <= maxHeight) {
            if (!Bumblezone.BzDimensionConfig.seaLevelOrHigherExitTeleporting.get() ||
                    mutableBlockPos.getY() > world.getSeaLevel()) {

                for (int range = 0; range < maximumRange; range++) {
                    int radius = range * range;
                    int nextRadius = (range + 1) * (range + 1);
                    for (int x = 0; x <= range * 2; x++) {
                        int x2 = x > range ? -(x - range) : x;

                        for (int z = 0; z <= range * 2; z++) {
                            int z2 = z > range ? -(z - range) : x;

                            //checks within the circular ring and not check the same positions multiple times
                            if (x2 * x2 + z2 * z2 >= radius && x2 * x2 + z2 * z2 < nextRadius) {
                                mutableBlockPos.set(position.getX() + x2, mutableBlockPos.getY(), position.getZ() + z2);

                                if (world.getBlockState(mutableBlockPos).getBlock() == Blocks.BEE_NEST) {
                                    //A Hive was found, try to find a valid spot next to it
                                    BlockPos validSpot = validPlayerSpawnLocation(world, mutableBlockPos, 4);
                                    if (validSpot != null) {
                                        return validSpot;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //move the block pos in the direction it needs to go
            if (checkingUpward) {
                mutableBlockPos.move(Direction.UP);
            } else {
                mutableBlockPos.move(Direction.DOWN);
            }
        }


        //this mode will not generate a beenest automatically.
        if(Bumblezone.BzDimensionConfig.teleportationMode.get() == 3) return null;

        //no valid spot was found, generate a hive and spawn us on the highest land
        //This if statement is so we dont get placed on roof of other roofed dimension
        if (maxHeight + 1 < world.getMaxBuildHeight()) {
            maxHeight += 1;
        }
        mutableBlockPos.set(
                position.getX(),
                BzPlacingUtils.topOfSurfaceBelowHeight(world, maxHeight, 0, position),
                position.getZ());

        if (mutableBlockPos.getY() <= 0) {
            //No valid spot was found. Just place character on a generate hive at center of height of coordinate
            //Basically just f*** it at this point lol
            mutableBlockPos.set(
                    position.getX(),
                    world.getMaxBuildHeight() / 2,
                    position.getZ());

        }
        createSpaceForPlayer(world, mutableBlockPos);
        return mutableBlockPos;
    }

    private static void createSpaceForPlayer(World world, BlockPos.Mutable mutableBlockPos) {
        if(Bumblezone.BzDimensionConfig.generateBeenest.get())
            world.setBlockAndUpdate(mutableBlockPos, Blocks.BEE_NEST.defaultBlockState());
        else if(world.getBlockState(mutableBlockPos).getMaterial() == Material.AIR ||
                (!world.getBlockState(mutableBlockPos).getFluidState().isEmpty() &&
                    !world.getBlockState(mutableBlockPos).getFluidState().is(FluidTags.WATER)))
            world.setBlockAndUpdate(mutableBlockPos, Blocks.HONEYCOMB_BLOCK.defaultBlockState());

        world.setBlockAndUpdate(mutableBlockPos.above(), Blocks.AIR.defaultBlockState());
    }

    private static BlockPos validPlayerSpawnLocation(World world, BlockPos position, int maximumRange) {
        //Try to find 2 non-solid spaces around it that the player can spawn at
        int radius;
        int outerRadius;
        int distanceSq;
        BlockPos.Mutable currentPos = new BlockPos.Mutable(position.getX(), position.getY(), position.getZ());

        //checks for 2 non-solid blocks with solid block below feet
        //checks outward from center position in both x, y, and z.
        //The x2, y2, and z2 is so it checks at center of the range box instead of the corner.
        for (int range = 0; range < maximumRange; range++) {
            radius = range * range;
            outerRadius = (range + 1) * (range + 1);

            for (int y = 0; y <= range * 2; y++) {
                int y2 = y > range ? -(y - range) : y;


                for (int x = 0; x <= range * 2; x++) {
                    int x2 = x > range ? -(x - range) : x;


                    for (int z = 0; z <= range * 2; z++) {
                        int z2 = z > range ? -(z - range) : z;

                        distanceSq = x2 * x2 + z2 * z2 + y2 * y2;
                        if (distanceSq >= radius && distanceSq < outerRadius) {
                            currentPos.set(position.offset(x2, y2, z2));
                            if (world.getBlockState(currentPos.below()).canOcclude() &&
                                    world.getBlockState(currentPos).getMaterial() == Material.AIR &&
                                    world.getBlockState(currentPos.above()).getMaterial() == Material.AIR) {
                                //valid space for player is found
                                return currentPos;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
}