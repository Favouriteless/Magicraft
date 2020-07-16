package com.favouriteless.magicraft.tileentity;

import com.favouriteless.magicraft.blocks.FumeFunnel;
import com.favouriteless.magicraft.blocks.WitchOven;
import com.favouriteless.magicraft.containers.WitchOvenContainer;
import com.favouriteless.magicraft.init.MagicraftBlocks;
import com.favouriteless.magicraft.init.MagicraftItems;
import com.favouriteless.magicraft.init.MagicraftTileEntities;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.*;
import net.minecraft.tileentity.*;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Random;

public class WitchOvenTileEntity extends FurnaceTileEntityBase {

    private static final Random random = new Random();
    private static final HashMap<Item, Item> BYPRODUCTS = new HashMap<Item, Item>();

    public static void init() {
        BYPRODUCTS.put(Items.BIRCH_SAPLING, MagicraftItems.BREATH_OF_THE_GODDESS.get());
        BYPRODUCTS.put(Items.OAK_SAPLING, MagicraftItems.EXHALE_OF_THE_HORNED_ONE.get());
        BYPRODUCTS.put(Items.JUNGLE_SAPLING, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.ACACIA_LOG, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.BIRCH_LOG, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.DARK_OAK_LOG, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.JUNGLE_LOG, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.OAK_LOG, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.SPRUCE_LOG, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.POTATO, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.BEEF, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.PORKCHOP, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.MUTTON, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.RABBIT, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.CHICKEN, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.COD, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.SALMON, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.KELP, MagicraftItems.FOUL_FUME.get());
        BYPRODUCTS.put(Items.SPRUCE_SAPLING, MagicraftItems.HINT_OF_REBIRTH.get());
        BYPRODUCTS.put(MagicraftItems.ROWAN_SAPLING.get(), MagicraftItems.WHIFF_OF_MAGIC.get());
        BYPRODUCTS.put(MagicraftItems.HAWTHORN_SAPLING.get(), MagicraftItems.ODOUR_OF_PURITY.get());
        BYPRODUCTS.put(MagicraftItems.ALDER_SAPLING.get(), MagicraftItems.REEK_OF_MISFORTUNE.get());
    }

    public WitchOvenTileEntity(TileEntityType<?> typeIn) {
        super(typeIn, NonNullList.withSize(5, ItemStack.EMPTY));
    }

    public WitchOvenTileEntity() {
        this(MagicraftTileEntities.WITCH_OVEN.get());
    }


    @Override
    protected ITextComponent getDefaultName() {
        return new TranslationTextComponent("container.witch_oven");
    }

    @Override
    protected Container createMenu(int id, PlayerInventory player) {
        return new WitchOvenContainer(id, player, this, this.furnaceData);
    }

    @Override
    public void tick() {
        boolean flag = this.isBurning();
        boolean flag1 = false;
        if (this.isBurning()) {
            --this.burnTime;
        }

        if (!this.world.isRemote) {
            ItemStack itemstack = this.inventoryContents.get(1);
            if (this.isBurning() || !itemstack.isEmpty() && !this.inventoryContents.get(0).isEmpty()) {
                IRecipe<?> irecipe = this.world.getRecipeManager().getRecipe(IRecipeType.SMELTING, this, this.world).orElse(null);
                if (!this.isBurning() && this.canSmelt(irecipe)) {
                    this.burnTime = this.getBurnTime(itemstack);
                    this.recipesUsed = this.burnTime;
                    if (this.isBurning()) {
                        flag1 = true;
                        if (itemstack.hasContainerItem())
                            this.inventoryContents.set(1, itemstack.getContainerItem());
                        else
                        if (!itemstack.isEmpty()) {
                            Item item = itemstack.getItem();
                            itemstack.shrink(1);
                            if (itemstack.isEmpty()) {
                                this.inventoryContents.set(1, itemstack.getContainerItem());
                            }
                        }
                    }
                }

                if (this.isBurning() && this.canSmelt(irecipe)) {
                    ++this.cookTime;
                    if (this.cookTime == this.cookTimeTotal) {
                        this.cookTime = 0;
                        this.cookTimeTotal = this.getCookTime();
                        this.smelt(irecipe);
                        flag1 = true;
                    }
                } else {
                    this.cookTime = 0;
                }
            } else if (!this.isBurning() && this.cookTime > 0) {
                this.cookTime = MathHelper.clamp(this.cookTime - 2, 0, this.cookTimeTotal);
            }

            if (flag != this.isBurning()) {
                flag1 = true;
                this.world.setBlockState(this.pos, this.world.getBlockState(this.pos).with(AbstractFurnaceBlock.LIT, this.isBurning()), 3);
                this.updateFumeFunnels();
            }
        }

        if (flag1) {
            this.markDirty();
        }

    }

