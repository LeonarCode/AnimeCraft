package com.leonard.animecraft.ability;

public enum PowerStyle {
    SHINOBI("Shinobi Arts", "Chakra"),
    BREATHING("Breathing Styles", "Stamina"),
    REAPER("Soul Reaper", "Spirit Pressure"),
    GUILD_MAGIC("Guild Magic", "Mana"),
    TITAN("Titan Shifter", "Titan Power");

    private final String displayName;
    private final String energyName;

    PowerStyle(String displayName, String energyName) {
        this.displayName = displayName;
        this.energyName = energyName;
    }

    public String displayName() {
        return displayName;
    }

    public String energyName() {
        return energyName;
    }
}
