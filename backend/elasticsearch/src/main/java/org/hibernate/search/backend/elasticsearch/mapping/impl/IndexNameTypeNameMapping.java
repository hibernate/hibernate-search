/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.engine.backend.document.model.dsl.spi.ImplicitFieldContributor;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.IndexSchemaRootContributor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractionHelper;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionRequestContext;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * Rely on the "_index" meta-field to resolve the type name.
 * Does not work with index aliases.
 */
public class IndexNameTypeNameMapping implements TypeNameMapping {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final TypeNameFromIndexNameExtractionHelper mappedTypeNameExtractionHelper;

	private final IndexLayoutStrategy indexLayoutStrategy;

	public IndexNameTypeNameMapping(IndexLayoutStrategy indexLayoutStrategy) {
		this.indexLayoutStrategy = indexLayoutStrategy;
		this.mappedTypeNameExtractionHelper = new TypeNameFromIndexNameExtractionHelper( indexLayoutStrategy );
	}

	@Override
	public Optional<IndexSchemaRootContributor> getIndexSchemaRootContributor() {
		// No need to add anything to documents, Elasticsearch metadata is enough
		return Optional.empty();
	}

	@Override
	public Optional<DocumentMetadataContributor> getDocumentMetadataContributor(String mappedTypeName) {
		// No need to add anything to documents, Elasticsearch metadata is enough
		return Optional.empty();
	}

	@Override
	public Optional<ImplicitFieldContributor> getImplicitFieldContributor() {
		return Optional.empty();
	}

	@Override
	public void register(IndexNames indexNames, String mappedTypeName) {
		String uniqueKey = IndexNames.normalizeName(
				indexLayoutStrategy.extractUniqueKeyFromHibernateSearchIndexName(
						indexNames.hibernateSearchIndex()
				)
		);
		mappedTypeNameExtractionHelper.primaryIndexNameUniqueKeyToMappedTypeNames
				.put( uniqueKey, mappedTypeName );
	}

	@Override
	public ProjectionExtractionHelper<String> getTypeNameExtractionHelper() {
		return mappedTypeNameExtractionHelper;
	}

	private static final class TypeNameFromIndexNameExtractionHelper implements ProjectionExtractionHelper<String> {

		private static final JsonAccessor<String> HIT_INDEX_NAME_ACCESSOR =
				JsonAccessor.root().property( "_index" ).asString();

		private final IndexLayoutStrategy indexLayoutStrategy;
		private final Map<String, String> primaryIndexNameUniqueKeyToMappedTypeNames = new ConcurrentHashMap<>();

		public TypeNameFromIndexNameExtractionHelper(IndexLayoutStrategy indexLayoutStrategy) {
			this.indexLayoutStrategy = indexLayoutStrategy;
		}

		@Override
		public void request(JsonObject requestBody, ProjectionRequestContext context) {
			// No need to request any additional information, Elasticsearch metadata is enough
		}

		@Override
		public String extract(JsonObject hit, ProjectionExtractContext context) {
			String primaryIndexName = HIT_INDEX_NAME_ACCESSOR.get( hit )
					.orElseThrow( log::elasticsearchResponseMissingData );

			String mappedTypeName;
			try {
				String uniqueKey = indexLayoutStrategy.extractUniqueKeyFromElasticsearchIndexName( primaryIndexName );
				mappedTypeName = primaryIndexNameUniqueKeyToMappedTypeNames.get( uniqueKey );
				if ( mappedTypeName == null ) {
					throw log.invalidIndexUniqueKey( uniqueKey, primaryIndexNameUniqueKeyToMappedTypeNames.keySet() );
				}
			}
			catch (SearchException e) {
				throw log.elasticsearchResponseUnknownIndexName( primaryIndexName, e.getMessage(), e );
			}
			return mappedTypeName;
		}
	}
}
