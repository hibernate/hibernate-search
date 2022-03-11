/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common.spi;

/**
 * Information about a value (non-object) field targeted by search,
 * be it in a projection, a predicate, a sort, ...
 *
 * @param <SC> The type of the backend-specific search scope.
 */
public interface SearchIndexValueFieldContext<SC extends SearchIndexScope<?>>
		extends SearchIndexNodeContext<SC> {

	SearchIndexValueFieldTypeContext<SC, ?, ?> type();

}
