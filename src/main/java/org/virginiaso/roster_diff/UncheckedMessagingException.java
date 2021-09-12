package org.virginiaso.roster_diff;

import javax.mail.MessagingException;

public class UncheckedMessagingException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public UncheckedMessagingException(MessagingException cause) {
		super(cause.getMessage(), cause);
	}
}
