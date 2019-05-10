/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.spi.NoOpDocumentElement;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchDocumentObjectBuilder implements DocumentElement {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchIndexSchemaObjectNode schemaNode;
	private final JsonObject content;

	public ElasticsearchDocumentObjectBuilder() {
		this( ElasticsearchIndexSchemaObjectNode.root(), new JsonObject() );
	}

	ElasticsearchDocumentObjectBuilder(ElasticsearchIndexSchemaObjectNode schemaNode, JsonObject content) {
		this.schemaNode = schemaNode;
		this.content = content;
	}

	@Override
	public <F> void addValue(IndexFieldReference<F> fieldReference, F value) {
		ElasticsearchIndexFieldReference<F> elasticsearchFieldReference = (ElasticsearchIndexFieldReference<F>) fieldReference;
		if ( !elasticsearchFieldReference.isEnabled() ) {
			return;
		}

		ElasticsearchIndexSchemaFieldNode<F> fieldSchemaNode = elasticsearchFieldReference.getSchemaNode();
		checkTreeConsistency( fieldSchemaNode.getParent() );
		if ( !fieldSchemaNode.isMultiValued() && elasticsearchFieldReference.hasValueIn( content ) ) {
			throw log.multipleValuesForSingleValuedField( fieldSchemaNode.getAbsolutePath() );
		}

		elasticsearchFieldReference.addTo( content, value );
	}

	@Override
	public DocumentElement addObject(IndexObjectFieldReference fieldReference) {
		ElasticsearchIndexObjectFieldReference elasticsearchFieldReference = (ElasticsearchIndexObjectFieldReference) fieldReference;
		if ( !elasticsearchFieldReference.isEnabled() ) {
			return NoOpDocumentElement.get();
		}

		ElasticsearchIndexSchemaObjectNode fieldSchemaNode = elasticsearchFieldReference.getSchemaNode();
		checkTreeConsistency( fieldSchemaNode.getParent() );
		if ( !fieldSchemaNode.isMultiValued() && elasticsearchFieldReference.hasValueIn( content ) ) {
			throw log.multipleValuesForSingleValuedField( fieldSchemaNode.getAbsolutePath() );
		}

		JsonObject jsonObject = new JsonObject();
		elasticsearchFieldReference.addTo( content, jsonObject );

		return new ElasticsearchDocumentObjectBuilder( fieldSchemaNode, jsonObject );
	}

	@Override
	public void addNullObject(IndexObjectFieldReference fieldReference) {
		ElasticsearchIndexObjectFieldReference elasticsearchFieldReference = (ElasticsearchIndexObjectFieldReference) fieldReference;
		if ( !elasticsearchFieldReference.isEnabled() ) {
			return;
		}

		ElasticsearchIndexSchemaObjectNode fieldSchemaNode = elasticsearchFieldReference.getSchemaNode();
		checkTreeConsistency( fieldSchemaNode.getParent() );
		if ( !fieldSchemaNode.isMultiValued() && elasticsearchFieldReference.hasValueIn( content ) ) {
			throw log.multipleValuesForSingleValuedField( fieldSchemaNode.getAbsolutePath() );
		}

		elasticsearchFieldReference.addTo( content, null );
	}

	private void checkTreeConsistency(ElasticsearchIndexSchemaObjectNode expectedParentNode) {
		if ( !Objects.equals( expectedParentNode, schemaNode ) ) {
			throw log.invalidFieldForDocumentElement( expectedParentNode.getAbsolutePath(), schemaNode.getAbsolutePath() );
		}
	}

	public JsonObject build(MultiTenancyStrategy multiTenancyStrategy, String tenantId, String id) {
		multiTenancyStrategy.contributeToIndexedDocument( content, tenantId, id );

		return content;
	}

}
