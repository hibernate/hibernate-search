/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common.spi;

import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.util.common.AssertionFailure;
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

	DslConverter<?, F> mappingDslConverter();

	DslConverter<F, F> indexDslConverter();

	DslConverter<?, ?> rawDslConverter();

	@Incubating
	DslConverter<?, F> parserDslConverter();

	default DslConverter<?, F> dslConverter(ValueModel valueModel) {
		switch ( valueModel ) {
			case RAW:
				throw new AssertionFailure( "Raw dsl converter is not supported" );
			case INDEX:
				return indexDslConverter();
			case STRING:
				return parserDslConverter();
			case MAPPING:
			default:
				return mappingDslConverter();
		}
	}

	ProjectionConverter<?, ?> rawProjectionConverter();

	ProjectionConverter<F, ?> mappingProjectionConverter();

	ProjectionConverter<F, F> indexProjectionConverter();

	@Incubating
	ProjectionConverter<F, ?> formatterProjectionConverter();

	default ProjectionConverter<F, ?> projectionConverter(ValueModel valueModel) {
		switch ( valueModel ) {
			case RAW:
				throw new AssertionFailure( "Raw projection converter is not supported" );
			case INDEX:
				return indexProjectionConverter();
			case STRING:
				return formatterProjectionConverter();
			case MAPPING:
			default:
				return mappingProjectionConverter();
		}
	}

	boolean highlighterTypeSupported(SearchHighlighterType type);
}
