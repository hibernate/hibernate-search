/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.loading.context.spi;

/**
 * A builder for {@link LoadingContext},
 * allowing to change the parameters of object loading while a query is being built.
 *
 * @param <R> The type of entity references.
 * @param <E> The type of loaded entities.
 */
public interface LoadingContextBuilder<R, E> {

	LoadingContext<R, E> build();

}
