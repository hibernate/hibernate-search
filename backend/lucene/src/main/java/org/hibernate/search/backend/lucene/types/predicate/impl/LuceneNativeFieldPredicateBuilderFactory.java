/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneNativeFieldPredicateBuilderFactory<F> implements LuceneFieldPredicateBuilderFactory<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public boolean isSearchable() {
		return false;
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldPredicateBuilderFactory<?> other) {
		return getClass().equals( other.getClass() );
	}

	@Override
	public MatchPredicateBuilder createMatchPredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public RangePredicateBuilder createRangePredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public PhrasePredicateBuilder createPhrasePredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public WildcardPredicateBuilder createWildcardPredicateBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public LuceneSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldState(
			LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public ExistsPredicateBuilder createExistsPredicateBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder createSpatialWithinBoundingBoxPredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder createSpatialWithinCirclePredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder createSpatialWithinPolygonPredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	private SearchException unsupported(LuceneSearchFieldContext<?> field) {
		return log.unsupportedDSLPredicatesForNativeField( field.eventContext() );
	}
}
