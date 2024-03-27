/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common.spi;

import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;

/**
 * Information about the type of a value (non-object) field targeted by search,
 * be it in a projection, a predicate, a sort, ...
 *
 * @param <SC> The type of the backend-specific search scope.
 * @param <N> The type representing the targeted index node.
 * @param <F> The indexed field value type.
 */
public interface SearchIndexValueFieldTypeContext<
		SC extends SearchIndexScope<?>,
		N,
		F>
		extends SearchIndexNodeTypeContext<SC, N> {

	Class<F> valueClass();

	DslConverter<?, F> dslConverter();

	DslConverter<F, F> rawDslConverter();

	default DslConverter<?, F> dslConverter(ValueConvert convert) {
		switch ( convert ) {
			case NO:
				return rawDslConverter();
			case YES:
			default:
				return dslConverter();
		}
	}

	ProjectionConverter<F, ?> projectionConverter();

	ProjectionConverter<F, F> rawProjectionConverter();

	default ProjectionConverter<F, ?> projectionConverter(ValueConvert convert) {
		switch ( convert ) {
			case NO:
				return rawProjectionConverter();
			case YES:
			default:
				return projectionConverter();
		}
	}

	boolean highlighterTypeSupported(SearchHighlighterType type);
}
