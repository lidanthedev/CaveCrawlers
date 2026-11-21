package me.lidan.cavecrawlers.items.abilities;

import lombok.Getter;

public enum DisplayAction {
    RIGHT_CLICK("RIGHT CLICK"),
    LEFT_CLICK("LEFT CLICK"),
    SHIFT_RIGHT_CLICK("SNEAK RIGHT CLICK"),
    SHIFT_LEFT_CLICK("SNEAK LEFT CLICK"),
    ANY_CLICK("CLICK");

    @Getter
    private final String value;

    DisplayAction(String value) {
        this.value = value;
    }
}
