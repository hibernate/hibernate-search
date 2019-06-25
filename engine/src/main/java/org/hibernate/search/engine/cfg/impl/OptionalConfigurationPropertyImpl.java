/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;

final class OptionalConfigurationPropertyImpl<T> extends AbstractConfigurationProperty<Optional<T>>
		implements OptionalConfigurationProperty<T> {

	private final Function<Object, T> converter;

	OptionalConfigurationPropertyImpl(String key, Function<Object, T> converter) {
		super( key );
		this.converter = converter;
	}

	@Override
	public <R> Optional<R> getAndMap(ConfigurationPropertySource source, Function<T, R> transform) {
		return getAndTransform( source, optional -> optional.map( transform ) );
	}

	@Override
	public T getOrThrow(ConfigurationPropertySource source, Function<String, RuntimeException> exceptionFunction) {
		return get( source ).orElseThrow( () -> exceptionFunction.apply( resolveOrRaw( source ) ) );
	}

	@Override
	public <R> R getAndMapOrThrow(ConfigurationPropertySource source, Function<T, R> transform,
			Function<String, RuntimeException> exceptionFunction) {
		return getAndTransform( source, optional -> optional.map( transform ) )
				.orElseThrow( () -> exceptionFunction.apply( resolveOrRaw( source ) ) );
	}

	@Override
	<R> R convert(Optional<?> rawValue, Function<Optional<T>, R> transform) {
		return transform.apply( rawValue.map( converter ) );
	}

}
