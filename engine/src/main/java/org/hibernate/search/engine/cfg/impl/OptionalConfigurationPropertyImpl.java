/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.impl;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
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
	public T getOrThrow(ConfigurationPropertySource source, Supplier<RuntimeException> exceptionSupplier) {
		return getAndTransform( source, optional -> optional.orElseThrow( exceptionSupplier ) );
	}

	@Override
	public <R> R getAndMapOrThrow(ConfigurationPropertySource source, Function<T, R> transform,
			Supplier<RuntimeException> exceptionSupplier) {
		return getAndTransform( source, optional -> optional.map( transform ).orElseThrow( exceptionSupplier ) );
	}

	@Override
	<R> R convert(Optional<?> rawValue, Function<Optional<T>, R> transform) {
		return transform.apply( rawValue.map( converter ) );
	}

}
