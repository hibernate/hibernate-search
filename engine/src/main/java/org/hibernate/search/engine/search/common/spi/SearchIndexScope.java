/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common.spi;

import java.util.Set;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

/**
 * Information about indexes targeted by search,
 * be it in a projection, a predicate, a sort, ...
 *
 * @param <S> The self type, i.e. the exposed type of this scope.
 */
public interface SearchIndexScope<S extends SearchIndexScope<?>>
		extends EventContextProvider {

	BackendMappingContext mappingContext();

	Set<String> hibernateSearchIndexNames();

	ToDocumentValueConvertContext toDocumentValueConvertContext();

	S withRoot(String objectFieldPath);

	String toAbsolutePath(String relativeFieldPath);

	SearchIndexIdentifierContext identifier();

	SearchIndexNodeContext<?> child(SearchIndexCompositeNodeContext<?> parent, String name);

	<T> T rootQueryElement(SearchQueryElementTypeKey<T> key);

	<T> T fieldQueryElement(String fieldPath, SearchQueryElementTypeKey<T> key);

}