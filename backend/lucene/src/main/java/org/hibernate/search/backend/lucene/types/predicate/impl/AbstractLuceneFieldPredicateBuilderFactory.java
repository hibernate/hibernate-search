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
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractLuceneFieldPredicateBuilderFactory<F>
		implements LuceneFieldPredicateBuilderFactory<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean searchable;

	public AbstractLuceneFieldPredicateBuilderFactory(boolean searchable) {
		this.searchable = searchable;
	}

	@Override
	public final boolean isSearchable() {
		return searchable;
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldPredicateBuilderFactory<?> other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		AbstractLuceneFieldPredicateBuilderFactory<?> castedOther =
				(AbstractLuceneFieldPredicateBuilderFactory<?>) other;
		return getCodec().isCompatibleWith( castedOther.getCodec() );
	}

	@Override
	public PhrasePredicateBuilder createPhrasePredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		throw log.textPredicatesNotSupportedByFieldType( field.eventContext() );
	}

	@Override
	public WildcardPredicateBuilder createWildcardPredicateBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field) {
		throw log.textPredicatesNotSupportedByFieldType( field.eventContext() );
	}

	@Override
	public LuceneSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldState(
			LuceneSearchFieldContext<F> field) {
		throw log.textPredicatesNotSupportedByFieldType( field.eventContext() );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder createSpatialWithinCirclePredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		throw log.spatialPredicatesNotSupportedByFieldType( field.eventContext() );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder createSpatialWithinPolygonPredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		throw log.spatialPredicatesNotSupportedByFieldType( field.eventContext() );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder createSpatialWithinBoundingBoxPredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		throw log.spatialPredicatesNotSupportedByFieldType( field.eventContext() );
	}

	@Override
	public ExistsPredicateBuilder createExistsPredicateBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field) {
		return new LuceneExistsPredicateBuilder( searchContext, field, getCodec() );
	}

	protected abstract LuceneFieldCodec<?> getCodec();

	protected void checkSearchable(LuceneSearchFieldContext<?> field) {
		if ( !searchable ) {
			throw log.nonSearchableField( field.absolutePath(), field.eventContext() );
		}
	}
}
