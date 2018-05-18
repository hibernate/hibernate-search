/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateCollector;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.util.impl.common.LoggerFactory;

abstract class AbstractStandardLuceneFieldPredicateBuilderFactory implements LuceneFieldPredicateBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public SpatialWithinCirclePredicateBuilder<LuceneSearchPredicateCollector> createSpatialWithinCirclePredicateBuilder(String absoluteFieldPath) {
		throw log.spatialPredicatesNotSupportedByFieldType( absoluteFieldPath );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<LuceneSearchPredicateCollector> createSpatialWithinPolygonPredicateBuilder(String absoluteFieldPath) {
		throw log.spatialPredicatesNotSupportedByFieldType( absoluteFieldPath );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<LuceneSearchPredicateCollector> createSpatialWithinBoundingBoxPredicateBuilder(String absoluteFieldPath) {
		throw log.spatialPredicatesNotSupportedByFieldType( absoluteFieldPath );
	}
}
