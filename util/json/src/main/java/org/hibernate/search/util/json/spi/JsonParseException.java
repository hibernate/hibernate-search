package org.hibernate.search.util.json.spi;

import org.hibernate.search.util.common.annotation.Incubating;

/** Malformed, truncated or constrained JSON input. No parser-library types appear in this contract. */
@Incubating
public class JsonParseException extends IllegalArgumentException {
	public JsonParseException(String message) {
		super( message );
	}

	public JsonParseException(String message, Throwable cause) {
		super( message, cause );
	}
}
