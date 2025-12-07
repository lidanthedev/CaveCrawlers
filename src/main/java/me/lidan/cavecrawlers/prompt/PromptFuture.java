package me.lidan.cavecrawlers.prompt;

import lombok.Getter;

import java.util.concurrent.CompletableFuture;

@Getter
public class PromptFuture extends CompletableFuture<String> {
    private final String promptTitle;

    public PromptFuture(String promptTitle) {
        this.promptTitle = promptTitle;
    }
}
