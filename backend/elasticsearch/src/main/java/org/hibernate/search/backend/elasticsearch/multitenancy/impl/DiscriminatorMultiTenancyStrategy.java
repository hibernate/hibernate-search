/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.multitenancy.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.IndexSchemaRootContributor;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractionHelper;
import org.hibernate.search.backend.elasticsearch.util.impl.ElasticsearchFields;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class DiscriminatorMultiTenancyStrategy implements MultiTenancyStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final Pattern UNDERSCORE_PATTERN = Pattern.compile( "_" );

	private static final String ESCAPED_UNDERSCORE = "__";

	private final DiscriminatorMultiTenancyDocumentMetadataContributor documentMetadataContributor =
			new DiscriminatorMultiTenancyDocumentMetadataContributor();

	private final DiscriminatorMultiTenancyIndexSchemaRootContributor schemaRootContributor =
			new DiscriminatorMultiTenancyIndexSchemaRootContributor();

	private final DiscriminatorMultiTenancyIdProjectionExtractionHelper idProjectionExtractionHelper =
			new DiscriminatorMultiTenancyIdProjectionExtractionHelper();

	@Override
	public boolean isMultiTenancySupported() {
		return true;
	}

	@Override
	public Optional<IndexSchemaRootContributor> getIndexSchemaRootContributor() {
		return Optional.of( schemaRootContributor );
	}

	@Override
	public String toElasticsearchId(String tenantId, String id) {
		return UNDERSCORE_PATTERN.matcher( tenantId ).replaceAll( ESCAPED_UNDERSCORE ) + "_" + UNDERSCORE_PATTERN.matcher( id ).replaceAll( ESCAPED_UNDERSCORE );
	}

	@Override
	public Optional<DocumentMetadataContributor> getDocumentMetadataContributor() {
		return Optional.of( documentMetadataContributor );
	}

	@Override
	public JsonObject decorateJsonQuery(JsonObject originalJsonQuery, String tenantId) {
		JsonObject jsonQuery = new JsonObject();
		JsonObjectAccessor boolQuery = JsonAccessor.root().property( "bool" ).asObject();
		boolQuery.property( "filter" ).asObject()
				.property( "term" ).asObject()
				.property( ElasticsearchFields.tenantIdFieldName() ).asString()
				.set( jsonQuery, tenantId );

		if ( originalJsonQuery != null ) {
			boolQuery.property( "must" ).set( jsonQuery, originalJsonQuery );
		}

		return jsonQuery;
	}

	@Override
	public DiscriminatorMultiTenancyIdProjectionExtractionHelper getIdProjectionExtractionHelper() {
		return idProjectionExtractionHelper;
	}

	@Override
	public void checkTenantId(String tenantId, EventContext backendContext) {
		if ( tenantId == null ) {
			throw log.multiTenancyEnabledButNoTenantIdProvided( backendContext );
		}
	}

	private static class DiscriminatorMultiTenancyIndexSchemaRootContributor implements IndexSchemaRootContributor {
		@Override
		public void contribute(RootTypeMapping rootTypeMapping) {
			PropertyMapping idPropertyMapping = new PropertyMapping();
			idPropertyMapping.setType( DataTypes.KEYWORD );
			idPropertyMapping.setIndex( false );
			idPropertyMapping.setStore( false );
			idPropertyMapping.setDocValues( true );
			rootTypeMapping.addProperty( ElasticsearchFields.idFieldName(), idPropertyMapping );

			PropertyMapping tenantIdPropertyMapping = new PropertyMapping();
			tenantIdPropertyMapping.setType( DataTypes.KEYWORD );
			tenantIdPropertyMapping.setIndex( true );
			tenantIdPropertyMapping.setStore( false );
			tenantIdPropertyMapping.setDocValues( true );
			rootTypeMapping.addProperty( ElasticsearchFields.tenantIdFieldName(), tenantIdPropertyMapping );
		}
	}

	private static class DiscriminatorMultiTenancyDocumentMetadataContributor implements DocumentMetadataContributor {
		private static final JsonAccessor<String> TENANT_ID_ACCESSOR =
				JsonAccessor.root().property( ElasticsearchFields.tenantIdFieldName() ).asString();
		private static final JsonAccessor<String> ID_ACCESSOR =
				JsonAccessor.root().property( ElasticsearchFields.idFieldName() ).asString();

		@Override
		public void contribute(JsonObject document, String tenantId, String id) {
			TENANT_ID_ACCESSOR.set( document, tenantId );
			ID_ACCESSOR.set( document, id );
		}
	}

	private static final class DiscriminatorMultiTenancyIdProjectionExtractionHelper implements ProjectionExtractionHelper<String> {
		private static final JsonArrayAccessor DOCVALUE_FIELDS_ACCESSOR =
				JsonAccessor.root().property( "docvalue_fields" ).asArray();

		private static final JsonAccessor<String> HIT_ID_ACCESSOR =
				JsonAccessor.root().property( "fields" ).asObject()
						.property( ElasticsearchFields.idFieldName() ).asArray()
						.element( 0 ).asString();

		private static final JsonElement ID_FIELD_NAME_JSON =
				new JsonPrimitive( ElasticsearchFields.idFieldName() );

		@Override
		public void request(JsonObject requestBody) {
			DOCVALUE_FIELDS_ACCESSOR.addElementIfAbsent( requestBody, ID_FIELD_NAME_JSON );
		}

		@Override
		public String extract(JsonObject hit) {
			return HIT_ID_ACCESSOR.get( hit ).orElseThrow( log::elasticsearchResponseMissingData );
		}
	}
}
