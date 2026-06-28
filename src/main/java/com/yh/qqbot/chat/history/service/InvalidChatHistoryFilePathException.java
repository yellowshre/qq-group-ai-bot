package com.yh.qqbot.chat.history.service;

public class InvalidChatHistoryFilePathException extends RuntimeException {

    public InvalidChatHistoryFilePathException() {
        super("Invalid chat history file path");
    }
}
