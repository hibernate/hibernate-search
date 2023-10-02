/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common;

import java.util.Optional;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.annotation.impl.SuppressJQAssistant;

@Incubating
@SuppressJQAssistant(
		reason = "We want to use the get/getOptional methods in this interface and there are some rules that prevents get*() methods on public types.")
public interface NamedValues {
	/**
	 * @param name The name of the value.
	 * @param paramType The expected type of the parameter.
	 * @param <T> The expected type of the value.
	 * @return The value with the given name.
	 * @throws SearchException If there is no value with the given name.
	 * @throws ClassCastException If the value with the given name cannot be cast to the expected type.
	 */
	<T> T get(String name, Class<T> paramType);

	/**
	 * @param name The name of the value.
	 * @param paramType The expected type of the parameter.
	 * @param <T> The expected type of the value.
	 * @return An optional containing the value with the given name,
	 * or {@link Optional#empty} if there is none.
	 * @throws ClassCastException If the value with the given name cannot be cast to the expected type.
	 */
	<T> Optional<T> getOptional(String name, Class<T> paramType);
}
