package me.lidan.cavecrawlers.quest;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class DialogueQuest extends Quest{
    String[] dialogues;
    long ticks;

    public DialogueQuest(String[] dialogues, long ticks){
        if(dialogues == null){
            throw new IllegalArgumentException("Dialogues array cannot be null");
        }
        this.dialogues = dialogues;
        this.ticks = ticks;
    }
    public DialogueQuest(String dialogue, long ticks){
        if(dialogue == null){
            throw new IllegalArgumentException("Dialogues array cannot be null");
        }
        this(new String[]{dialogue}, ticks);
    }
    public DialogueQuest(String[] dialogues){
        this(dialogues, 10L);
    }
    public DialogueQuest(String dialogue){
        this(new String[]{dialogue});
    }

    @Override
    public void startQuest(UUID uuid) {
        /*check if the uuid valid*/
        final Player player  = Bukkit.getPlayer(uuid);
        if(player == null){
            return;
        }

        /*will send the dialogues with [ticks] delay*/
        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                /*check if in the end*/
                if (i == dialogues.length){
                    finishQuest(uuid);
                    cancel();
                }

                /*check if finishQuest was called*/
                if(finishQuestMap.getOrDefault(uuid, false)){
                    cancel();
                }

                /*send the string i in the dialogues*/
                player.sendMessage(dialogues[i]);
                i += 1;
            }

            /*will remove from the finishQuestMap the player*/
            @Override
            public synchronized void cancel() throws IllegalStateException {
                super.cancel();
                finishQuestMap.remove(uuid);
            }
        }.runTaskLater(CaveCrawlers.getInstance(), ticks);
    }
}
