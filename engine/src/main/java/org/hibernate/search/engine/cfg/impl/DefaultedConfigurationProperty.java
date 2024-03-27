/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
