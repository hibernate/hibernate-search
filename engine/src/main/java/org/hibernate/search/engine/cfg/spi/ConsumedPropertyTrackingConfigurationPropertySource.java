/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.Optional;
import java.util.function.Consumer;

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
