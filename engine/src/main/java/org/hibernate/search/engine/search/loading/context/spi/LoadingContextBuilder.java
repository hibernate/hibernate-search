/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.loading.context.spi;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;

/**
 * A builder for {@link LoadingContext},
 * allowing to change the parameters of object loading while a query is being built.
 *
 * @param <R> The type of entity references.
 * @param <E> The type of loaded entities.
 * @param <LOS> The type of the initial step of the loading options definition DSL accessible through {@link SearchQueryOptionsStep#loading(Consumer)}.
 */
public interface LoadingContextBuilder<R, E, LOS> {

	/**
	 * @return The inital step of the loading options definition DSL passed to user-defined consumers added through
	 * {@link SearchQueryOptionsStep#loading(Consumer)}.
	 */
	LOS toAPI();

	/**
	 * @return The configured loading context.
	 */
	LoadingContext<R, E> build();

}
