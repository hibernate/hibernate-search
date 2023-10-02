/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import static org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey.of;

import org.hibernate.search.engine.backend.types.IndexFieldTraits;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.projection.dsl.spi.HighlightProjectionBuilder;

public final class ProjectionTypeKeys {

	private ProjectionTypeKeys() {
	}

	public static final SearchQueryElementTypeKey<FieldProjectionBuilder.TypeSelector> FIELD =
			of( IndexFieldTraits.Projections.FIELD );
	public static final SearchQueryElementTypeKey<DistanceToFieldProjectionBuilder> DISTANCE =
			of( IndexFieldTraits.Projections.DISTANCE );
	public static final SearchQueryElementTypeKey<CompositeProjectionBuilder> OBJECT =
			of( IndexFieldTraits.Projections.OBJECT );
	public static final SearchQueryElementTypeKey<HighlightProjectionBuilder> HIGHLIGHT =
			of( IndexFieldTraits.Projections.HIGHLIGHT );
	public static final SearchQueryElementTypeKey<?> ID = of( "projection:id" );
	public static final SearchQueryElementTypeKey<?> DOCUMENT_REFERENCE = of( "projection:document-reference" );
	public static final SearchQueryElementTypeKey<?> ENTITY = of( "projection:entity" );
	public static final SearchQueryElementTypeKey<?> ENTITY_REFERENCE = of( "projection:entity-reference" );
	public static final SearchQueryElementTypeKey<?> SCORE = of( "projection:score" );
}
