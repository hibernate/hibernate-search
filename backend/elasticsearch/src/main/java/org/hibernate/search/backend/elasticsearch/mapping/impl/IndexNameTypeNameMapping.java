/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.mapping.impl;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.IndexSchemaRootContributor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractionHelper;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionRequestContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.ImplicitFieldContributor;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;

import com.google.gson.JsonObject;

/**
 * Rely on the "_index" meta-field to resolve the type name.
 * Does not work with index aliases.
 */
public class IndexNameTypeNameMapping implements TypeNameMapping {

	private TypeNameFromIndexNameExtractionHelper mappedTypeNameExtractionHelper;
	private IndexLayoutStrategy indexLayoutStrategy;

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
	public ProjectionExtractionHelper<String> onStart(IndexLayoutStrategy indexLayoutStrategy) {
		this.indexLayoutStrategy = indexLayoutStrategy;
		this.mappedTypeNameExtractionHelper = new TypeNameFromIndexNameExtractionHelper( indexLayoutStrategy );
		return this.mappedTypeNameExtractionHelper;
	}

	@Override
	public void register(IndexNames indexNames, String mappedTypeName) {
		if ( indexLayoutStrategy == null ) {
			throw new AssertionFailure( "On start was not called yet. Cannot register an index before starting the backend." );
		}
		String uniqueKey = IndexNames.normalizeName(
				indexLayoutStrategy.extractUniqueKeyFromHibernateSearchIndexName(
						indexNames.hibernateSearchIndex()
				)
		);
		mappedTypeNameExtractionHelper.primaryIndexNameUniqueKeyToMappedTypeNames
				.put( uniqueKey, mappedTypeName );
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
					.orElseThrow( ElasticsearchClientLog.INSTANCE::elasticsearchResponseMissingData );

			String mappedTypeName;
			try {
				String uniqueKey = indexLayoutStrategy.extractUniqueKeyFromElasticsearchIndexName( primaryIndexName );
				mappedTypeName = primaryIndexNameUniqueKeyToMappedTypeNames.get( uniqueKey );
				if ( mappedTypeName == null ) {
					throw ElasticsearchClientLog.INSTANCE.invalidIndexUniqueKey( uniqueKey,
							primaryIndexNameUniqueKeyToMappedTypeNames.keySet() );
				}
			}
			catch (SearchException e) {
				throw ElasticsearchClientLog.INSTANCE.elasticsearchResponseUnknownIndexName( primaryIndexName, e.getMessage(),
						e );
			}
			return mappedTypeName;
		}
	}
}
