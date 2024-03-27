/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.multitenancy.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.common.impl.DocumentIdHelper;
import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.IndexSchemaRootContributor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractionHelper;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionRequestContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.JsonObject;

public class NoMultiTenancyStrategy implements MultiTenancyStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final NoMultiTenancyElasticsearchDocumentIdHelper documentIdHelper =
			new NoMultiTenancyElasticsearchDocumentIdHelper();
	private final NoMultiTenancyIdProjectionExtractionHelper idProjectionExtractionHelper =
			new NoMultiTenancyIdProjectionExtractionHelper();

	@Override
	public Optional<IndexSchemaRootContributor> indexSchemaRootContributor() {
		// No need to add anything to documents, Elasticsearch metadata is enough
		return Optional.empty();
	}

	@Override
	public DocumentIdHelper documentIdHelper() {
		return documentIdHelper;
	}

	@Override
	public Optional<DocumentMetadataContributor> documentMetadataContributor() {
		// No need to add anything to documents, Elasticsearch metadata is enough
		return Optional.empty();
	}

	@Override
	public JsonObject filterOrNull(String tenantId) {
		// No need for a filter
		return null;
	}

	@Override
	public JsonObject filterOrNull(Set<String> tenantIds) {
		// No need for a filter
		return null;
	}

	@Override
	public NoMultiTenancyIdProjectionExtractionHelper idProjectionExtractionHelper() {
		return idProjectionExtractionHelper;
	}

	private static final class NoMultiTenancyElasticsearchDocumentIdHelper implements DocumentIdHelper {
		@Override
		public void checkTenantId(String tenantId, EventContext backendContext) {
			if ( tenantId != null ) {
				throw log.tenantIdProvidedButMultiTenancyDisabled( Collections.singleton( tenantId ), backendContext );
			}
		}

		@Override
		public void checkTenantId(Set<String> tenantIds, EventContext context) {
			if ( tenantIds != null && !tenantIds.isEmpty() ) {
				throw log.tenantIdProvidedButMultiTenancyDisabled( tenantIds, context );
			}
		}

		@Override
		public String toElasticsearchId(String tenantId, String id) {
			return id;
		}
	}

	private static final class NoMultiTenancyIdProjectionExtractionHelper
			implements ProjectionExtractionHelper<String> {
		private static final JsonAccessor<String> HIT_ID_ACCESSOR =
				JsonAccessor.root().property( "_id" ).asString();

		@Override
		public void request(JsonObject requestBody, ProjectionRequestContext context) {
			// No need to request any additional information, Elasticsearch metadata is enough
		}

		@Override
		public String extract(JsonObject hit, ProjectionExtractContext context) {
			return HIT_ID_ACCESSOR.get( hit ).orElseThrow( log::elasticsearchResponseMissingData );
		}
	}
}
