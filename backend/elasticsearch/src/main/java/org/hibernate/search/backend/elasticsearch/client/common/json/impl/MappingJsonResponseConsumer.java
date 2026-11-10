package org.hibernate.search.backend.elasticsearch.client.common.json.impl;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.function.Function;

import org.hibernate.search.util.json.spi.JsonResponseConsumer;

/** Maps a consumer result without exposing its representation to the transport. */
public final class MappingJsonResponseConsumer<S, T> implements JsonResponseConsumer<T> {
	private final JsonResponseConsumer<S> delegate;
	private final Function<S, T> mapper;

	public MappingJsonResponseConsumer(JsonResponseConsumer<S> delegate, Function<S, T> mapper) {
		this.delegate = delegate;
		this.mapper = mapper;
	}

	public void feed(ByteBuffer input) {
		delegate.feed( input );
	}

	public void complete() {
		delegate.complete();
	}

	public T result() {
		return mapper.apply( delegate.result() );
	}

	public void failedParsing(RuntimeException failure) {
		delegate.failedParsing( failure );
	}

	public void throwIfFailed() {
		delegate.throwIfFailed();
	}

	public InputStream openDiagnosticStream() {
		return delegate.openDiagnosticStream();
	}

	public long inputBytes() {
		return delegate.inputBytes();
	}

	public void close() {
		delegate.close();
	}
}
