package me.lidan.cavecrawlers.prompt;

public class PromptException extends RuntimeException {
    public PromptException(String message) {
        super(message);
    }

    public PromptException(String message, Throwable cause) {
        super(message, cause);
    }

    public PromptException(Throwable cause) {
        super(cause);
    }
}
