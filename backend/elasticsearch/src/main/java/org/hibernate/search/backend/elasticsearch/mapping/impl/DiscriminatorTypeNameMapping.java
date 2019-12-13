/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.IndexSchemaRootContributor;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractionHelper;
import org.hibernate.search.backend.elasticsearch.util.impl.ElasticsearchFields;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Rely on a discriminator field added to each document to resolve the type name.
 * Works correctly with index aliases.
 */
public class DiscriminatorTypeNameMapping implements TypeNameMapping {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String MAPPED_TYPE_FIELD_NAME = ElasticsearchFields.internalFieldName( "type" );

	private final TypeNameDiscriminatorSchemaRootContributor schemaRootContributor =
			new TypeNameDiscriminatorSchemaRootContributor();

	private final TypeNameFromDiscriminatorExtractionHelper typeNameExtractionHelper =
			new TypeNameFromDiscriminatorExtractionHelper();

	@Override
	public Optional<IndexSchemaRootContributor> getIndexSchemaRootContributor() {
		return Optional.of( schemaRootContributor );
	}

	@Override
	public Optional<DocumentMetadataContributor> getDocumentMetadataContributor(String mappedTypeName) {
		return Optional.of( new TypeNameDiscriminatorContributor( mappedTypeName ) );
	}

	@Override
	public void register(String elasticsearchIndexName, String mappedTypeName) {
		// Nothing to do
	}

	@Override
	public ProjectionExtractionHelper<String> getTypeNameExtractionHelper() {
		return typeNameExtractionHelper;
	}

	private static final class TypeNameDiscriminatorSchemaRootContributor
			implements IndexSchemaRootContributor {
		@Override
		public void contribute(RootTypeMapping rootTypeMapping) {
			PropertyMapping mappedTypePropertyMapping = new PropertyMapping();
			mappedTypePropertyMapping.setType( DataTypes.KEYWORD );
			mappedTypePropertyMapping.setIndex( false );
			mappedTypePropertyMapping.setStore( false );
			mappedTypePropertyMapping.setDocValues( true );
			rootTypeMapping.addProperty( MAPPED_TYPE_FIELD_NAME, mappedTypePropertyMapping );
		}
	}

	private static final class TypeNameDiscriminatorContributor
			implements DocumentMetadataContributor {
		private static final JsonAccessor<String> DOCUMENT_MAPPED_TYPE_FIELD_ACCESSOR =
				JsonAccessor.root().property( MAPPED_TYPE_FIELD_NAME ).asString();

		private final String mappedTypeName;

		private TypeNameDiscriminatorContributor(String mappedTypeName) {
			this.mappedTypeName = mappedTypeName;
		}

		@Override
		public void contribute(JsonObject document, String tenantId, String id) {
			DOCUMENT_MAPPED_TYPE_FIELD_ACCESSOR.set( document, mappedTypeName );
		}
	}

	private static final class TypeNameFromDiscriminatorExtractionHelper implements ProjectionExtractionHelper<String> {
		private static final JsonArrayAccessor DOCVALUE_FIELDS_ACCESSOR =
				JsonAccessor.root().property( "docvalue_fields" ).asArray();
		private static final JsonAccessor<String> HIT_MAPPED_TYPE_NAME_ACCESSOR =
				JsonAccessor.root().property( "fields" ).asObject()
						.property( MAPPED_TYPE_FIELD_NAME ).asArray()
						.element( 0 ).asString();
		private static final JsonElement MAPPED_TYPE_FIELD_NAME_JSON = new JsonPrimitive( MAPPED_TYPE_FIELD_NAME );


		@Override
		public void request(JsonObject requestBody) {
			DOCVALUE_FIELDS_ACCESSOR.addElementIfAbsent( requestBody, MAPPED_TYPE_FIELD_NAME_JSON );
		}

		@Override
		public String extract(JsonObject hit) {
			return HIT_MAPPED_TYPE_NAME_ACCESSOR.get( hit )
					.orElseThrow( () -> log.missingTypeFieldInDocument( MAPPED_TYPE_FIELD_NAME ) );
		}
	}
}
