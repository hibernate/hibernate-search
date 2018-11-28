/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.util.impl.common.Contracts;
import org.hibernate.search.util.impl.common.LoggerFactory;

abstract class AbstractStandardLuceneFieldPredicateBuilderFactory<C extends LuceneFieldConverter<?, ?>>
		implements LuceneFieldPredicateBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	final C converter;

	AbstractStandardLuceneFieldPredicateBuilderFactory(C converter) {
		Contracts.assertNotNull( converter, "converter" );
		this.converter = converter;
	}

	@Override
	public boolean isDslCompatibleWith(LuceneFieldPredicateBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractStandardLuceneFieldPredicateBuilderFactory<?> castedOther =
				(AbstractStandardLuceneFieldPredicateBuilderFactory<?>) other;
		return converter.isConvertDslToIndexCompatibleWith( castedOther.converter );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinCirclePredicateBuilder(
			String absoluteFieldPath) {
		throw log.spatialPredicatesNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinPolygonPredicateBuilder(
			String absoluteFieldPath) {
		throw log.spatialPredicatesNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinBoundingBoxPredicateBuilder(
			String absoluteFieldPath) {
		throw log.spatialPredicatesNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}
}
