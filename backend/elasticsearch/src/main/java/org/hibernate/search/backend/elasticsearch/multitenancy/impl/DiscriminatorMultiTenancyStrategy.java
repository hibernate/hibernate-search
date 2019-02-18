/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.multitenancy.impl;

import java.lang.invoke.MethodHandles;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.impl.ElasticsearchFields;
import org.hibernate.search.util.reporting.EventContext;
import org.hibernate.search.util.impl.common.LoggerFactory;

import com.google.gson.JsonObject;

public class DiscriminatorMultiTenancyStrategy implements MultiTenancyStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final Pattern UNDERSCORE_PATTERN = Pattern.compile( "_" );

	private static final String ESCAPED_UNDERSCORE = "__";

	private static final JsonAccessor<String> ID_ACCESSOR = JsonAccessor.root().property( ElasticsearchFields.idFieldName() ).asString();

	private static final JsonAccessor<String> TENANT_ID_ACCESSOR = JsonAccessor.root().property( ElasticsearchFields.tenantIdFieldName() ).asString();

	private static final JsonAccessor<String> STORED_FIELDS_ACCESSOR = JsonAccessor.root().property( "stored_fields" ).asString();

	private static final JsonAccessor<String> HIT_ID_ACCESSOR = JsonAccessor.root().property( "fields" ).asObject()
			.property( ElasticsearchFields.idFieldName() ).asArray()
			.element( 0 ).asString();

	@Override
	public boolean isMultiTenancySupported() {
		return true;
	}

	@Override
	public void contributeToMapping(RootTypeMapping rootTypeMapping) {
		PropertyMapping idPropertyMapping = new PropertyMapping();
		idPropertyMapping.setIndex( true );
		idPropertyMapping.setStore( Boolean.TRUE );
		idPropertyMapping.setType( DataType.KEYWORD );
		rootTypeMapping.addProperty( ElasticsearchFields.idFieldName(), idPropertyMapping );

		PropertyMapping tenantIdPropertyMapping = new PropertyMapping();
		tenantIdPropertyMapping.setIndex( true );
		tenantIdPropertyMapping.setStore( Boolean.TRUE );
		tenantIdPropertyMapping.setType( DataType.KEYWORD );
		rootTypeMapping.addProperty( ElasticsearchFields.tenantIdFieldName(), tenantIdPropertyMapping );
	}

	@Override
	public String toElasticsearchId(String tenantId, String id) {
		return UNDERSCORE_PATTERN.matcher( tenantId ).replaceAll( ESCAPED_UNDERSCORE ) + "_" + UNDERSCORE_PATTERN.matcher( id ).replaceAll( ESCAPED_UNDERSCORE );
	}

	@Override
	public void contributeToIndexedDocument(JsonObject document, String tenantId, String id) {
		ID_ACCESSOR.set( document, id );
		TENANT_ID_ACCESSOR.set( document, tenantId );
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
	public void contributeToSearchRequest(JsonObject requestBody) {
		STORED_FIELDS_ACCESSOR.add( requestBody, ElasticsearchFields.idFieldName() );
	}

	@Override
	public String extractTenantScopedDocumentId(JsonObject hit) {
		return HIT_ID_ACCESSOR.get( hit ).orElseThrow( log::elasticsearchResponseMissingData );
	}

	@Override
	public void checkTenantId(String tenantId, EventContext backendContext) {
		if ( tenantId == null ) {
			throw log.multiTenancyEnabledButNoTenantIdProvided( backendContext );
		}
	}
}
