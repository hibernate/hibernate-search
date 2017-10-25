/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalPropertyContext;
import org.hibernate.search.engine.cfg.spi.PropertyContext;

final class OptionalPropertyContextImpl<T> implements OptionalPropertyContext<T> {

	private final String key;
	private final Function<Object, Optional<T>> converter;

	public OptionalPropertyContextImpl(String key, Function<Object, Optional<T>> converter) {
		this.key = key;
		this.converter = converter;
	}

	@Override
	public OptionalPropertyContext<List<T>> multivalued(Pattern separatorPattern) {
		return new OptionalPropertyContextImpl<>(
				key,
				v -> ConvertUtils.convertMultiValue( separatorPattern, converter, v )
		);
	}

	@Override
	public PropertyContext<T> withDefault(T defaultValue) {
		return new PropertyContextImpl<>( key, createOptionalParser().andThen( o -> o.orElse( defaultValue ) ) );
	}

	@Override
	public PropertyContext<T> withDefault(Supplier<T> defaultValueSupplier) {
		return new PropertyContextImpl<>( key, createOptionalParser().andThen( o -> o.orElseGet( defaultValueSupplier ) ) );
	}

	@Override
	public ConfigurationProperty<Optional<T>> build() {
		return new FunctionConfigurationProperty<>( key, createOptionalParser() );
	}

	private Function<Optional<?>, Optional<T>> createOptionalParser() {
		return o -> o.flatMap( converter );
	}
}
