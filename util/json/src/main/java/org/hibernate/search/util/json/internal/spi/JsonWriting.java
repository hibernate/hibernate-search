package org.hibernate.search.util.json.internal.spi;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import org.hibernate.search.util.json.impl.JsonBodyOutput;
import org.hibernate.search.util.json.spi.JsonBody;

/** Internal strategy selection for blocking output. */
public final class JsonWriting {
	private JsonWriting() {
	}

	public static void writeTo(JsonBody body, OutputStream destination, JsonWriteStrategy strategy) throws IOException {
		Objects.requireNonNull( strategy );
		if ( strategy == JsonWriteStrategy.NATIVE ) {
			body.writeTo( destination );
		}
		else {
			JsonBodyOutput.writeTo( body, destination );
		}
	}
}
