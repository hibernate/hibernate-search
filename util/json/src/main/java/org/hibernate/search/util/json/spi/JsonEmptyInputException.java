package org.hibernate.search.util.json.spi;

import org.hibernate.search.util.common.annotation.Incubating;

/** No JSON value was present at EOF. Transport boundaries may interpret this as an absent body. */
@Incubating
public final class JsonEmptyInputException extends JsonParseException {
	public JsonEmptyInputException() {
		this( "Expected one JSON root; input was empty" );
	}

	public JsonEmptyInputException(String message) {
		super( message );
	}
}
