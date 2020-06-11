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
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
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
	public MatchPredicateBuilder<LuceneSearchPredicateBuilder> createMatchPredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public RangePredicateBuilder<LuceneSearchPredicateBuilder> createRangePredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public PhrasePredicateBuilder<LuceneSearchPredicateBuilder> createPhrasePredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public WildcardPredicateBuilder<LuceneSearchPredicateBuilder> createWildcardPredicateBuilder(
			LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public LuceneSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldState(
			LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public ExistsPredicateBuilder<LuceneSearchPredicateBuilder> createExistsPredicateBuilder(
			LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinBoundingBoxPredicateBuilder(
			LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinCirclePredicateBuilder(
			LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinPolygonPredicateBuilder(
			LuceneSearchFieldContext<F> field) {
		throw unsupported( field );
	}

	private SearchException unsupported(LuceneSearchFieldContext<?> field) {
		return log.unsupportedDSLPredicatesForNativeField( field.eventContext() );
	}
}
