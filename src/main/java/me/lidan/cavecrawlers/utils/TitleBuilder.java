package me.lidan.cavecrawlers.utils;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TitleBuilder class to build titles easily
 * How to use:
 * Make a new instance of the TitleBuilder class
 * Set the players
 * Set the title
 * Set the subtitle
 * Set the fade in, stay, fade out
 * Show the title
 * The title will be shown to the players
 * most steps are optional
 */
@Getter
public class TitleBuilder {
    private Set<Player> allPlayers;
    private String title;
    private String subtitle;
    private int fadeIn;
    private int stay;
    private int fadeOut;

    public TitleBuilder(){
        allPlayers = new HashSet<>();
        title = "";
        subtitle = "";
        fadeIn = 0;
        stay = 1;
        fadeOut = 0;
    }

    public TitleBuilder setPlayers(Set<Player> players){
        allPlayers = players;
        return this;
    }
    public TitleBuilder setPlayers(List<Player> players){
        allPlayers = new HashSet<>(players);
        return this;
    }
    public TitleBuilder setPlayers(Player player){
        allPlayers.clear();
        allPlayers.add(player);
        return this;
    }
    public TitleBuilder setPlayers(Player... players){
        allPlayers = new HashSet<>(List.of(players));
        return this;
    }
    public TitleBuilder addPlayer(Player player){
        allPlayers.add(player);
        return this;
    }

    public TitleBuilder addPlayers(Player... player){
        allPlayers.addAll(List.of(player));
        return this;
    }
    public TitleBuilder addPlayers(List<Player> players){
        allPlayers.addAll(players);
        return this;
    }
    public TitleBuilder addPlayers(Set<Player> players){
        allPlayers.addAll(players);
        return this;
    }


    public TitleBuilder setTitle(String title){
        this.title = title;
        return this;
    }
    public TitleBuilder addToTitle(String title){
        this.title += title;
        return this;
    }

    public TitleBuilder setSubtitle(String subtitle){
        this.subtitle = subtitle;
        return this;
    }
    public TitleBuilder addToSubtitle(String subtitle){
        this.subtitle += subtitle;
        return this;
    }

    public TitleBuilder setFadeIn(int fadeIn){
        this.fadeIn = fadeIn;
        return this;
    }
    public TitleBuilder addFadeIn(int fadeIn){
        this.fadeIn += fadeIn;
        return this;
    }

    public TitleBuilder setStay(int stay){
        this.stay = stay;
        return this;
    }
    public TitleBuilder addStay(int stay){
        this.stay += stay;
        return this;
    }

    public TitleBuilder setFadeOut(int fadeOut){
        this.fadeOut = fadeOut;
        return this;
    }
    public TitleBuilder addFadeOut(int fadeOut){
        this.fadeOut += fadeOut;
        return this;
    }

    public TitleBuilder setTime(int fadeIn, int stay, int fadeOut){
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
        return this;
    }

    public TitleBuilder addTime(int fadeIn, int stay, int fadeOut){
        this.fadeIn += fadeIn;
        this.stay += stay;
        this.fadeOut += fadeOut;
        return this;
    }


    public void show(){
        Duration fadeInDuration = fadeIn == 0 ? Duration.ZERO : Duration.ofSeconds(fadeIn);
        Duration stayDuration = stay == 0 ? Duration.ZERO : Duration.ofSeconds(stay);;
        Duration fadeOutDuration = fadeOut == 0 ? Duration.ZERO : Duration.ofSeconds(fadeOut);;
        for(Player player: allPlayers){
            player.sendTitlePart(TitlePart.TITLE, Component.text(title));
            player.sendTitlePart(TitlePart.SUBTITLE, Component.text(subtitle));
            player.sendTitlePart(TitlePart.TIMES, Title.Times.times(fadeInDuration, stayDuration, fadeOutDuration));
        }
    }
}
