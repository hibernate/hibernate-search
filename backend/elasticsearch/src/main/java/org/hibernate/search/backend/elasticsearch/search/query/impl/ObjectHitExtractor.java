/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.backend.elasticsearch.impl.MultiTenancyStrategy;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.LoadingHitCollector;
import org.hibernate.search.engine.search.query.spi.SearchQueryFactory;

import com.google.gson.JsonObject;

/**
 * A hit extractor used when search results are expected to contain loaded objects.
 *
 * @see SearchQueryFactory#asObjects(SessionContext, HitAggregator)
 */
class ObjectHitExtractor extends AbstractDocumentReferenceHitExtractor<LoadingHitCollector> {

	ObjectHitExtractor(MultiTenancyStrategy multiTenancyStrategy) {
		super( multiTenancyStrategy );
	}

	@Override
	public void extract(LoadingHitCollector collector, JsonObject responseBody, JsonObject hit) {
		collector.collectForLoading( extractDocumentReference( hit ) );
	}

}
