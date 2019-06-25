/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ConsumedPropertyTrackingConfigurationPropertySource implements ConfigurationPropertySource {

	private final ConfigurationPropertySource delegate;

	private final Set<String> consumedPropertyKeys;

	public ConsumedPropertyTrackingConfigurationPropertySource(ConfigurationPropertySource delegate) {
		this.delegate = delegate;
		this.consumedPropertyKeys = ConcurrentHashMap.newKeySet();
	}

	@Override
	public Optional<?> get(String key) {
		Optional<String> resolved = resolve( key );
		if ( resolved.isPresent() ) {
			consumedPropertyKeys.add( resolved.get() );
		}
		return delegate.get( key );
	}

	@Override
	public Optional<String> resolve(String key) {
		return delegate.resolve( key );
	}

	public Set<String> getConsumedPropertyKeys() {
		return Collections.unmodifiableSet( consumedPropertyKeys );
	}
}
