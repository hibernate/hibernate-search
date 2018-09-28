/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Optional;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.search.extraction.impl.HitExtractor;
import org.hibernate.search.backend.lucene.search.extraction.impl.ScoreHitExtractor;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

public class ScoreSearchProjectionImpl implements LuceneSearchProjection<Float> {

	private static final ScoreSearchProjectionImpl INSTANCE = new ScoreSearchProjectionImpl();

	static ScoreSearchProjectionImpl get() {
		return INSTANCE;
	}

	private ScoreSearchProjectionImpl() {
	}

	@Override
	public Optional<HitExtractor<? super ProjectionHitCollector>> getHitExtractor(LuceneIndexModel indexModel) {
		return Optional.of( ScoreHitExtractor.get() );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
