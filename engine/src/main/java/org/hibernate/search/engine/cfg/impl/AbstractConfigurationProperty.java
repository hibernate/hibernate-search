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

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConvertUtils;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractConfigurationProperty<T> implements ConfigurationProperty<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String key;

	AbstractConfigurationProperty(String key) {
		this.key = key;
	}

	@Override
	public T get(ConfigurationPropertySource source) {
		return doGet( source, Function.identity() );
	}

	@Override
	public <R> R getAndTransform(ConfigurationPropertySource source, Function<T, R> transform) {
		return doGet( source, transform );
	}

	abstract <R> R convert(Optional<?> rawValue, Function<T, R> transform);

	<R> R doGet(ConfigurationPropertySource source, Function<T, R> transform) {
		Optional<?> rawValue = source.get( key ).map( ConvertUtils::trimIfString );
		try {
			return convert( rawValue, transform );
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
