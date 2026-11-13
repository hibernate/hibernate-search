package org.hibernate.search.backend.elasticsearch.multitenancy.impl;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.common.impl.DocumentIdHelper;
import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.IndexSchemaRootContributor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.logging.spi.ConfigurationLog;
import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.MetadataFields;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractionHelper;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionRequestContext;
import org.hibernate.search.util.common.reporting.EventContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public class DiscriminatorMultiTenancyStrategy implements MultiTenancyStrategy {

	private static final String ID_FIELD_NAME = MetadataFields.internalFieldName( "tenant_doc_id" );

	private static final String TENANT_ID_FIELD_NAME = MetadataFields.internalFieldName( "tenant_id" );

	private final DiscriminatorMultiTenancyIndexSchemaRootContributor schemaRootContributor =
			new DiscriminatorMultiTenancyIndexSchemaRootContributor();

	private final DiscriminatorMultiTenancyElasticsearchDocumentIdHelper documentIdHelper =
			new DiscriminatorMultiTenancyElasticsearchDocumentIdHelper();

	private final DiscriminatorMultiTenancyDocumentMetadataContributor documentMetadataContributor =
			new DiscriminatorMultiTenancyDocumentMetadataContributor();

	private final DiscriminatorMultiTenancyIdProjectionExtractionHelper idProjectionExtractionHelper =
			new DiscriminatorMultiTenancyIdProjectionExtractionHelper();

	@Override
	public Optional<IndexSchemaRootContributor> indexSchemaRootContributor() {
		return Optional.of( schemaRootContributor );
	}

	@Override
	public DocumentIdHelper documentIdHelper() {
		return documentIdHelper;
	}

	@Override
	public Optional<DocumentMetadataContributor> documentMetadataContributor() {
		return Optional.of( documentMetadataContributor );
	}

	@Override
	public ObjectNode filterOrNull(String tenantId) {
		return Queries.term( TENANT_ID_FIELD_NAME, tenantId );
	}

	@Override
	public ObjectNode filterOrNull(Set<String> tenantIds) {
		return Queries.anyTerm( TENANT_ID_FIELD_NAME, tenantIds );
	}

	@Override
	public DiscriminatorMultiTenancyIdProjectionExtractionHelper idProjectionExtractionHelper() {
		return idProjectionExtractionHelper;
	}

	private static class DiscriminatorMultiTenancyIndexSchemaRootContributor implements IndexSchemaRootContributor {

		@Override
		public void contribute(RootTypeMapping rootTypeMapping) {
			PropertyMapping idPropertyMapping = new PropertyMapping();
			idPropertyMapping.setType( DataTypes.KEYWORD );
			idPropertyMapping.setIndex( false );
			idPropertyMapping.setDocValues( true );
			rootTypeMapping.addProperty( ID_FIELD_NAME, idPropertyMapping );
			PropertyMapping tenantIdPropertyMapping = new PropertyMapping();
			tenantIdPropertyMapping.setType( DataTypes.KEYWORD );
			tenantIdPropertyMapping.setIndex( true );
			tenantIdPropertyMapping.setDocValues( true );
			rootTypeMapping.addProperty( TENANT_ID_FIELD_NAME, tenantIdPropertyMapping );
		}
	}

	private static final class DiscriminatorMultiTenancyElasticsearchDocumentIdHelper implements DocumentIdHelper {

		private static final Pattern UNDERSCORE_PATTERN = Pattern.compile( "_" );

		private static final String ESCAPED_UNDERSCORE = "__";

		@Override
		public void checkTenantId(String tenantId, EventContext backendContext) {
			if ( tenantId == null ) {
				throw ConfigurationLog.INSTANCE.multiTenancyEnabledButNoTenantIdProvided( backendContext );
			}
		}

		@Override
		public void checkTenantId(Set<String> tenantIds, EventContext context) {
			if ( tenantIds == null || tenantIds.isEmpty() ) {
				throw ConfigurationLog.INSTANCE.multiTenancyEnabledButNoTenantIdProvided( context );
			}
		}

		@Override
		public String toElasticsearchId(String tenantId, String id) {
			return UNDERSCORE_PATTERN.matcher( tenantId ).replaceAll( ESCAPED_UNDERSCORE ) + "_"
					+ UNDERSCORE_PATTERN.matcher( id ).replaceAll( ESCAPED_UNDERSCORE );
		}
	}

	private static class DiscriminatorMultiTenancyDocumentMetadataContributor implements DocumentMetadataContributor {

		private static final JsonAccessor<String> TENANT_ID_ACCESSOR =
				JsonAccessor.root().property( TENANT_ID_FIELD_NAME ).unwrapSingletonArrays().asString();

		private static final JsonAccessor<String> ID_ACCESSOR =
				JsonAccessor.root().property( ID_FIELD_NAME ).unwrapSingletonArrays().asString();

		@Override
		public void contribute(ObjectNode document, String tenantId, String id) {
			TENANT_ID_ACCESSOR.set( document, tenantId );
			ID_ACCESSOR.set( document, id );
		}
	}

	private static final class DiscriminatorMultiTenancyIdProjectionExtractionHelper
			implements ProjectionExtractionHelper<String> {

		private static final JsonAccessor<String> HIT_ID_ACCESSOR =
				JsonAccessor.root().property( "fields" ).unwrapSingletonArrays().asObject().property( ID_FIELD_NAME ).asArray()
						.element( 0 ).unwrapSingletonArrays().asString();

		private static final JsonNode ID_FIELD_NAME_JSON = JsonScalarConversions.toNode( ID_FIELD_NAME );

		@Override
		public void request(ObjectNode requestBody, ProjectionRequestContext context) {
			context.root().getSearchSyntax().requestDocValues( requestBody, ID_FIELD_NAME_JSON );
		}

		@Override
		public String extract(ObjectNode hit, ProjectionExtractContext context) {
			return HIT_ID_ACCESSOR.get( hit ).orElseThrow( ElasticsearchClientLog.INSTANCE::elasticsearchResponseMissingData );
		}
	}
}
