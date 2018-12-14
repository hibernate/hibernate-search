/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.projection;

import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;

/**
 * A DSL context allowing to create a projection, with some Lucene-specific methods.
 *
 * @param <R> The type of references.
 * @param <O> The type of loaded objects.
 * @see SearchProjectionFactoryContext
 */
public interface LuceneSearchProjectionFactoryContext<R, O> extends SearchProjectionFactoryContext<R, O> {
}
