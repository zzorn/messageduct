package org.messageduct.example;

/**
 *
 */
public class SayMessage {
    private String text;

    // For serialization
    private SayMessage() {
    }

    public SayMessage(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