    @Override
    protected void smelt(@Nullable IRecipe<?> recipe) {
        if (recipe != null && this.canSmelt(recipe)) {
            ItemStack itemstack = this.inventoryContents.get(0);
            ItemStack itemstack1 = recipe.getRecipeOutput();
            ItemStack itemstack2 = this.inventoryContents.get(2);

            if(random.nextInt(10) <= getByproductChance()) {
                createByproduct(itemstack);
            }

            if (itemstack2.isEmpty()) {
                this.inventoryContents.set(2, itemstack1.copy());
            } else if (itemstack2.getItem() == itemstack1.getItem()) {
                itemstack2.grow(itemstack1.getCount());
            }

            if (!this.world.isRemote) {
                this.setRecipeUsed(recipe);
            }

            if (itemstack.getItem() == Blocks.WET_SPONGE.asItem() && !this.inventoryContents.get(1).isEmpty() && this.inventoryContents.get(1).getItem() == Items.BUCKET) {
                this.inventoryContents.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemstack.shrink(1);
        }
    }

    private void createByproduct(ItemStack itemStackIn) {
        Item inputItem = itemStackIn.getItem();
        ItemStack outputStack = this.inventoryContents.get(4);
        ItemStack jarStack = this.inventoryContents.get(3);

        if(BYPRODUCTS.keySet().contains(inputItem) && !jarStack.isEmpty()) {
            if (outputStack.isEmpty()) {
                this.inventoryContents.set(4, new ItemStack(BYPRODUCTS.get(inputItem)));
                jarStack.shrink(1);
            } else if (outputStack.getItem() == BYPRODUCTS.get(inputItem) && outputStack.getCount() < outputStack.getMaxStackSize()) {
                outputStack.grow(1);
                jarStack.shrink(1);
            }
        }
    }

    private void updateFumeFunnels() {
        if(!world.isRemote()) {
            BlockState state = this.world.getBlockState(this.pos);
            Direction facing = state.get(WitchOven.FACING);

            BlockPos[] potentialFilters = new BlockPos[]{
                    new BlockPos(this.pos.add(facing.rotateY().getDirectionVec())),
                    new BlockPos(this.pos.add(facing.rotateYCCW().getDirectionVec())),
                    new BlockPos(this.pos.add(Direction.UP.getDirectionVec()))
            };

            for (BlockPos _pos : potentialFilters) {
                if (this.world.getBlockState(_pos).getBlock() instanceof FumeFunnel) {
                    this.world.setBlockState(_pos, this.world.getBlockState(_pos).with(AbstractFurnaceBlock.LIT, this.isBurning()), 3);
                }
            }
        }
    }

    private int getByproductChance() {
        BlockState state = this.world.getBlockState(this.pos);
        Direction facing = state.get(WitchOven.FACING);
        int byproductChance = 1;

        Block[] potentialFilters = new Block[] {
                this.world.getBlockState(this.pos.add(facing.rotateY().getDirectionVec())).getBlock(),
                this.world.getBlockState(this.pos.add(facing.rotateYCCW().getDirectionVec())).getBlock(),
                this.world.getBlockState(this.pos.add(Direction.UP.getDirectionVec())).getBlock()
        };

        for(Block block : potentialFilters) {
            if(block == MagicraftBlocks.FUME_FUNNEL.get()) {
                byproductChance += 1;
            }
            else if(block == MagicraftBlocks.FUME_FUNNEL_FILTERED.get()) {
                byproductChance += 2;
            }
        }
        return byproductChance;
    }

}