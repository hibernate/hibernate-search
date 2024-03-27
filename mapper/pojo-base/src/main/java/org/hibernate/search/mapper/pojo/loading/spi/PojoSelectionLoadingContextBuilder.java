/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;

/**
 * A builder for {@link PojoSelectionLoadingContext},
 * allowing changes to the parameters of object loading,
 * for example while a query is being built.
 *
 * @param <LOS> The type of the initial step of the loading options definition DSL
 * accessible through {@link SearchQueryOptionsStep#loading(Consumer)}.
 */
public interface PojoSelectionLoadingContextBuilder<LOS> {

	/**
	 * @return The inital step of the loading options definition DSL passed to user-defined consumers added through
	 * {@link SearchQueryOptionsStep#loading(Consumer)}.
	 */
	LOS toAPI();

	/**
	 * @return The configured loading context.
	 */
	PojoSelectionLoadingContext build();

}
