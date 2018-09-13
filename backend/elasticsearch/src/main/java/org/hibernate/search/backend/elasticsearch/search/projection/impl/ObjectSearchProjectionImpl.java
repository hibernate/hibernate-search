/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.search.query.impl.HitExtractor;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

public class ObjectSearchProjectionImpl implements ElasticsearchSearchProjection<Object> {

	private static final ObjectSearchProjectionImpl INSTANCE = new ObjectSearchProjectionImpl();

	static ObjectSearchProjectionImpl get() {
		return INSTANCE;
	}

	private ObjectSearchProjectionImpl() {
	}

	@Override
	public Optional<HitExtractor<? super ProjectionHitCollector>> getHitExtractor(SearchBackendContext searchBackendContext,
			ElasticsearchIndexModel indexModel) {
		return Optional.of( searchBackendContext.getObjectHitExtractor() );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
