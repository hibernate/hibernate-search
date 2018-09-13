/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Optional;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.search.query.impl.HitExtractor;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

public interface LuceneSearchProjection<T> extends SearchProjection<T> {

	Optional<HitExtractor<? super ProjectionHitCollector>> getHitExtractor(LuceneIndexModel indexModel);
}
