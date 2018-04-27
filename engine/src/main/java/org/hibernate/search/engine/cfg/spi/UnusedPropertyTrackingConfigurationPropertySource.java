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

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

public final class UnusedPropertyTrackingConfigurationPropertySource implements ConfigurationPropertySource {

	private final ConfigurationPropertySource delegate;

	private final Set<String> unusedPropertyKeys;

	public UnusedPropertyTrackingConfigurationPropertySource(
			ConfigurationPropertySource delegate, Set<String> availablePropertyKeys) {
		this.delegate = delegate;
		this.unusedPropertyKeys = ConcurrentHashMap.newKeySet();
		unusedPropertyKeys.addAll( availablePropertyKeys );
	}

	@Override
	public Optional<?> get(String key) {
		unusedPropertyKeys.remove( key );
		return delegate.get( key );
	}

	public Set<String> getUnusedPropertyKeys() {
		return Collections.unmodifiableSet( unusedPropertyKeys );
	}
}
