/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.loading.spi;

/**
 * An execution context for queries,
 * providing components allowing to load data from an external source (relational database, ...).
 *
 * @param <E> The type of loaded entities.
 */
public interface SearchLoadingContext<E> {

	Object unwrap();

	ProjectionHitMapper<E> createProjectionHitMapper();

}
