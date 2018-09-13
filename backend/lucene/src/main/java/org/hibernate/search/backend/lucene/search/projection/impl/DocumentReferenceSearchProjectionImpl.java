/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Optional;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.search.query.impl.DocumentReferenceProjectionHitExtractor;
import org.hibernate.search.backend.lucene.search.query.impl.HitExtractor;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

public class DocumentReferenceSearchProjectionImpl implements LuceneSearchProjection<DocumentReference> {

	private static final DocumentReferenceSearchProjectionImpl INSTANCE = new DocumentReferenceSearchProjectionImpl();

	static DocumentReferenceSearchProjectionImpl get() {
		return INSTANCE;
	}

	private DocumentReferenceSearchProjectionImpl() {
	}

	@Override
	public Optional<HitExtractor<? super ProjectionHitCollector>> getHitExtractor(LuceneIndexModel indexModel) {
		return Optional.of( DocumentReferenceProjectionHitExtractor.get() );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
