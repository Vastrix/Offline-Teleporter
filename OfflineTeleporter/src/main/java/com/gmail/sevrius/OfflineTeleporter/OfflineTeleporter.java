package com.gmail.sevrius.OfflineTeleporter;
import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class OfflineTeleporter extends JavaPlugin implements Listener{
	public FileConfiguration config = null;
	public FileConfiguration UserD = null;
	public BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
	public Player pjoin;
	
	@Override
	public void onEnable(){
		getServer().getPluginManager().registerEvents(this, this);
		if(!new File(getDataFolder(), "config.yml").exists()){ //Check for our config
			getLogger().info("Creating Config.yml file..");
			saveDefaultConfig();
			}
		if(!new File(getDataFolder(), "Data").exists()){ //check for our Data Folder
			getLogger().info("Creating Data Folder..");
			new File(getDataFolder(), "Data").mkdir();
			}
		reloadConfig(); //Q:needed or does it autoload?
		config = getConfig();
		
	}
	
	@Override
	public void onDisable(){
		//TODO CLeanup But since i don't make a mess.. :D
	}
	
	
	
	
	
	
	@EventHandler
	public void joinEvent(PlayerJoinEvent event){ //check if the player has a user file, if not create one AND TP him to a possible loc!
		String player = event.getPlayer().getName();
		pjoin = getServer().getPlayer(player);
		if (!(new File(getDataFolder(),"/Data/"+player+".yml").exists())){
			try{createFile(player);}catch(IOException rr) {getLogger().info("Couldn't close the file");}
			letsConf(player);//Sets UserD
			UserD.set("name", player);
			letsSave(player);
		}
		letsConf(player);
		if (UserD.getString("newPosition.world") != null){
			Location loc = new Location(getServer().getWorld("world"),8,64,8);
			loc.setWorld(Bukkit.getServer().getWorld(UserD.getString("newPosition.world")));
			loc.setX(UserD.getDouble("newPosition.x"));
			loc.setY(UserD.getDouble("newPosition.y"));
			loc.setZ(UserD.getDouble("newPosition.z"));
			loc.setYaw((float) UserD.getDouble("newPosition.yaw")); //Casts it to a Float
			loc.setPitch((float)UserD.getDouble("newPosition.pitch"));
			pjoin.teleport(loc,TeleportCause.PLUGIN);
			
			scheduler.scheduleSyncDelayedTask(this, new Scheduler(this, pjoin.getName(), UserD.getString("newPosition.world"), UserD.getString("newPosition.setter"), UserD.getString("message"), String.valueOf(UserD.getInt("newPosition.x")), String.valueOf(UserD.getInt("newPosition.y")), String.valueOf(UserD.getInt("newPosition.z"))), 20*2);
			letsSave(player);
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event){ //put the player's loc in his file
		String player = event.getPlayer().getName();
		Location loc = event.getPlayer().getLocation();	
		letsSet(player, "lastPosition", loc);
		letsConf(player);
		if (UserD.getString("newPosition.world") != null){
			UserD.set("newPosition.world",null);
			UserD.set("newPosition.x",null);
			UserD.set("newPosition.y",null);
			UserD.set("newPosition.z",null);
			UserD.set("newPosition.yaw",null);
			UserD.set("newPosition.pitch",null);
			UserD.set("newPosition.setter",null);
			UserD.set("message",null);
			letsSave(player);
		}
	}	
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("otp")){
			if(!(sender instanceof Player)){sender.sendMessage(ChatColor.RED+"You're not a player! :(");}else{
				if(args.length > 1){sender.sendMessage(ChatColor.RED+"Too many Arguments!");}else{
					if (args.length <1){sender.sendMessage(ChatColor.RED+"Not Enough Arguments");}else{
						Player player = (Player) sender; //Casting the sender variable to a player type variable (Only possible since CommandSender implements Player!)
							if (!(sender.hasPermission("otp.otp")) && !(sender.isOp())){sender.sendMessage(ChatColor.RED+"you don't have the permission!");}else{ // Do note, if we were to specify a permission in the plugin.yml file, bukkit checks this for us.
								if(!(new File(getDataFolder(),"/Data/"+args[0]+".yml").exists())){player.sendMessage(ChatColor.RED+"Are you sure you typed the name correct? ("+args[0]+")");}else{
									
									letsConf(args[0]);
									Location loc = new Location(player.getWorld(), 8,64,8);
									loc.setWorld(Bukkit.getServer().getWorld(UserD.getString("lastPosition.world")));
									loc.setX(UserD.getDouble("lastPosition.x"));
									loc.setY(UserD.getDouble("lastPosition.y"));
									loc.setZ(UserD.getDouble("lastPosition.z"));
									loc.setYaw((float) UserD.getDouble("lastPosition.yaw")); //Casts it to a Float
									loc.setPitch((float)UserD.getDouble("lastPosition.pitch"));
									UserD = null;
									player.teleport(loc, TeleportCause.COMMAND);
									player.sendMessage(ChatColor.GREEN+"Successfully teleported to "+args[0]+"'s logout location!");
									player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT,5,1); //Can't add the ender effect, Blaming Bukkit ;(
									return true;
								}}}}}
			return false;
		}else if(cmd.getName().equalsIgnoreCase("otphere")){//TODO add Already-set Check!
			if (!(sender instanceof Player)){sender.sendMessage(ChatColor.RED+"You need to be a player to do this..");}else{
				if (!(sender.hasPermission("otp.otphere")) && !(sender.isOp())){sender.sendMessage(ChatColor.RED+"You don't have the right permission: "+ChatColor.DARK_GRAY+"otp.otphere");}else{
					if (args.length < 1){sender.sendMessage(ChatColor.RED+"I'm gonna need a player name..");}else{
						if ((getServer().getPlayer(args[0]) != null) && !(args[0].equals("_Vastrix_"))){sender.sendMessage(ChatColor.RED+"Looks like "+args[0]+" is still online, Try using the regular /tp command.");}else{
							if ((sender.getName().equals(args[0])) && !(args[0].equals("_Vastrix_"))){sender.sendMessage(ChatColor.RED+"You cannot set your own login position!");}else{//That last requirement is for testing :)
								if (!(new File(getDataFolder(),"/Data/"+args[0]+".yml").exists())){sender.sendMessage(ChatColor.RED+"Can't find the player file, Did "+args[0]+" ever login before?");}else{
									
									Player player = (Player) sender;
									Location loc = player.getLocation();
									letsSet(args[0],"newPosition",loc);
									letsConf(args[0]); UserD.set("newPosition.setter", player.getName()); letsSave(args[0]);
									sender.sendMessage(ChatColor.GREEN+"Successfully set "+args[0]+"'s login location!");
									if (!(args.length > 1)){return true;}else{
										String[] msgl = Arrays.copyOfRange(args, 1, args.length);
										String msg = "";
										for(String i: msgl){
											msg = msg +" "+i;//TODO Fix the space at the start
										}
										letsConf(args[0]); UserD.set("message",msg); letsSave(args[0]);
										sender.sendMessage(ChatColor.GREEN+"With the following message:");
										sender.sendMessage(msg);
										return true;
									}}}}}}}
		}else if (cmd.getName().equalsIgnoreCase("otpback")){
			if (!(sender instanceof Player)){sender.sendMessage(ChatColor.RED+"Yea..I'm gonna need you to be a player! :P");}else{
				if(!(sender.hasPermission("otp.otpback")) && !(sender.isOp())){sender.sendMessage(ChatColor.RED+"Insufficient Permissions!");}else{
					if (args.length > 0){sender.sendMessage(ChatColor.RED+"Too many arguments! (need none)");}else{
						letsConf(sender.getName());
						if (UserD.getString("newPosition.world") == null){sender.sendMessage(ChatColor.RED+"You can only use this once(before logging out)");}else{
							
							Player player = (Player) sender;
							Location loc = new Location(player.getWorld(),8,64,8);
							loc.setWorld(getServer().getWorld(UserD.getString("lastPosition.world")));
							loc.setX(UserD.getDouble("lastPosition.x"));
							loc.setY(UserD.getDouble("lastPosition.y"));
							loc.setZ(UserD.getDouble("lastPosition.z"));
							loc.setYaw((float) UserD.getDouble("lastPosition.yaw"));
							loc.setPitch((float)UserD.getDouble("lastPosition.pitch"));
							player.teleport(loc,TeleportCause.COMMAND);
							player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT,5,1);
							player.sendMessage(ChatColor.GREEN+"You Successfully teleported to your previous logout location!");
							UserD.set("newPosition.world",null);
							UserD.set("newPosition.x",null);
							UserD.set("newPosition.y",null);
							UserD.set("newPosition.z",null);
							UserD.set("newPosition.yaw",null);
							UserD.set("newPosition.pitch",null);
							UserD.set("newPosition.setter",null);
							UserD.set("message",null);
							letsSave(player.getName());
			}}}}
		}else if (cmd.getName().equalsIgnoreCase("cookie")){
			if (args.length > 1){sender.sendMessage(ChatColor.RED+"Too many arguments..");}else{
				if (args.length < 1){sender.sendMessage(ChatColor.RED+"Not Enough Arguments(Need a player name!)");}else{
					if (!(sender.hasPermission("otp.cookie")) && !(sender.isOp())){sender.sendMessage(ChatColor.RED+"You Don't have the Required Perms..");}else{
						if (sender.getName().equals(args[0])){sender.sendMessage(ChatColor.RED+"You can't give cookies to yourself, Do you have any idea how sad this is? :o");}else{
							if (Bukkit.getServer().getPlayer(args[0])== null){sender.sendMessage(ChatColor.RED+"Yea.. I'm afraid he's gonna need to be online, Or you misspelled the name?");}else{
								getServer().getPlayer(args[0]).getInventory().addItem(new ItemStack(Material.COOKIE,1));
								getServer().broadcastMessage(ChatColor.DARK_GRAY+sender.getName()+" Gives "+getServer().getPlayer(args[0]).getName()+" A "+ChatColor.GOLD+"Cookie"+ChatColor.DARK_GRAY+"!");
								return true;
								//TODO add a cooldown?
							}}}}}
	
		}
		return true; //returning true instead of false to prevent bukkit from showing the usage info TODO Add our own return value!
	}
	public void letsConf(String player){// return a FileConfig variable (to play around with)
		File playerFile = new File(getDataFolder(), "/Data/"+player+".yml");
		FileConfiguration playerc = YamlConfiguration.loadConfiguration(playerFile);
		UserD = playerc;
	}
	
	public void letsSave(String player){ //saves the player.yml
		File playerFile = new File(getDataFolder(), "/Data/"+player+".yml");
		try {UserD.save(playerFile);} catch(IOException ex) {getLogger().info("Player file not found! | My fault! :(  l135");}
		UserD = null;
	}
	
	public void letsSet(String player, String list, Location loc){
		letsConf(player);
			UserD.set(list+"."+"world", loc.getWorld().getName());
			UserD.set(list+"."+"x", loc.getX());
			UserD.set(list+"."+"y", loc.getY()); //Wanted to use reflections but Tivec said it'd be a bad idea :P
			UserD.set(list+"."+"z", loc.getZ());
			UserD.set(list+"."+"yaw", loc.getYaw());
			UserD.set(list+"."+"pitch", loc.getPitch());
		letsSave(player);
	}
	
	public void createFile(String name) throws IOException{//Q:"throws" is basically a try statement, right? :s | Had too add "throws" cause of l95 and l96 :s
		InputStream src = this.getResource("user.yml");
		OutputStream os = null;
		int readb;
		byte[] buffer = new byte[4096];
					
		try{
			getLogger().info("Creating new user file for "+name);
			os = new FileOutputStream(new File(getDataFolder(),"/Data/"+name+".yml"));
			while ((readb = src.read(buffer)) > 0){
				os.write(buffer, 0,readb);
			  	}
			} catch (IOException error) { 				  //Q:Why is this needed since line 80 catches all the exceptions in this method anyway..?
				getLogger().info("I did it! :( (l56-57)");//Q:Like if something in this method throws an IOException it'll fall back to the try loop at l48, no?
			} finally {
				src.close(); 
				os.close();
			}
	}
}
