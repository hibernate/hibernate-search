/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

final class DefaultedConfigurationProperty<T> extends AbstractConfigurationProperty<T> {

	private final Function<Object, T> converter;
	private final Supplier<T> defaultValueSupplier;

	DefaultedConfigurationProperty(String key, Function<Object, T> converter, Supplier<T> defaultValueSupplier) {
		super( key );
		this.converter = converter;
		this.defaultValueSupplier = defaultValueSupplier;
	}

	@Override
	<R> R convert(Optional<?> rawValue, Function<T, R> transform) {
		T defaultedValue = rawValue.map( converter ).orElseGet( defaultValueSupplier );
		return transform.apply( defaultedValue );
	}
}
