/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.IdProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ScoreProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.util.common.function.TriFunction;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Explanation;

public class LuceneSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private final LuceneSearchIndexScope<?> scope;

	public LuceneSearchProjectionBuilderFactory(LuceneSearchIndexScope<?> scope) {
		this.scope = scope;
	}

	@Override
	public DocumentReferenceProjectionBuilder documentReference() {
		return new LuceneDocumentReferenceProjection.Builder( scope );
	}

	@Override
	public <E> EntityProjectionBuilder<E> entity() {
		return new LuceneEntityProjection.Builder<>( scope );
	}

	@Override
	public <R> EntityReferenceProjectionBuilder<R> entityReference() {
		return new LuceneEntityReferenceProjection.Builder<>( scope );
	}

	@Override
	public <I> IdProjectionBuilder<I> id(Class<I> identifierType) {
		SearchIndexIdentifierContext identifier = scope.identifier();
		return new LuceneIdProjection.Builder<>( scope,
				identifier.projectionConverter().withConvertedType( identifierType, identifier ) );
	}

	@Override
	public ScoreProjectionBuilder score() {
		return new LuceneScoreProjection.Builder( scope );
	}

	@Override
	public <V> CompositeProjectionBuilder<V> composite(Function<List<?>, V> transformer,
			SearchProjection<?>... projections) {
		LuceneSearchProjection<?, ?>[] typedProjections = new LuceneSearchProjection<?, ?>[ projections.length ];
		for ( int i = 0; i < projections.length; i++ ) {
			typedProjections[i] = toImplementation( projections[i] );
		}
		return new LuceneCompositeProjection.Builder<>( scope,
				ProjectionCompositor.fromList( projections.length, transformer ),
				typedProjections );
	}

	@Override
	public <P1, V> CompositeProjectionBuilder<V> composite(Function<P1, V> transformer,
			SearchProjection<P1> projection) {
		return new LuceneCompositeProjection.Builder<>( scope,
				ProjectionCompositor.from( transformer ),
				toImplementation( projection ) );
	}

	@Override
	public <P1, P2, V> CompositeProjectionBuilder<V> composite(BiFunction<P1, P2, V> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		return new LuceneCompositeProjection.Builder<>( scope,
				ProjectionCompositor.from( transformer ),
				toImplementation( projection1 ), toImplementation( projection2 ) );
	}

	@Override
	public <P1, P2, P3, V> CompositeProjectionBuilder<V> composite(TriFunction<P1, P2, P3, V> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		return new LuceneCompositeProjection.Builder<>( scope,
				ProjectionCompositor.from( transformer ),
				toImplementation( projection1 ), toImplementation( projection2 ), toImplementation( projection3 ) );
	}

	public SearchProjectionBuilder<Document> document() {
		return new LuceneDocumentProjection.Builder( scope );
	}

	public SearchProjectionBuilder<Explanation> explanation() {
		return new LuceneExplanationProjection.Builder( scope );
	}

	private <T> LuceneSearchProjection<?, T> toImplementation(SearchProjection<T> projection) {
		return LuceneSearchProjection.from( scope, projection );
	}
}
