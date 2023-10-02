/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

public final class ConsumedPropertyTrackingConfigurationPropertySource implements ConfigurationPropertySource {

	private final ConfigurationPropertySource delegate;

	private final Consumer<String> tracker;

	public ConsumedPropertyTrackingConfigurationPropertySource(ConfigurationPropertySource delegate,
			Consumer<String> tracker) {
		this.delegate = delegate;
		this.tracker = tracker;
	}

	@Override
	public Optional<?> get(String key) {
		Optional<String> resolved = resolve( key );
		if ( resolved.isPresent() ) {
			tracker.accept( resolved.get() );
		}
		return delegate.get( key );
	}

	@Override
	public Optional<String> resolve(String key) {
		return delegate.resolve( key );
	}

}
