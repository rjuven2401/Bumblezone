package net.telepathicgrunt.bumblezone.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;


public class PorousHoneycombBlock extends Block
{

	public PorousHoneycombBlock()
	{
		super(Block.Properties.create(Material.CLAY, MaterialColor.ADOBE).hardnessAndResistance(0.5F).sound(SoundType.CORAL));
		setRegistryName("porous_honeycomb_block");
	}


	/**
	 * Allow player to harvest honey and put honey into this block using bottles
	 */
	@SuppressWarnings("deprecation")
	public ActionResultType onUse(BlockState thisBlockState, World world, BlockPos position, PlayerEntity playerEntity, Hand playerHand, BlockRayTraceResult raytraceResult)
	{
		ItemStack itemstack = playerEntity.getHeldItem(playerHand);

		/*
		 * Player is adding honey to this block if it is not filled with honey
		 */
		if (itemstack.getItem() == Items.field_226638_pX_)
		{
			if (!world.isRemote)
			{
				itemstack.shrink(1); // remove current honey bottle
				world.setBlockState(position, BzBlocksInit.FILLED_POROUS_HONEYCOMB.get().getDefaultState(), 3); // added honey to this block

				world.playSound(playerEntity, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.NEUTRAL, 1.0F, 1.0F);
				if (itemstack.isEmpty())
				{
					playerEntity.setHeldItem(playerHand, new ItemStack(Items.GLASS_BOTTLE)); // places empty bottle in hand
				}
				else if (!playerEntity.inventory.addItemStackToInventory(new ItemStack(Items.GLASS_BOTTLE))) // places empty bottle in inventory
				{
					playerEntity.dropItem(new ItemStack(Items.GLASS_BOTTLE), false); // drops empty bottle if inventory is full
				}
			}

			return ActionResultType.SUCCESS;
		}
		else
		{
			return super.onUse(thisBlockState, world, position, playerEntity, playerHand, raytraceResult);
		}
	}

}