package com.cts.project.shbs.exception;

public class HotelServiceException  extends RuntimeException{
	public HotelServiceException(String msg, Throwable cause) {
		super(msg,cause);
	}
	
	public HotelServiceException(String msg) {
        super(msg);
    }

}
