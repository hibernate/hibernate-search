package org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi;

/** Owns the current attempt's cleanup, including cancellation before a response head arrives. */
public final class JsonResponseConsumerScope {
	private volatile boolean cancelled;
	private Runnable release;

	public void register(Runnable cleanup) {
		Runnable previous;
		boolean releaseNow;
		synchronized (this) {
			previous = release;
			releaseNow = cancelled;
			release = releaseNow ? null : cleanup;
		}
		// Never hold the scope lock while waiting for an in-flight parser callback to finish.
		if ( previous != null ) {
			previous.run();
		}
		if ( releaseNow ) {
			cleanup.run();
		}
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void cancel() {
		Runnable cleanup;
		synchronized (this) {
			cancelled = true;
			cleanup = release;
			release = null;
		}
		if ( cleanup != null ) {
			cleanup.run();
		}
	}
}
