/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

class FunctionConfigurationProperty<T> implements ConfigurationProperty<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String key;
	private final Function<Optional<?>, T> function;

	FunctionConfigurationProperty(String key, Function<Optional<?>, T> function) {
		this.key = key;
		this.function = function;
	}

	@Override
	public T get(ConfigurationPropertySource source) {
		Optional<?> rawValue = source.get( key );
		try {
			return function.apply( rawValue );
		}
		catch (RuntimeException e) {
			String displayedKey = key;

			// Try to display the key that must be set by the user, if possible
			try {
				Optional<String> resolvedKey = source.resolve( key );
				if ( resolvedKey.isPresent() ) {
					displayedKey = resolvedKey.get();
				}
			}
			catch (RuntimeException e2) {
				// Take care not to erase the original exception
				e.addSuppressed( e2 );
			}

			throw log.unableToConvertConfigurationProperty(
					displayedKey, rawValue.isPresent() ? rawValue.get() : "", e.getMessage(), e
			);
		}
	}

	@Override
	public Optional<String> resolve(ConfigurationPropertySource source) {
		return source.resolve( key );
	}

	@Override
	public String resolveOrRaw(ConfigurationPropertySource source) {
		return resolve( source ).orElse( key );
	}
}
