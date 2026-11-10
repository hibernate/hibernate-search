package org.hibernate.search.util.json.spi;

import java.nio.charset.Charset;

/** Shareable factory: create an independent consumer for each response attempt. */
@FunctionalInterface
@org.hibernate.search.util.common.annotation.Incubating
public interface JsonResponseConsumerFactory<T> {
	JsonResponseConsumer<T> create(Charset charset, long decodedByteLimit);
}
