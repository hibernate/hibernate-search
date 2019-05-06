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
 * @param <R> The type of object references.
 * @param <O> The type of loaded objects.
 */
public interface LoadingContextBuilder<R, O> {

	LoadingContext<R, O> build();

}
