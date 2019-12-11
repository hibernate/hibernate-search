/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchDocumentReference;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public final class DocumentReferenceExtractionHelper implements ProjectionExtractionHelper<DocumentReference> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final JsonAccessor<String> HIT_INDEX_NAME_ACCESSOR = JsonAccessor.root().property( "_index" ).asString();

	private final Function<String, String> indexNameConverter;
	private final ProjectionExtractionHelper<String> idHelper;

	public DocumentReferenceExtractionHelper(
			Function<String, String> indexNameConverter,
			ProjectionExtractionHelper<String> idHelper) {
		this.indexNameConverter = indexNameConverter;
		this.idHelper = idHelper;
	}

	@Override
	public void request(JsonObject requestBody) {
		idHelper.request( requestBody );
	}

	@Override
	public DocumentReference extract(JsonObject hit) {
		String indexName = HIT_INDEX_NAME_ACCESSOR.get( hit )
				.map( indexNameConverter )
				.orElseThrow( log::elasticsearchResponseMissingData );
		String id = idHelper.extract( hit );
		return new ElasticsearchDocumentReference( indexName, id );
	}
}
