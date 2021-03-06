package com.favouriteless.magicraft.rituals;

import com.favouriteless.magicraft.init.MagicraftDamageSources;
import com.favouriteless.magicraft.init.MagicraftRituals;
import net.minecraft.block.BlockState;

import net.minecraft.client.renderer.texture.ITickable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractRitual implements ITickable, IForgeRegistryEntry<AbstractRitual> {

    public String[] GLYPHS_REQUIRED;
    public EntityType<?>[] ENTITIES_REQUIRED;
    public HashMap<Item, Integer> ITEMS_REQUIRED = new HashMap<Item, Integer>();

    protected List<Entity> ENTITIES_TO_KILL = new ArrayList<Entity>();
    protected List<ItemEntity> ITEMS_TO_USE = new ArrayList<ItemEntity>();
    protected List<ItemStack> ITEMS_USED = new ArrayList<ItemStack>();

    protected boolean activating = false; // True if currently killing entities

    public boolean isExecutingEffect;
    protected int ticks = 0;

    public String name; // Registry name of the ritual
    public ServerWorld world; // World ritual started in
    public BlockPos pos; // Position ritual started at
    public UUID casterUUID; // Player who started ritual
    public UUID targetUUID; // Target of the ritual

    public AbstractRitual() { }

    public abstract void execute();
    protected abstract void onTick();

    public void setData(BlockPos pos, UUID caster, UUID target, ServerWorld world) {
        this.pos = pos;
        this.world = world;
        this.casterUUID = caster;
        this.targetUUID = target;
    };

    public CompoundNBT getTag() {
        CompoundNBT tag = new CompoundNBT();

        tag.putString("name", name);
        tag.putString("dimensionKey", world.getDimensionKey().getRegistryName().toString());
        tag.putDouble("xPos", pos.getX());
        tag.putDouble("yPos", pos.getY());
        tag.putDouble("zPos", pos.getZ());
        tag.putUniqueId("casterUUID", casterUUID);

        if(this.targetUUID != null) {
            tag.putUniqueId("targetUUID", targetUUID);
        }

        return tag;
    }

    public void startRitual(BlockState state, ServerWorld world, BlockPos pos, PlayerEntity player) {
        MagicraftRituals.ACTIVE_RITUALS.add(this);
        this.activating = true;
        this.world = world;
        this.pos = pos;
        this.casterUUID = player.getUniqueID();

        for(Entity entity : ENTITIES_TO_KILL) {
            if(entity instanceof ItemEntity) {
                ITEMS_TO_USE.add((ItemEntity)entity);
            }
        }
        ENTITIES_TO_KILL.removeAll(ITEMS_TO_USE);
    }

    @Override
    public void tick() { // Tick based activation stuff, ie items disappearing, entities dying, particles etc
        if (activating) {
            ticks++;

            if (ticks == 20) {
                ticks = 0;

                // OTHER ENTITIES
                if (!ENTITIES_TO_KILL.isEmpty()) {
                    Entity entity = ENTITIES_TO_KILL.get(0);

                    if (entity.isAlive()) {

                        world.spawnParticle(ParticleTypes.POOF, entity.getPosX(), entity.getPosY(), entity.getPosZ(), 10, 0.3, 0.3, 0.3, 0);
                        if (entity instanceof LivingEntity) {
                            entity.attackEntityFrom(MagicraftDamageSources.RITUAL_SACRIFICE, Float.MAX_VALUE);
                        } else {
                            world.playSound(null, entity.getPosX(), entity.getPosY(), entity.getPosZ(), SoundEvents.ENTITY_CHICKEN_EGG, SoundCategory.MASTER, 2f, 1f);
                            entity.remove();
                        }
                        ENTITIES_TO_KILL.remove(entity);
                    } else {
                        stopActivating();
                    }
                    // ITEMS
                } else if (!ITEMS_TO_USE.isEmpty()) {
                    ItemEntity itemEntity = ITEMS_TO_USE.get(0);

                    if (itemEntity.isAlive()) {
                        ItemStack item = itemEntity.getItem();

                        world.spawnParticle(ParticleTypes.POOF, itemEntity.getPosX(), itemEntity.getPosY(), itemEntity.getPosZ(), 10, 0.3, 0.3, 0.3, 0);
                        world.playSound(null, itemEntity.getPosX(), itemEntity.getPosY(), itemEntity.getPosZ(), SoundEvents.ENTITY_CHICKEN_EGG, SoundCategory.MASTER, 2f, 1f);

                        if (item.getCount() <= ITEMS_REQUIRED.get(item.getItem())) {
                            ITEMS_USED.add(item);
                            ITEMS_REQUIRED.replace(item.getItem(), ITEMS_REQUIRED.get(item.getItem()) - item.getCount());
                            itemEntity.remove();
                        } else if (item.getCount() > ITEMS_REQUIRED.get(item.getItem())) {
                            int countNeeded = ITEMS_REQUIRED.get(item.getItem());
                            ITEMS_USED.add(new ItemStack(item.getItem(), countNeeded));
                            item.setCount(item.getCount() - countNeeded);
                            ITEMS_REQUIRED.remove(item.getItem());
                        }
                        ITEMS_TO_USE.remove(itemEntity);

                    } else {
                        stopActivating();
                    }

                } else {
                    execute();
                    activating = false;
                }

            }

        }
        if(this.isExecutingEffect) {
            this.onTick();
        }
    }

    public void stopActivating() {
        activating = false;
        for (ItemStack item : ITEMS_USED) {
            world.addEntity(new ItemEntity(world, pos.getX(), pos.getY() + 1.1, pos.getZ(), item));
        }
        world.spawnParticle(ParticleTypes.CLOUD, pos.getX(), pos.getY() + 1, pos.getZ(), 200, 1, 1, 1, 0);
        world.playSound(null, pos.getX(), pos.getY() + 1, pos.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_SNARE, SoundCategory.MASTER, 2f, 1f);
    }



    @Override
    public AbstractRitual setRegistryName(ResourceLocation name) { return null; }
    @Nullable
    @Override
    public ResourceLocation getRegistryName() { return new ResourceLocation("magicraft", name); }
    @Override
    public Class getRegistryType() { return null; }

}
