package com.darwinsys.regexp;

/** An exception that represents a syntax error in an RE pattern. */
public class RESyntaxException extends Exception {
	/** Construct an RESyntaxException with a message */
	public RESyntaxException(String mesg) {
		super(mesg);
	}
	/** Construct a user-hostile RESyntaxException with no message. */
	public RESyntaxException() {
		super();
	}
}
