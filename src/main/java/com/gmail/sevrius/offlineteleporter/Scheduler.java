package com.gmail.sevrius.offlineteleporter;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Scheduler implements Runnable {
    String world;
    String setter;
    String msg;
    Player pjoin;
    String x;
    String y;
    String z;
    OfflineTeleporter plugin;


    public Scheduler(OfflineTeleporter pl, String player, String worldd, String setterr, String msgg, String xx, String yy, String zz) {//Using a bunch of vars instead of just requesting the UserD because threads are easy to screw up :s
        plugin = pl;
        pjoin = plugin.getServer().getPlayer(player);
        world = worldd;
        setter = setterr;
        if (msgg != null) {
            msg = msgg;
        } else {
            msg = " None";
        }
        x = xx;
        y = yy;
        z = zz;
    }


    @Override
    public void run() {//No params accepted, so we go with vars
        pjoin.sendMessage(ChatColor.GRAY + "You were teleported!");
        pjoin.sendMessage(ChatColor.DARK_PURPLE + "-----------------------------------------------------");
        pjoin.sendMessage(ChatColor.DARK_GRAY + "Teleporter: " + setter);
        pjoin.sendMessage(ChatColor.DARK_GRAY + "   Message:" + msg);
        pjoin.sendMessage(ChatColor.DARK_GRAY + "  Prev Pos: " + world + ", " + x + ", " + y + ", " + z);
        if ((pjoin.hasPermission("otp.otpback")) || (pjoin.isOp())) {
            pjoin.sendMessage(ChatColor.DARK_GRAY + "You can use /otpback to go back to where you were!");
        }
    }
}