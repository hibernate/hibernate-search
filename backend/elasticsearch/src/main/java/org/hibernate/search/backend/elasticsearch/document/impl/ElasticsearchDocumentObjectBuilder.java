/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.spi.NoOpDocumentElement;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class ElasticsearchDocumentObjectBuilder implements DocumentElement {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchIndexModel model;
	private final ElasticsearchIndexSchemaObjectNode schemaNode;
	private final JsonObject content;

	public ElasticsearchDocumentObjectBuilder(ElasticsearchIndexModel model) {
		this( model, ElasticsearchIndexSchemaObjectNode.root(), new JsonObject() );
	}

	ElasticsearchDocumentObjectBuilder(ElasticsearchIndexModel model, ElasticsearchIndexSchemaObjectNode schemaNode,
			JsonObject content) {
		this.model = model;
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
		addValue( fieldSchemaNode, value );
	}

	@Override
	public DocumentElement addObject(IndexObjectFieldReference fieldReference) {
		ElasticsearchIndexObjectFieldReference elasticsearchFieldReference = (ElasticsearchIndexObjectFieldReference) fieldReference;
		if ( !elasticsearchFieldReference.isEnabled() ) {
			return NoOpDocumentElement.get();
		}

		ElasticsearchIndexSchemaObjectNode fieldSchemaNode = elasticsearchFieldReference.getSchemaNode();

		JsonObject jsonObject = new JsonObject();
		addObject( fieldSchemaNode, jsonObject );

		return new ElasticsearchDocumentObjectBuilder( model, fieldSchemaNode, jsonObject );
	}

	@Override
	public void addNullObject(IndexObjectFieldReference fieldReference) {
		ElasticsearchIndexObjectFieldReference elasticsearchFieldReference = (ElasticsearchIndexObjectFieldReference) fieldReference;
		if ( !elasticsearchFieldReference.isEnabled() ) {
			return;
		}

		ElasticsearchIndexSchemaObjectNode fieldSchemaNode = elasticsearchFieldReference.getSchemaNode();
		addObject( fieldSchemaNode, null );
	}

	@Override
	public void addValue(String relativeFieldName, Object value) {
		String absoluteFieldPath = FieldPaths.compose( schemaNode.getAbsolutePath(), relativeFieldName );
		ElasticsearchIndexSchemaFieldNode<?> node = model.getFieldNode( absoluteFieldPath );

		if ( node == null ) {
			throw log.unknownFieldForIndexing( absoluteFieldPath, model.getEventContext() );
		}

		addValueUnknownType( node, value );
	}

	@Override
	public DocumentElement addObject(String relativeFieldName) {
		String absoluteFieldPath = schemaNode.getAbsolutePath( relativeFieldName );
		ElasticsearchIndexSchemaObjectNode fieldSchemaNode = model.getObjectNode( absoluteFieldPath );

		if ( fieldSchemaNode == null ) {
			throw log.unknownFieldForIndexing( absoluteFieldPath, model.getEventContext() );
		}

		JsonObject jsonObject = new JsonObject();
		addObject( fieldSchemaNode, jsonObject );

		return new ElasticsearchDocumentObjectBuilder( model, fieldSchemaNode, jsonObject );
	}

	@Override
	public void addNullObject(String relativeFieldName) {
		String absoluteFieldPath = schemaNode.getAbsolutePath( relativeFieldName );
		ElasticsearchIndexSchemaObjectNode fieldSchemaNode = model.getObjectNode( absoluteFieldPath );

		if ( fieldSchemaNode == null ) {
			throw log.unknownFieldForIndexing( absoluteFieldPath, model.getEventContext() );
		}

		addObject( fieldSchemaNode, null );
	}

	public JsonObject build() {
		return content;
	}

	private <F> void addValue(ElasticsearchIndexSchemaFieldNode<F> node, F value) {
		ElasticsearchIndexSchemaObjectNode expectedParentNode = node.getParent();
		checkTreeConsistency( expectedParentNode );

		JsonAccessor<JsonElement> accessor = node.getRelativeAccessor();
		ElasticsearchIndexFieldType<F> type = node.getType();

		if ( !node.isMultiValued() && accessor.hasExplicitValue( content ) ) {
			throw log.multipleValuesForSingleValuedField( node.getAbsolutePath() );
		}
		accessor.add( content, type.getCodec().encode( value ) );
	}

	@SuppressWarnings("unchecked") // We check types explicitly using reflection
	private void addValueUnknownType(ElasticsearchIndexSchemaFieldNode<?> node, Object value) {
		if ( value == null ) {
			addValue( node, null );
		}
		else {
			@SuppressWarnings("rawtypes")
			ElasticsearchIndexSchemaFieldNode typeCheckedNode =
					node.withValueType( value.getClass(), model.getEventContext() );
			addValue( typeCheckedNode, value );
		}
	}

	private void addObject(ElasticsearchIndexSchemaObjectNode node, JsonObject value) {
		ElasticsearchIndexSchemaObjectNode expectedParentNode = node.getParent();
		checkTreeConsistency( expectedParentNode );

		JsonAccessor<JsonElement> accessor = node.getRelativeAccessor();

		if ( !node.isMultiValued() && accessor.hasExplicitValue( content ) ) {
			throw log.multipleValuesForSingleValuedField( node.getAbsolutePath() );
		}
		accessor.add( content, value );
	}

	private void checkTreeConsistency(ElasticsearchIndexSchemaObjectNode expectedParentNode) {
		if ( !Objects.equals( expectedParentNode, schemaNode ) ) {
			throw log.invalidFieldForDocumentElement( expectedParentNode.getAbsolutePath(), schemaNode.getAbsolutePath() );
		}
	}

}
