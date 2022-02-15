package com.telepathicgrunt.the_bumblezone.blocks;

import com.mojang.datafixers.util.Pair;
import com.telepathicgrunt.the_bumblezone.blocks.blockentities.HoneyCocoonBlockEntity;
import com.telepathicgrunt.the_bumblezone.modinit.BzBlockEntities;
import com.telepathicgrunt.the_bumblezone.modinit.BzCriterias;
import com.telepathicgrunt.the_bumblezone.modinit.BzFluids;
import com.telepathicgrunt.the_bumblezone.modinit.BzItems;
import com.telepathicgrunt.the_bumblezone.tags.BzFluidTags;
import com.telepathicgrunt.the_bumblezone.tags.BzItemTags;
import com.telepathicgrunt.the_bumblezone.utils.GeneralUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class HoneyCocoon extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    protected final VoxelShape shape;

    public HoneyCocoon() {
        super(Properties.of(Material.EGG, MaterialColor.COLOR_YELLOW).strength(0.3F, 0.3F).randomTicks().noOcclusion().sound(SoundType.HONEY_BLOCK));
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, Boolean.FALSE));

        VoxelShape voxelshape = Block.box(1.0D, 1.0D, 1.0D, 15.0D, 14.0D, 15.0D);
        voxelshape = Shapes.or(voxelshape, Block.box(2.0D, 0.0D, 2.0D, 14.0D, 1.0D, 14.0D));
        voxelshape = Shapes.or(voxelshape, Block.box(3.0D, 14.0D, 3.0D, 13.0D, 16.0D, 13.0D));
        shape = voxelshape;
    }

    @Override
    public VoxelShape getShape(BlockState blockstate, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return BzBlockEntities.HONEY_COCOON_BE.get().create(blockPos, blockState);
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    /**
     * Setup properties
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    /**
     * Can be waterlogged so return sugar water fluid if so
     */
    @SuppressWarnings("deprecation")
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? BzFluids.SUGAR_WATER_FLUID.get().getSource(false) : super.getFluidState(state);
    }

    /**
     * begin fluid tick if waterlogged
     */
    @SuppressWarnings("deprecation")
    @Override
    public BlockState updateShape(BlockState blockstate, Direction facing,
                                  BlockState facingState, LevelAccessor world,
                                  BlockPos currentPos, BlockPos facingPos) {

        if (blockstate.getValue(WATERLOGGED)) {
            world.scheduleTick(currentPos, BzFluids.SUGAR_WATER_FLUID.get(), BzFluids.SUGAR_WATER_FLUID.get().getTickDelay(world));
        }

        return super.updateShape(blockstate, facing, facingState, world, currentPos, facingPos);
    }

    /**
     * sets waterlogging as well if replacing water
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState blockstate = this.defaultBlockState();
        FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
        return blockstate.setValue(WATERLOGGED, fluidstate.getType().is(BzFluidTags.CONVERTIBLE_TO_SUGAR_WATER) && fluidstate.isSource());
    }

    @Override
    public void randomTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, Random random) {
        BlockState aboveState = serverLevel.getBlockState(blockPos.above());
        if(blockState.getValue(WATERLOGGED) && aboveState.getFluidState().is(FluidTags.WATER) && aboveState.getCollisionShape(serverLevel, blockPos).isEmpty()) {

            BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
            if(blockEntity instanceof HoneyCocoonBlockEntity honeyCocoonBlockEntity) {
                List<Pair<ItemStack, Integer>> itemStacks = new ArrayList<>();
                for(int i = 0; i < honeyCocoonBlockEntity.getContainerSize(); i++) {
                    ItemStack itemStack = honeyCocoonBlockEntity.getItem(i);
                    if(!itemStack.isEmpty()) {
                        itemStacks.add(new Pair<>(itemStack, i));
                    }
                }

                if(itemStacks.isEmpty()) {
                    return;
                }

                ItemStack takenItem = honeyCocoonBlockEntity.removeItem(itemStacks.get(random.nextInt(itemStacks.size())).getSecond(), 1);
                spawnItemEntity(serverLevel, blockPos, takenItem, -0.2D);
            }
        }
        else if(!blockState.getValue(WATERLOGGED) && aboveState.getCollisionShape(serverLevel, blockPos).isEmpty()) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
            if(blockEntity instanceof HoneyCocoonBlockEntity honeyCocoonBlockEntity) {
                List<Pair<ItemStack, Integer>> emptyBroods = new ArrayList<>();
                List<Pair<ItemStack, Integer>> beeFeeding = new ArrayList<>();
                for(int i = 0; i < honeyCocoonBlockEntity.getContainerSize(); i++) {
                    ItemStack itemStack = honeyCocoonBlockEntity.getItem(i);
                    if(!itemStack.isEmpty()) {
                        if(itemStack.getItem() == BzItems.EMPTY_HONEYCOMB_BROOD.get()) {
                            emptyBroods.add(new Pair<>(itemStack, i));
                        }

                        if(itemStack.is(BzItemTags.BEE_FEEDING_ITEMS)) {
                            beeFeeding.add(new Pair<>(itemStack, i));
                        }
                    }
                }

                if(emptyBroods.isEmpty() || beeFeeding.isEmpty()) {
                    return;
                }


                honeyCocoonBlockEntity.removeItem(emptyBroods.get(random.nextInt(emptyBroods.size())).getSecond(), 1);
                ItemStack consumedItem = honeyCocoonBlockEntity.removeItem(beeFeeding.get(random.nextInt(beeFeeding.size())).getSecond(), 1);
                if(consumedItem.hasContainerItem()) {
                    ItemStack ejectedItem = consumedItem.getContainerItem();
                    spawnItemEntity(serverLevel, blockPos, ejectedItem, 0.2D);
                }

                boolean addedToInv = false;
                for(int i = 0; i < honeyCocoonBlockEntity.getContainerSize(); i++) {
                    ItemStack itemStack = honeyCocoonBlockEntity.getItem(i);
                    if (itemStack.isEmpty() || (itemStack.getItem() == BzItems.HONEYCOMB_BROOD.get() && itemStack.getCount() < 64)) {
                        if(itemStack.isEmpty()) {
                            honeyCocoonBlockEntity.setItem(i, BzItems.HONEYCOMB_BROOD.get().getDefaultInstance());
                        }
                        else {
                            itemStack.grow(1);
                        }

                        addedToInv = true;
                        break;
                    }
                }
                if(!addedToInv) {
                    spawnItemEntity(serverLevel, blockPos, BzItems.HONEYCOMB_BROOD.get().getDefaultInstance(), 0.2D);
                }
            }
        }
    }

    private static void spawnItemEntity(ServerLevel serverLevel, BlockPos blockPos, ItemStack itemToSpawn, double ySpeed) {
        if(!itemToSpawn.isEmpty()) {
            ItemEntity itemEntity = new ItemEntity(
                    serverLevel,
                    blockPos.getX() + 0.5D,
                    blockPos.getY() + 1D,
                    blockPos.getZ() + 0.5D,
                    itemToSpawn);
            itemEntity.setDefaultPickUpDelay();
            itemEntity.setDeltaMovement(new Vec3(0, ySpeed, 0));
            serverLevel.addFreshEntity(itemEntity);
        }
    }

    @Override
    public InteractionResult use(BlockState blockstate, Level world,
                                 BlockPos position, Player playerEntity,
                                 InteractionHand playerHand, BlockHitResult raytraceResult) {

        ItemStack itemstack = playerEntity.getItemInHand(playerHand);

        if (itemstack.getItem() == Items.GLASS_BOTTLE && blockstate.getValue(WATERLOGGED)) {

            world.playSound(playerEntity, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(),
                    SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);

            GeneralUtils.givePlayerItem(playerEntity, playerHand, new ItemStack(BzItems.SUGAR_WATER_BOTTLE.get()), false, true);
            return InteractionResult.SUCCESS;
        }
        else if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        else {
            MenuProvider menuprovider = this.getMenuProvider(blockstate, world, position);
            if (menuprovider != null) {
                playerEntity.openMenu(menuprovider);
            }

            return InteractionResult.CONSUME;
        }
    }

    @Override
    public boolean canPlaceLiquid(BlockGetter world, BlockPos blockPos, BlockState blockState, Fluid fluid) {
        return !blockState.getValue(WATERLOGGED) && fluid.is(BzFluidTags.CONVERTIBLE_TO_SUGAR_WATER) && fluid.defaultFluidState().isSource();
    }

    @Override
    public boolean placeLiquid(LevelAccessor world, BlockPos blockPos, BlockState blockState, FluidState fluidState) {
        if (!blockState.getValue(WATERLOGGED) && fluidState.getType().is(BzFluidTags.CONVERTIBLE_TO_SUGAR_WATER) && fluidState.isSource()) {
            if (!world.isClientSide()) {
                world.setBlock(blockPos, blockState.setValue(WATERLOGGED, true), 3);
                world.scheduleTick(blockPos, BzFluids.SUGAR_WATER_FLUID.get(), BzFluids.SUGAR_WATER_FLUID.get().getTickDelay(world));
            }
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public ItemStack pickupBlock(LevelAccessor world, BlockPos blockPos, BlockState blockState) {
        if (blockState.getValue(WATERLOGGED)) {
            world.setBlock(blockPos, blockState.setValue(WATERLOGGED, false), 3);
            return new ItemStack(BzItems.SUGAR_WATER_BUCKET.get());
        }
        else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack itemStack) {
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, itemStack) > 0 && player instanceof ServerPlayer serverPlayer) {
            BzCriterias.HONEY_COCOON_SILK_TOUCH_TRIGGER.trigger(serverPlayer);
        }

        super.playerDestroy(level, player, pos, state, blockEntity, itemStack);
    }

    /**
     * Called periodically clientside on blocks near the player to show honey particles.
     */
    @Override
    public void animateTick(BlockState blockState, Level world, BlockPos position, Random random) {
        if(!blockState.getValue(WATERLOGGED)) {
            if (world.random.nextFloat() < 0.05F) {
                this.spawnHoneyParticles(world, position);
            }
        }
    }

    /**
     * intermediary method to apply the blockshape and ranges that the particle can spawn in for the next addHoneyParticle
     * method
     */
    private void spawnHoneyParticles(Level world, BlockPos position) {
        double x = (world.random.nextDouble() * 14) + 1;
        double y = (world.random.nextDouble() * 6) + 5;
        double z = (world.random.nextDouble() * 14) + 1;

        if (world.random.nextBoolean()) {
            if (world.random.nextBoolean()) x = 0.8D;
            else x = 15.2;
        }
        else {
            if (world.random.nextBoolean()) z = 0.8D;
            else z = 15.2;
        }

        world.addParticle(ParticleTypes.FALLING_HONEY,
                (x / 16) + position.getX(),
                (y / 16) + position.getY(),
                (z / 16) + position.getZ(),
                0.0D,
                0.0D,
                0.0D);
    }
}