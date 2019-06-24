/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.projection.impl;

import org.hibernate.search.backend.lucene.search.dsl.projection.LuceneSearchProjectionFactory;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionBuilderFactory;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactory;
import org.hibernate.search.engine.search.dsl.projection.ProjectionFinalStep;
import org.hibernate.search.engine.search.dsl.projection.spi.DelegatingSearchProjectionFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Explanation;

public class LuceneSearchProjectionFactoryImpl<R, E>
		extends DelegatingSearchProjectionFactory<R, E>
		implements LuceneSearchProjectionFactory<R, E> {

	private final LuceneSearchProjectionBuilderFactory factory;

	public LuceneSearchProjectionFactoryImpl(SearchProjectionFactory<R, E> delegate,
			LuceneSearchProjectionBuilderFactory factory) {
		super( delegate );
		this.factory = factory;
	}

	@Override
	public ProjectionFinalStep<Document> document() {
		return new LuceneDocumentProjectionFinalStep( factory );
	}

	@Override
	public ProjectionFinalStep<Explanation> explanation() {
		return new LuceneExplanationProjectionFinalStep( factory );
	}
}
