package com.yh.qqbot.dto;

public record OutboundMessage(String text, String imagePath) {

    public static OutboundMessage text(String text) {
        return new OutboundMessage(text, null);
    }

    public static OutboundMessage image(String imagePath) {
        return new OutboundMessage(null, imagePath);
    }

    public static OutboundMessage textWithImage(String text, String imagePath) {
        return new OutboundMessage(text, imagePath);
    }

    public boolean isEmpty() {
        return (text == null || text.isBlank()) && (imagePath == null || imagePath.isBlank());
    }
}
