/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

public interface OptionalConfigurationProperty<T> extends ConfigurationProperty<Optional<T>> {

	/**
	 * Get and transform the value of this configuration property.
	 * <p>
	 * Similar to calling {@link #getAndTransform(ConfigurationPropertySource, Function)},
	 * but easier to use, since the transform function is applied to the content of the optional,
	 * not to the optional itself.
	 * <p>
	 * Any exception occurring during transformation will be wrapped in another exception adding some context,
	 * such as the {@link #resolveOrRaw(ConfigurationPropertySource) resolved key} for this property.
	 *
	 * @param source A configuration source.
	 * @param transform A transform function to be applied to the value of this configuration property
	 * before returning the result.
	 * @param <R> The transformed type.
	 * @return The value of this property according to the given source.
	 */
	<R> Optional<R> getAndMap(ConfigurationPropertySource source, Function<T, R> transform);

	/**
	 * Get the value of this configuration property, throwing an exception if the value is not present.
	 *
	 * @param source A configuration source.
	 * @param exceptionSupplier A supplier that will be called to create an exception if the value is missing.
	 * to create an exception if the value is missing.
	 * @return The value of this property according to the given source.
	 */
	T getOrThrow(ConfigurationPropertySource source, Supplier<RuntimeException> exceptionSupplier);

	/**
	 * Get and transform the value of this configuration property, throwing an exception if the value is not present.
	 * <p>
	 * Any exception occurring during transformation will be wrapped in another exception adding some context,
	 * such as the {@link #resolveOrRaw(ConfigurationPropertySource) resolved key} for this property.
	 *
	 * @param <R> The transformed type.
	 * @param source A configuration source.
	 * @param transform A transform function to be applied to the value of this configuration property
	 * before returning the result.
	 * @param exceptionSupplier A supplier that will be called to create an exception if the value is missing.
	 * @return The value of this property according to the given source.
	 */
	<R> R getAndMapOrThrow(ConfigurationPropertySource source, Function<T, R> transform,
			Supplier<RuntimeException> exceptionSupplier);

}
