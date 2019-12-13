/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.multitenancy.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.IndexSchemaRootContributor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractionHelper;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public class NoMultiTenancyStrategy implements MultiTenancyStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final NoMultiTenancyIdProjectionExtractionHelper idProjectionExtractionHelper =
			new NoMultiTenancyIdProjectionExtractionHelper();

	@Override
	public boolean isMultiTenancySupported() {
		return false;
	}

	@Override
	public Optional<IndexSchemaRootContributor> getIndexSchemaRootContributor() {
		// No need to add anything to documents, Elasticsearch metadata is enough
		return Optional.empty();
	}

	@Override
	public String toElasticsearchId(String tenantId, String id) {
		return id;
	}

	@Override
	public Optional<DocumentMetadataContributor> getDocumentMetadataContributor() {
		// No need to add anything to documents, Elasticsearch metadata is enough
		return Optional.empty();
	}

	@Override
	public JsonObject decorateJsonQuery(JsonObject originalJsonQuery, String tenantId) {
		return originalJsonQuery;
	}

	@Override
	public NoMultiTenancyIdProjectionExtractionHelper getIdProjectionExtractionHelper() {
		return idProjectionExtractionHelper;
	}

	@Override
	public void checkTenantId(String tenantId, EventContext backendContext) {
		if ( tenantId != null ) {
			throw log.tenantIdProvidedButMultiTenancyDisabled( tenantId, backendContext );
		}
	}

	private static final class NoMultiTenancyIdProjectionExtractionHelper implements ProjectionExtractionHelper<String> {
		private static final JsonAccessor<String> HIT_ID_ACCESSOR =
				JsonAccessor.root().property( "_id" ).asString();

		@Override
		public void request(JsonObject requestBody) {
			// No need to request any additional information, Elasticsearch metadata is enough
		}

		@Override
		public String extract(JsonObject hit) {
			return HIT_ID_ACCESSOR.get( hit ).orElseThrow( log::elasticsearchResponseMissingData );
		}
	}
}
