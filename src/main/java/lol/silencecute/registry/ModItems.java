package lol.silencecute.registry;

import lol.silencecute.Client;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.Keys.ITEMS, Client.MOD_ID);

    
    public static final RegistryObject<Item> MAGNETIC_BOMB = ITEMS.register(
            "magnetic_bomb", () -> new Item(new Item.Properties().stacksTo(1)));

    
    public static final RegistryObject<Item> LUNA_GRENADE = ITEMS.register(
            "luna_grenade", () -> new Item(new Item.Properties().stacksTo(1)));

    
    public static final RegistryObject<Item> LUNA_SHOCK_ARROW = ITEMS.register(
            "luna_shock_arrow", () -> new Item(new Item.Properties().stacksTo(1)));

    
    public static final RegistryObject<Item> LUNA_COMPOUND_BOW = ITEMS.register(
            "luna_compound_bow", () -> new Item(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
