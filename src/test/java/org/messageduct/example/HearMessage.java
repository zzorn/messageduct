package org.messageduct.example;

import java.awt.*;

/**
 *
 */
public class HearMessage {
    private String speaker;
    private String text;
    private Color color;

    // For serialization
    private HearMessage() {
    }

    public HearMessage(String speaker, String text, Color color) {
        this.speaker = speaker;
        this.text = text;
        this.color = color;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getText() {
        return text;
    }

    public Color getColor() {
        return color;
    }

    @Override public String toString() {
        return "HearMessage{" +
               "speaker='" + speaker + '\'' +
               ", text='" + text + '\'' +
               ", color=" + color +
               '}';
    }
}
