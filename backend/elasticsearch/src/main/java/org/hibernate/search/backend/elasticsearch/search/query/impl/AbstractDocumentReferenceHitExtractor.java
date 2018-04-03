/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchDocumentReference;
import org.hibernate.search.engine.search.DocumentReference;

import com.google.gson.JsonObject;

abstract class AbstractDocumentReferenceHitExtractor<C> implements HitExtractor<C> {

	private static final JsonAccessor<String> HIT_INDEX_NAME_ACCESSOR = JsonAccessor.root().property( "_index" ).asString();

	private final MultiTenancyStrategy multiTenancyStrategy;

	protected AbstractDocumentReferenceHitExtractor(MultiTenancyStrategy multiTenancyStrategy) {
		this.multiTenancyStrategy = multiTenancyStrategy;
	}

	@Override
	public void contributeRequest(JsonObject requestBody) {
		multiTenancyStrategy.contributeToSearchRequest( requestBody );
	}

	protected DocumentReference extractDocumentReference(JsonObject hit) {
		String indexName = HIT_INDEX_NAME_ACCESSOR.get( hit ).get();
		String id = multiTenancyStrategy.extractTenantScopedDocumentId( hit );
		return new ElasticsearchDocumentReference( indexName, id );
	}
}
