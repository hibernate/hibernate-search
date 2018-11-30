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

import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.DefaultedPropertyContext;

final class DefaultedPropertyContextImpl<T> implements DefaultedPropertyContext<T> {
	private final String key;
	private final Function<Object, Optional<T>> converter;
	private final Supplier<T> defaultValueSupplier;

	DefaultedPropertyContextImpl(String key, Function<Object, Optional<T>> converter, Supplier<T> defaultValueSupplier) {
		this.key = key;
		this.converter = converter;
		this.defaultValueSupplier = defaultValueSupplier;
	}

	@Override
	public ConfigurationProperty<T> build() {
		return new DefaultedConfigurationProperty<>( key, converter, defaultValueSupplier );
	}
}
