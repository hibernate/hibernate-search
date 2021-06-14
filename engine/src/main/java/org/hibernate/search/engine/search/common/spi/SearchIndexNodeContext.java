/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common.spi;

import java.util.List;

import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

/**
 * Information about an index node targeted by search,
 * be it the index root, a value field or an object field.
 * <p>
 * This is used in predicates, projections, sorts, ...
 *
 * @param <SC> The type of the backend-specific search scope.
 */
public interface SearchIndexNodeContext<SC extends SearchIndexScope>
		extends EventContextProvider {

	EventContext relativeEventContext();

	boolean isComposite();

	boolean isValueField();

	SearchIndexCompositeNodeContext<SC> toComposite();

	SearchIndexValueFieldContext<SC> toValueField();

	String absolutePath();

	String[] absolutePathComponents();

	List<String> nestedPathHierarchy();

	default String nestedDocumentPath() {
		List<String> hierarchy = nestedPathHierarchy();
		return ( hierarchy.isEmpty() ) ? null :
				// nested path is the LAST element on the path hierarchy
				hierarchy.get( hierarchy.size() - 1 );
	}

	// Query elements: predicates, sorts, projections, aggregations, ...

	<T> T queryElement(SearchQueryElementTypeKey<T> key, SC searchContext);

}
