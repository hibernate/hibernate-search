/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractionHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * Rely on the "_index" meta-field to resolve the type name.
 * Does not work with index aliases.
 */
public class IndexNameTypeNameMapping implements TypeNameMapping {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final TypeNameFromIndexNameExtractionHelper mappedTypeNameExtractionHelper =
			new TypeNameFromIndexNameExtractionHelper();

	@Override
	public void register(String elasticsearchIndexName, String mappedTypeName) {
		mappedTypeNameExtractionHelper.mappedTypeNamesByElasticsearchIndexNames
				.put( elasticsearchIndexName, mappedTypeName );
	}

	@Override
	public ProjectionExtractionHelper<String> getMappedTypeNameExtractionHelper() {
		return mappedTypeNameExtractionHelper;
	}

	private static final class TypeNameFromIndexNameExtractionHelper implements ProjectionExtractionHelper<String> {

		private static final JsonAccessor<String> HIT_INDEX_NAME_ACCESSOR =
				JsonAccessor.root().property( "_index" ).asString();

		private final Map<String, String> mappedTypeNamesByElasticsearchIndexNames = new ConcurrentHashMap<>();

		@Override
		public void request(JsonObject requestBody) {
			// No need to request any additional information, Elasticsearch metadata is enough
		}

		@Override
		public String extract(JsonObject hit) {
			String elasticsearchIndexName = HIT_INDEX_NAME_ACCESSOR.get( hit )
					.orElseThrow( log::elasticsearchResponseMissingData );
			String mappedTypeName = mappedTypeNamesByElasticsearchIndexNames.get( elasticsearchIndexName );
			if ( mappedTypeName == null ) {
				throw log.elasticsearchResponseUnknownIndexName( elasticsearchIndexName );
			}
			return mappedTypeName;
		}
	}
}
