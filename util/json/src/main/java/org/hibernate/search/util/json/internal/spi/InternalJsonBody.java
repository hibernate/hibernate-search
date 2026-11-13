package org.hibernate.search.util.json.internal.spi;

import java.io.IOException;
import java.io.OutputStream;

import org.hibernate.search.util.json.impl.JsonBodyOutput;
import org.hibernate.search.util.json.spi.JsonBody;

/** Internal writer selection; external transports only see JsonBody. */
public interface InternalJsonBody extends JsonBody {
	@Override
	default void writeTo(OutputStream destination) throws IOException {
		writeTo( destination, JsonWriteStrategy.NATIVE );
	}

	default void writeTo(OutputStream destination, JsonWriteStrategy strategy) throws IOException {
		java.util.Objects.requireNonNull( strategy );
		JsonBodyOutput.writeTo( this, destination );
	}
}
