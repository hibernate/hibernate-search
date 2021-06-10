/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common.spi;

import java.util.Set;

/**
 * Information about indexes targeted by search,
 * be it in a projection, a predicate, a sort, ...
 *
 * @param <S> The self type, i.e. the type of the backend-specific search index scope.
 */
public interface SearchIndexScope<S extends SearchIndexScope<S>> {

	Set<String> hibernateSearchIndexNames();

	SearchIndexCompositeNodeContext<S> root();

	SearchIndexNodeContext<S> field(String absoluteFieldPath);

}