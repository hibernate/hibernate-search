/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.dsl.impl;

import org.hibernate.search.backend.lucene.search.projection.dsl.LuceneSearchProjectionFactory;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionIndexScope;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.spi.AbstractSearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.dsl.spi.StaticProjectionFinalStep;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Explanation;

public class LuceneSearchProjectionFactoryImpl<R, E>
		extends AbstractSearchProjectionFactory<
				LuceneSearchProjectionFactory<R, E>,
				LuceneSearchProjectionIndexScope<?>,
				R,
				E>
		implements LuceneSearchProjectionFactory<R, E> {

	public LuceneSearchProjectionFactoryImpl(SearchProjectionDslContext<LuceneSearchProjectionIndexScope<?>> dslContext) {
		super( dslContext );
	}

	@Override
	public LuceneSearchProjectionFactory<R, E> withRoot(String objectFieldPath) {
		return new LuceneSearchProjectionFactoryImpl<>( dslContext.rescope(
				dslContext.scope().withRoot( objectFieldPath ) ) );
	}

	@Override
	public ProjectionFinalStep<Document> document() {
		return new StaticProjectionFinalStep<>( dslContext.scope().projectionBuilders().document() );
	}

	@Override
	public ProjectionFinalStep<Explanation> explanation() {
		return new StaticProjectionFinalStep<>( dslContext.scope().projectionBuilders().explanation() );
	}
}
