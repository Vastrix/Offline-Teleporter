package com.gmail.sevrius.offlineteleporter;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class OfflineTeleporter extends JavaPlugin implements Listener {
    public FileConfiguration config = null;
    public FileConfiguration UserD = null;
    public BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
    public Player pjoin;
    public HashMap<String, String> uuids = new HashMap<>(); //playername, UUID

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        if (!(new File(getDataFolder(), "config.yml").exists())) { //Check for our config
            getLogger().info("Creating Config.yml file..");
            saveDefaultConfig();
            reloadConfig();
        } else if ((getConfig().getDouble("Version")) != (Double.parseDouble((this.getDescription().getVersion())))) {
            Double vers = getConfig().getDouble("Version");
            getConfig().set("Version", Double.parseDouble(this.getDescription().getVersion()));

            if (vers <= 0.2) {//Q: Good way to add new entries?
                getLogger().info("Changing map structure..");
                File[] folder = new File(getDataFolder(), "/Data/").listFiles();
                if (folder.length >= 1) {
                    for (File i : folder) {
                        i.delete();
                    }
                }
            }
            this.saveConfig();
        }

        if (!(new File(getDataFolder(), "Data").exists())) { //check for our Data Folder
            getLogger().info("Creating Data Folder..");
            new File(getDataFolder(), "Data").mkdir();
        }
        reloadConfig(); //Q:needed or does it autoload?

        File[] folder = new File(getDataFolder(), "/Data/").listFiles();
        getLogger().info("Mapping UUID's..");//
        for (File i : folder) {
            if (i.isFile()) {
                String name = i.getName().substring(0, i.getName().lastIndexOf("."));
                letsConf(name);
                if (UserD.getString("UUID") != null) { //Putting stuff in the uuid hashmap
                    uuids.put(name, UserD.getString("UUID"));
                } else {
                    getLogger().info("error: No UUID in the userfile: " + i.getName());
                }
                UserD = null;
            }
        }
        getLogger().info("Finished Mapping! (" + uuids.size() + ")");
    }

    @Override
    public void onDisable() {
        //Cleanup, But since i don't make a mess.. :D
    }


    @EventHandler
    public void joinEvent(PlayerJoinEvent event) { //check if the player has a user file, if not create one AND TP him to a possible loc!
        String player = event.getPlayer().getName(); //make a string variable from the player name
        pjoin = getServer().getPlayer(event.getPlayer().getUniqueId()); //make a PLAYER variable from the player's UUID

        if (!(uuids.containsValue(pjoin.getUniqueId().toString()))) { //Checks if there's already a player file, if not..
            try {
                createFile(player);
            } catch (IOException rr) {
                getLogger().info("Couldn't close the file");
            }
            letsConf(player);//Sets UserD
            UserD.set("UUID", pjoin.getUniqueId().toString()); //Saving the UUID
            letsSave(player);
            uuids.put(pjoin.getName(), pjoin.getUniqueId().toString()); //Updating Hashmap
        } else {
            String key = getkey(pjoin.getPlayer().getUniqueId().toString());
            if (!(key.equals(pjoin.getName()))) { //If his name has changed..
                File pfile = new File(getDataFolder(), "/Data/" + key + ".yml");
                File nname = new File(pjoin.getName());
                pfile.renameTo(nname); //Changing the player file name to the most recent name. TODO Test
                uuids.remove(key); //Updating uuid hashmap
                uuids.put(pjoin.getName(), pjoin.getUniqueId().toString());
            }
        }
        letsConf(player);
        if (UserD.getString("newPosition.world") != null) { //If world is set then someone used otphere
            Location loc = new Location(getServer().getWorld("world"), 8, 64, 8);
            loc.setWorld(Bukkit.getServer().getWorld(UserD.getString("newPosition.world")));
            loc.setX(UserD.getDouble("newPosition.x"));
            loc.setY(UserD.getDouble("newPosition.y"));
            loc.setZ(UserD.getDouble("newPosition.z"));
            loc.setYaw((float) UserD.getDouble("newPosition.yaw")); //Casts it to a Float
            loc.setPitch((float) UserD.getDouble("newPosition.pitch"));
            pjoin.teleport(loc, TeleportCause.PLUGIN);

            scheduler.scheduleSyncDelayedTask(this, new Scheduler(this, pjoin.getName(), UserD.getString("newPosition.world"), UserD.getString("newPosition.setter"), UserD.getString("message"), String.valueOf(UserD.getInt("newPosition.x")), String.valueOf(UserD.getInt("newPosition.y")), String.valueOf(UserD.getInt("newPosition.z"))), 20 * 2);
            letsSave(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String player = event.getPlayer().getName();
        Location loc = event.getPlayer().getLocation();
        letsSet(player, "lastPosition", loc); //put the player's loc in his file
        letsConf(player);
        if (UserD.getString("newPosition.world") != null) { //nullifies the newPosition vars (So they can't use otpback on next login)
            UserD.set("newPosition.world", null);
            UserD.set("newPosition.x", null);
            UserD.set("newPosition.y", null);
            UserD.set("newPosition.z", null);
            UserD.set("newPosition.yaw", null);
            UserD.set("newPosition.pitch", null);
            UserD.set("newPosition.setter", null);
            UserD.set("message", null);
            letsSave(player);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("otp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You're not a player! :(");
            } else { //checks if the caller is a player (or the console)
                if (args.length > 1) {
                    sender.sendMessage(ChatColor.RED + "Too many Arguments!");
                } else {
                    if (args.length < 1) {
                        sender.sendMessage(ChatColor.RED + "Not Enough Arguments");
                    } else {
                        Player player = (Player) sender; //Casting the sender variable to a player type variable (Only possible since CommandSender implements Player!)
                        if (!(sender.hasPermission("otp.otp")) && !(sender.isOp())) {
                            sender.sendMessage(ChatColor.RED + "you don't have the permission!");
                        } else { // Do note, if we were to specify a permission in the plugin.yml file, bukkit checks this for us.
                            if (casein(args[0]) == null) {
                                player.sendMessage(ChatColor.RED + "Are you sure you typed the name correct? (" + args[0] + ")");
                            } else {
                                letsConf(casein(args[0]));
                                Location loc = new Location(player.getWorld(), 8, 64, 8);
                                loc.setWorld(Bukkit.getServer().getWorld(UserD.getString("lastPosition.world")));
                                loc.setX(UserD.getDouble("lastPosition.x"));
                                loc.setY(UserD.getDouble("lastPosition.y"));
                                loc.setZ(UserD.getDouble("lastPosition.z"));
                                loc.setYaw((float) UserD.getDouble("lastPosition.yaw")); //Casts it to a Float
                                loc.setPitch((float) UserD.getDouble("lastPosition.pitch"));
                                UserD = null;
                                player.teleport(loc, TeleportCause.COMMAND);
                                player.sendMessage(ChatColor.GREEN + "Successfully teleported to " + args[0] + "'s logout location!");
                                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMEN_TELEPORT, 5, 1); //Can't add the ender effect, Blaming Bukkit ;(
                                return true;
                            }
                        }
                    }
                }
            }
        } else if (cmd.getName().equalsIgnoreCase("otphere")) {//TODO add Already-set Check!
            String caseins;
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You need to be a player to do this..");
            } else {
                if (!(sender.hasPermission("otp.otphere")) && !(sender.isOp())) {
                    sender.sendMessage(ChatColor.RED + "You don't have the right permission: " + ChatColor.DARK_GRAY + "otp.otphere");
                } else {
                    if (args.length < 1) {
                        sender.sendMessage(ChatColor.RED + "I'm gonna need a player name..");
                    } else {
                        if (casein(args[0]) == null) {
                            sender.sendMessage(ChatColor.RED + "Can't find the player file, Did " + args[0] + " ever login before?");
                        } else {//Checks if the player exists on the server
                            caseins = casein(args[0]);//the real player name (cased and everything)
                            if ((getServer().getPlayer(caseins) != null)) {
                                sender.sendMessage(ChatColor.RED + "Looks like " + caseins + " is still online, Try using the regular /tphere command.");
                            } else {//checks if the player is online
                                if ((sender.getName().equals(caseins))) {
                                    sender.sendMessage(ChatColor.RED + "You cannot set your own login position!");
                                } else {//checks if people are doing weird things :p


                                    Player player = (Player) sender;
                                    Location loc = player.getLocation();
                                    letsSet(caseins, "newPosition", loc);
                                    letsConf(caseins);
                                    UserD.set("newPosition.setter", player.getName());
                                    letsSave(caseins);
                                    sender.sendMessage(ChatColor.GREEN + "Successfully set " + caseins + "'s login location!");
                                    if (!(args.length > 1)) {
                                        return true;
                                    } else {//Checks if there is a msg set
                                        String[] msgl = Arrays.copyOfRange(args, 1, args.length);//Sets everything after the second arg in the msgl array
                                        String msg = "";
                                        for (String i : msgl) {//putting everything from the array in a string
                                            msg = msg + " " + i;//TODO Fix the space at the start
                                        }
                                        letsConf(caseins);
                                        UserD.set("message", msg);
                                        letsSave(caseins);
                                        sender.sendMessage(ChatColor.GREEN + "With the following message:");
                                        sender.sendMessage(msg);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (cmd.getName().equalsIgnoreCase("otpback")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Yea..I'm gonna need you to be a player! :P");
            } else {
                if (!(sender.hasPermission("otp.otpback")) && !(sender.isOp())) {
                    sender.sendMessage(ChatColor.RED + "Insufficient Permissions!");
                } else {
                    if (args.length > 0) {
                        sender.sendMessage(ChatColor.RED + "Too many arguments! (need none)");
                    } else {
                        letsConf(sender.getName());
                        if (UserD.getString("newPosition.world") == null) {
                            sender.sendMessage(ChatColor.RED + "You can only use this once(before logging out)");
                        } else {

                            Player player = (Player) sender;
                            Location loc = new Location(player.getWorld(), 8, 64, 8);
                            loc.setWorld(getServer().getWorld(UserD.getString("lastPosition.world")));
                            loc.setX(UserD.getDouble("lastPosition.x"));
                            loc.setY(UserD.getDouble("lastPosition.y"));
                            loc.setZ(UserD.getDouble("lastPosition.z"));
                            loc.setYaw((float) UserD.getDouble("lastPosition.yaw"));
                            loc.setPitch((float) UserD.getDouble("lastPosition.pitch"));
                            player.teleport(loc, TeleportCause.COMMAND);
                            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMEN_TELEPORT, 5, 1);
                            player.sendMessage(ChatColor.GREEN + "You Successfully teleported to your previous logout location!");
                            UserD.set("newPosition.world", null);
                            UserD.set("newPosition.x", null);
                            UserD.set("newPosition.y", null);
                            UserD.set("newPosition.z", null);
                            UserD.set("newPosition.yaw", null);
                            UserD.set("newPosition.pitch", null);
                            UserD.set("newPosition.setter", null);
                            UserD.set("message", null);
                            letsSave(player.getName());
                        }
                    }
                }
            }
        } else if (cmd.getName().equalsIgnoreCase("cookie")) {
            String caseins;
            if (args.length > 1) {
                sender.sendMessage(ChatColor.RED + "Too many arguments..");
            } else {
                if (args.length < 1) {
                    sender.sendMessage(ChatColor.RED + "Not Enough Arguments(Need a player name!)");
                } else {
                    if (!(sender.hasPermission("otp.cookie")) && !(sender.isOp())) {
                        sender.sendMessage(ChatColor.RED + "You Don't have the Required Perms..");
                    } else {
                        if (casein(args[0]) == null) {
                            sender.sendMessage(ChatColor.RED + "Any chance that you misstyped the name? " + args[0]);
                        } else {
                            caseins = casein(args[0]);
                            if (sender.getName().equals(caseins)) {
                                sender.sendMessage(ChatColor.RED + "You can't give cookies to yourself, Do you have any idea how sad this is? :o");
                            } else {
                                if (Bukkit.getServer().getPlayer(caseins) == null) {
                                    sender.sendMessage(ChatColor.RED + "Yea.. I'm afraid he's gonna need to be online, Or you misspelled the name?");
                                } else {
                                    getServer().getPlayer(caseins).getInventory().addItem(new ItemStack(Material.COOKIE, 1));
                                    getServer().broadcastMessage(ChatColor.DARK_GRAY + sender.getName() + " Gives " + caseins + " A " + ChatColor.GOLD + "Cookie" + ChatColor.DARK_GRAY + "!");
                                    return true;
                                    //TODO add a cooldown?
                                }
                            }
                        }
                    }
                }
            }
            // Following the coding style here...
        } else if (cmd.getName().equalsIgnoreCase("otpspawn")) {
            String caseins;
            if (args.length < 1)
                return false;
            if (casein(args[0]) == null) {
                sender.sendMessage(ChatColor.RED + "Can't find the player file, Did " + args[0] + " ever login before?");
            } else {
                caseins = casein(args[0]);
                if ((getServer().getPlayer(caseins) != null)) {
                    sender.sendMessage(ChatColor.RED + "Looks like " + caseins + " is still online, Try using the regular /tphere command.");
                } else {
                    try {
                        String changeOwner = "console";
                        if (sender instanceof Player) {
                            changeOwner = sender.getName();
                            if (!(sender.hasPermission("otp.otpspawn")) && !(sender.isOp()))
                                return false;
                        }
                        Location loc = getServer().getWorlds().get(0).getSpawnLocation();
                        letsSet(caseins, "newPosition", loc);
                        letsConf(caseins);
                        UserD.set("newPosition.setter", changeOwner);
                        letsSave(caseins);
                        sender.sendMessage(ChatColor.GREEN + "Successfully set " + caseins + "'s login location!");
                    } catch (Exception e) {
                        sender.sendMessage("Unable to OTP to spawn - exception caught");
                    }

                }
            }
        }


        return true; //returning true instead of false to prevent bukkit from showing the usage info TODO Add our own return value!
    }


    public String getkey(String value) {
        for (Map.Entry<String, String> entry : uuids.entrySet()) { //This will loop through the entire map |Tivec helped me understand this, entrySet() = (pseudcode)http://puu.sh/9c37T/5b3d148e2d.png
            if (value.equals(entry.getValue())) {
                return entry.getKey().toString();
            }
        }
        return null;
    }

    /**
     * So, this method allows the input "adam" to return with the player file "Adam"(caps)
     * however!, if there are two player files (Adam and adam) than the file "adam" will return!
     *
     * @param arg the name the user inputs
     * @return the real player name (with caps) or null if the player name isn't found
     */
    public String casein(String arg) {
        int ir = 0;
        Map.Entry<String, String> target = null;

        for (Map.Entry<String, String> entry : uuids.entrySet()) {
            if (entry.getKey().equals(arg)) {
                return entry.getKey();
            } else if (entry.getKey().equalsIgnoreCase(arg)) {
                ir++;
                target = entry;
            }
        }
        if (ir == 1 && target != null) {
            return target.getKey();
        } else if (ir < 1) {
            return null;
        }
        return null;
    }

    public void letsConf(String player) {// return a FileConfig variable (to play around with)
        File playerFile = new File(getDataFolder(), "/Data/" + player + ".yml");
        FileConfiguration playerc = YamlConfiguration.loadConfiguration(playerFile);
        UserD = playerc;
    }

    public void letsSave(String player) { //saves the player.yml
        File playerFile = new File(getDataFolder(), "/Data/" + player + ".yml");
        try {
            UserD.save(playerFile);
        } catch (IOException ex) {
            getLogger().info("Player file not found! | My fault! :(  l135");
        }
        UserD = null;
    }

    public void letsSet(String player, String list, Location loc) {
        letsConf(player);
        UserD.set(list + "." + "world", loc.getWorld().getName());
        UserD.set(list + "." + "x", loc.getX());
        UserD.set(list + "." + "y", loc.getY()); //Wanted to use reflections but Tivec said it'd be a bad idea :P
        UserD.set(list + "." + "z", loc.getZ());
        UserD.set(list + "." + "yaw", loc.getYaw());
        UserD.set(list + "." + "pitch", loc.getPitch());
        letsSave(player);
    }

    public void createFile(String name) throws IOException {//Q:"throws" is basically a try statement, right? :s | Had too add "throws" :s
        InputStream src = this.getResource("user.yml");
        OutputStream os = null;
        int readb;
        byte[] buffer = new byte[4096];

        try {
            getLogger().info("Creating new user file for " + name);
            os = new FileOutputStream(new File(getDataFolder(), "/Data/" + name + ".yml"));
            while ((readb = src.read(buffer)) > 0) {
                os.write(buffer, 0, readb);
            }
        } catch (IOException error) {                  //Q:Why is this needed since line 222 catches all the exceptions in this method anyway..?
            getLogger().info("I did it! :( (l56-57)");//Q:Like if something in this method throws an IOException it'll fall back to the try loop at l67, no?
        } finally {
            src.close();
            os.close();
        }
    }
}
