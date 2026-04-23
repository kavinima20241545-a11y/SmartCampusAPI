package com.smartcampus.exception;

//RoomNotEmptyException

public class RoomNotEmptyException extends RuntimeException {
    public RoomNotEmptyException(String message) {
        super(message);
    }
}