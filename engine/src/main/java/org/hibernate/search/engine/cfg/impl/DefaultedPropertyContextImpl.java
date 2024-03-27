/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.DefaultedPropertyContext;

final class DefaultedPropertyContextImpl<T> implements DefaultedPropertyContext<T> {
	private final String key;
	private final Function<Object, T> converter;
	private final Supplier<T> defaultValueSupplier;

	DefaultedPropertyContextImpl(String key, Function<Object, T> converter, Supplier<T> defaultValueSupplier) {
		this.key = key;
		this.converter = converter;
		this.defaultValueSupplier = defaultValueSupplier;
	}

	@Override
	public ConfigurationProperty<T> build() {
		return new DefaultedConfigurationProperty<>( key, converter, defaultValueSupplier );
	}
}
