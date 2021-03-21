package me.profelements.dynatech.items.electric.transfer;

import java.util.List;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.libraries.paperlib.PaperLib;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.handlers.ItemHandler;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.cscorelib2.data.PersistentDataAPI;
import me.profelements.dynatech.DynaTech;
import me.profelements.dynatech.DynaTechItems;
import net.md_5.bungee.api.ChatColor;

public class WirelessEnergyPoint extends SlimefunItem implements EnergyNetProvider {

    private static final NamespacedKey WIRELESS_LOCATION_KEY = new NamespacedKey(DynaTech.getInstance(), "wireless-location");
    private final int capacity;
    private final int energyRate;

    @ParametersAreNonnullByDefault
    public WirelessEnergyPoint(Category category, int capacity, int energyRate, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);

        this.capacity = capacity;
        this.energyRate = energyRate;

        addItemHandler(onRightClick(), onBlockPlace(), onBlockBreak());
    }

    @Override
    public int getGeneratedOutput(Location l, Config data) {
        String wirelessBankLocation = BlockStorage.getLocationInfo(l, "wireless-location");
        
        int chargedNeeded = getCapacity() - getCharge(l);

        if(chargedNeeded != 0 && wirelessBankLocation != null) {
            Location wirelessEnergyBank = StringToLocation(wirelessBankLocation);

            if (wirelessEnergyBank != null && BlockStorage.checkID(wirelessEnergyBank).equals(DynaTechItems.WIRELESS_ENERGY_BANK.getItemId())) {
                PaperLib.getChunkAtAsync(wirelessEnergyBank);
                int BankCharge = getCharge(wirelessEnergyBank);
                
                if (BankCharge > chargedNeeded) {
                    if (chargedNeeded > getEnergyRate()) {
                        removeCharge(wirelessEnergyBank, getEnergyRate());
                        return getEnergyRate();
                    }
                    removeCharge(wirelessEnergyBank, chargedNeeded);
                    return chargedNeeded;
                }
                
            }

        }
        return 0;
    }

    private ItemHandler onRightClick() {
        return new ItemUseHandler() {

            @Override
            public void onRightClick(PlayerRightClickEvent event) {

                Optional<Block> blockClicked = event.getClickedBlock();           
                Optional<SlimefunItem> sfBlockClicked = event.getSlimefunBlock();
                if (blockClicked.isPresent() && sfBlockClicked.isPresent()) {
                    Location blockLoc = blockClicked.get().getLocation();
                    SlimefunItem sfBlock = sfBlockClicked.get();
                    ItemStack item = event.getItem();


                    if (sfBlock != null && sfBlock.getId().equals(DynaTechItems.WIRELESS_ENERGY_BANK.getItemId()) && blockLoc != null) {
                        event.cancel();
                        ItemMeta im = item.getItemMeta();
                        String locationString = LocationToString(blockLoc);
                        
                        PersistentDataAPI.setString(im, WIRELESS_LOCATION_KEY, locationString);
                        item.setItemMeta(im);
                        setItemLore(item, blockLoc);
                    }
                }   
            } 
        };
    }

    private ItemHandler onBlockPlace() {
        return new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(BlockPlaceEvent event) {
                
                
                Location blockLoc = event.getBlockPlaced().getLocation();
                ItemStack item = event.getItemInHand();
                String locationString = PersistentDataAPI.getString(item.getItemMeta(), WIRELESS_LOCATION_KEY);
                
                if (item != null && item.getType() == DynaTechItems.WIRELESS_ENERGY_POINT.getType() && item.hasItemMeta() && locationString != null) {
                    BlockStorage.addBlockInfo(blockLoc, "wireless-location", locationString);
                    
                }   
            }
            
        };
    }

    private ItemHandler onBlockBreak() {
        return new BlockBreakHandler(false, false) {

			@Override
			public void onPlayerBreak(BlockBreakEvent event, ItemStack block, List<ItemStack> drops) {
				BlockStorage.clearBlockInfo(event.getBlock().getLocation());
			}
            
        };
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    public int getEnergyRate() {
        return energyRate;
    }

    private void setItemLore(ItemStack item, Location l) {
        ItemMeta im = item.getItemMeta();
        List<String> lore = im.getLore();
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains("Location: ")) {
                lore.remove(i);
            } 
        }

        lore.add(ChatColor.WHITE + "Location: " + l.getWorld().getName() + " " + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ());

        im.setLore(lore);
        item.setItemMeta(im);
        
    }


    private String LocationToString(Location l) {
        return l.getWorld().getName()+":"+l.getBlockX()+":"+l.getBlockY()+":"+l.getBlockZ();
    }

    private Location StringToLocation (String str) {
            String[] locComponents = str.split(":");
            return new Location(Bukkit.getWorld(locComponents[0]), Double.parseDouble(locComponents[1]), Double.parseDouble(locComponents[2]), Double.parseDouble(locComponents[3]));
    }

}