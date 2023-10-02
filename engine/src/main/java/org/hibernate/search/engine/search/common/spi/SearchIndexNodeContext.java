/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common.spi;

import java.util.List;

import org.hibernate.search.util.common.SearchException;
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
public interface SearchIndexNodeContext<SC extends SearchIndexScope<?>>
		extends EventContextProvider {

	EventContext relativeEventContext();

	boolean isComposite();

	boolean isObjectField();

	boolean isValueField();

	SearchIndexCompositeNodeContext<SC> toComposite();

	SearchIndexCompositeNodeContext<SC> toObjectField();

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

	String closestMultiValuedParentAbsolutePath();

	boolean multiValued();

	boolean multiValuedInRoot();

	// Query elements: predicates, sorts, projections, aggregations, ...

	<T> T queryElement(SearchQueryElementTypeKey<T> key, SC searchContext);

	SearchException cannotUseQueryElement(SearchQueryElementTypeKey<?> key, String hint, Exception causeOrNull);

}
