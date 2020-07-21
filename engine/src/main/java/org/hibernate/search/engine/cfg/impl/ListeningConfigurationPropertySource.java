/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.util.common.impl.Contracts;

public class ListeningConfigurationPropertySource implements ConfigurationPropertySource {
	private final ConfigurationPropertySource delegate;
	private final Consumer<String> listener;

	public ListeningConfigurationPropertySource(ConfigurationPropertySource delegate, Consumer<String> listener) {
		Contracts.assertNotNull( delegate, "delegate" );
		Contracts.assertNotNull( listener, "listener" );
		this.delegate = delegate;
		this.listener = listener;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "delegate=" + delegate
				+ ", listener=" + listener
				+ "]";
	}

	@Override
	public Optional<?> get(String key) {
		Optional<?> result = delegate.get( key );
		if ( result.isPresent() ) {
			listener.accept( delegate.resolve( key )
					// This shouldn't happen, but let's be extra careful...
					.orElse( "<Could not resolve: " + key + ">" ) );
		}
		return result;
	}

	@Override
	public Optional<String> resolve(String key) {
		return delegate.resolve( key );
	}
}
