/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.hibernate.search.engine.cfg.spi.ConvertUtils;
import org.hibernate.search.engine.cfg.spi.DefaultedPropertyContext;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalPropertyContext;

final class OptionalPropertyContextImpl<T> implements OptionalPropertyContext<T> {

	private static final Pattern MULTI_VALUE_SEPARATOR_PATTERN = Pattern.compile( "," );

	private final String key;
	private final Function<Object, T> converter;

	OptionalPropertyContextImpl(String key, Function<Object, T> converter) {
		this.key = key;
		this.converter = converter;
	}

	@Override
	public OptionalPropertyContext<T> substitute(UnaryOperator<Object> substitution) {
		return new OptionalPropertyContextImpl<>( key, substitution.andThen( converter ) );
	}

	@Override
	public OptionalPropertyContext<T> substitute(Object expected, Object replacement) {
		return substitute( v -> {
			if ( Objects.equals( v, expected ) ) {
				return replacement;
			}
			else {
				return v;
			}
		} );
	}

	@Override
	public OptionalPropertyContext<List<T>> multivalued() {
		return new OptionalPropertyContextImpl<>(
				key,
				v -> ConvertUtils.convertMultiValue( MULTI_VALUE_SEPARATOR_PATTERN, converter, v )
		);
	}

	@Override
	public DefaultedPropertyContext<T> withDefault(T defaultValue) {
		return new DefaultedPropertyContextImpl<>( key, converter, () -> defaultValue );
	}

	@Override
	public DefaultedPropertyContext<T> withDefault(Supplier<T> defaultValueSupplier) {
		return new DefaultedPropertyContextImpl<>( key, converter, defaultValueSupplier );
	}

	@Override
	public OptionalConfigurationProperty<T> build() {
		return new OptionalConfigurationPropertyImpl<>( key, converter );
	}
}
