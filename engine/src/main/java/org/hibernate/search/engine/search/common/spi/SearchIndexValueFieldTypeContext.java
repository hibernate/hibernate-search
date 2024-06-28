/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common.spi;

import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.util.common.annotation.Incubating;

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

	@Incubating
	DslConverter<?, F> parser();

	default DslConverter<?, F> dslConverter(ValueModel valueModel) {
		switch ( valueModel ) {
			case INDEX:
				return rawDslConverter();
			case STRING:
				return parser();
			case MAPPING:
			default:
				return dslConverter();
		}
	}

	ProjectionConverter<F, ?> projectionConverter();

	ProjectionConverter<F, F> rawProjectionConverter();

	@Incubating
	ProjectionConverter<F, ?> formatter();

	default ProjectionConverter<F, ?> projectionConverter(ValueModel valueModel) {
		switch ( valueModel ) {
			case INDEX:
				return rawProjectionConverter();
			case STRING:
				return formatter();
			case MAPPING:
			default:
				return projectionConverter();
		}
	}

	boolean highlighterTypeSupported(SearchHighlighterType type);
}
