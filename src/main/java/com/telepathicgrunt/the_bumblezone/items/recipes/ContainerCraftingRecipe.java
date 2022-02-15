package com.telepathicgrunt.the_bumblezone.items.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.telepathicgrunt.the_bumblezone.modinit.BzItems;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraftforge.registries.ForgeRegistryEntry;

public class ContainerCraftingRecipe extends ShapelessRecipe {
    private final String group;
    private final ItemStack recipeOutput;
    private final NonNullList<Ingredient> recipeItems;
    public ContainerCraftingRecipe(ResourceLocation idIn, String groupIn, ItemStack recipeOutputIn, NonNullList<Ingredient> recipeItemsIn) {
        super(idIn, groupIn, recipeOutputIn, recipeItemsIn);
        this.group = groupIn;
        this.recipeOutput = recipeOutputIn;
        this.recipeItems = recipeItemsIn;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BzItems.CONTAINER_CRAFTING_RECIPE.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return recipeItems;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        NonNullList<ItemStack> remainingInv = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
        int containerOutput = recipeOutput.hasContainerItem() ? recipeOutput.getCount() : 0;

        for(int i = 0; i < remainingInv.size(); ++i) {
            ItemStack item = inv.getItem(i);
            if (item.hasContainerItem()) {
                if(containerOutput > 0 &&
                    (recipeOutput.getItem() == item.getContainerItem().getItem() ||
                    recipeOutput.getContainerItem().getItem() == item.getItem() ||
                    recipeOutput.getContainerItem().getItem() == item.getContainerItem().getItem()))
                {
                    containerOutput--;
                }
                else {
                    remainingInv.set(i, item.getContainerItem().getItem().getDefaultInstance());
                }
            }
        }

        return remainingInv;
    }


    public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<ContainerCraftingRecipe> {
        @Override
        public ContainerCraftingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            String s = GsonHelper.getAsString(json, "group", "");
            NonNullList<Ingredient> DefaultedList = getIngredients(GsonHelper.getAsJsonArray(json, "ingredients"));
            if (DefaultedList.isEmpty()) {
                throw new JsonParseException("No ingredients for shapeless recipe");
            }
            else {
                ItemStack itemstack = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
                return new ContainerCraftingRecipe(recipeId, s, itemstack, DefaultedList);
            }
        }

        private static NonNullList<Ingredient> getIngredients(JsonArray jsonElements) {
            NonNullList<Ingredient> defaultedList = NonNullList.create();

            for (int i = 0; i < jsonElements.size(); ++i) {
                Ingredient ingredient = Ingredient.fromJson(jsonElements.get(i));
                if (!ingredient.isEmpty()) {
                    defaultedList.add(ingredient);
                }
            }

            return defaultedList;
        }

        @Override
        public ContainerCraftingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            String s = buffer.readUtf(32767);
            int i = buffer.readVarInt();
            NonNullList<Ingredient> defaultedList = NonNullList.withSize(i, Ingredient.EMPTY);

            for (int j = 0; j < defaultedList.size(); ++j) {
                defaultedList.set(j, Ingredient.fromNetwork(buffer));
            }

            ItemStack itemstack = buffer.readItem();
            return new ContainerCraftingRecipe(recipeId, s, itemstack, defaultedList);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ContainerCraftingRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeVarInt(recipe.recipeItems.size());

            for (Ingredient ingredient : recipe.recipeItems) {
                ingredient.toNetwork(buffer);
            }

            buffer.writeItem(recipe.recipeOutput);
        }
    }
}