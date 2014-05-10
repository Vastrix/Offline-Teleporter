package com.gmail.sevrius.OfflineTeleporter;
import java.io.*;
import java.nio.file.Files;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
import org.bukkit.Server;

public class OfflineTeleporter extends JavaPlugin implements Listener{
	private FileConfiguration config = null;
	public FileConfiguration UserD = null;
	
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
		reloadConfig(); //needed or does it autoload?
		config = getConfig();
		
	}
	
	@Override
	public void onDisable(){
		//TODO CLeanup
	}
	
	
	
	
	
	
	@EventHandler
	public void joinEvent(PlayerJoinEvent event){ //check if the player has a user file, if not create one AND TP him to a possible loc!
		String player = event.getPlayer().getName();
		if (!(new File(getDataFolder(),"/Data/"+player+".yml").exists())){
			try{createFile(player);}catch(IOException rr) {getLogger().info("Couldn't close the file");}
			letsConf(player);
			UserD.set("name", player);
			letsSave(player);
			}
		//if (letsConf(player).getString("newPosition.world") != null){ //this somehow screws with the format i think..
			//getLogger().info("Not Null"); //Dbug
			//TODO Add TP to loc
		//}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event){ //put the player's loc in his file
		String player = event.getPlayer().getName();
		Location loc = event.getPlayer().getLocation();	
		letsSet(player, "lastPosition", loc);
	}	
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("otp")){
			if(!(sender instanceof Player)){sender.sendMessage(ChatColor.RED+"You're not a player! :(");}else{
				if(args.length > 1){sender.sendMessage("Too many Arguments!");}else{
					Player player = (Player) sender; //Casting the sender variable to a player type variable (Only possible since CommandSender implements Player!)
					if (!(sender.hasPermission("otp.otp"))||!(sender.isOp())){sender.sendMessage(ChatColor.RED+"you don't have the permission!");}else{
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
							player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT,5,1);
							return true;
						}}}}
			return false;
		}else if(cmd.getName().equalsIgnoreCase("otphere")){
			
		}
		return false;
	}
	
	public void letsConf(String player){// return a FileConfig variable (to play around with)
		File playerFile = new File(getDataFolder(), "/Data/"+player+".yml");
		FileConfiguration playerc = YamlConfiguration.loadConfiguration(playerFile);
		UserD = playerc;
	}
	
	public void letsSave(String player){ //saves the player.yml
		File playerFile = new File(getDataFolder(), "/Data/"+player+".yml");
		try {UserD.save(playerFile);} catch(IOException ex) {getLogger().info("Player file not found! | My fault! :(  l62");}
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
	
	public void createFile(String name) throws IOException{// "throws" is basically a try statement, right? :s | Had too add "throws" cause of l95 and l96 :s
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
			} catch (IOException error) { 				  //Why is this needed since line 80 catches all the exceptions in this method anyway..?
				getLogger().info("I did it! :( (l56-57)");//Like if something in this method throws an IOException it'll fall back to the try loop at l48, no?
			} finally {
				src.close(); 
				os.close();
			}
	}
}
