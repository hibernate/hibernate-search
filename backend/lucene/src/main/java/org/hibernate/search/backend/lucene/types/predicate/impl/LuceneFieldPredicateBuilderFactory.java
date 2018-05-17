/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateCollector;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;

/**
 * @author Guillaume Smet
 */
public interface LuceneFieldPredicateBuilderFactory {

	MatchPredicateBuilder<LuceneSearchPredicateCollector> createMatchPredicateBuilder(String absoluteFieldPath);

	RangePredicateBuilder<LuceneSearchPredicateCollector> createRangePredicateBuilder(String absoluteFieldPath);

	SpatialWithinCirclePredicateBuilder<LuceneSearchPredicateCollector> createSpatialWithinCirclePredicateBuilder(String absoluteFieldPath);

	// equals()/hashCode() needs to be implemented if the predicate factory is not a singleton

	boolean equals(Object obj);

	int hashCode();
}
