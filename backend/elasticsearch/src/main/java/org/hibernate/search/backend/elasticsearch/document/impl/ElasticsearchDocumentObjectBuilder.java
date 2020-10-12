/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaValueFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
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
		this( model, model.root(), new JsonObject() );
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

		ElasticsearchIndexSchemaValueFieldNode<F> fieldSchemaNode = elasticsearchFieldReference.getSchemaNode();
		addValue( fieldSchemaNode, value );
	}

	@Override
	public DocumentElement addObject(IndexObjectFieldReference fieldReference) {
		ElasticsearchIndexObjectFieldReference elasticsearchFieldReference = (ElasticsearchIndexObjectFieldReference) fieldReference;

		ElasticsearchIndexSchemaObjectFieldNode fieldSchemaNode = elasticsearchFieldReference.getSchemaNode();

		JsonObject jsonObject = new JsonObject();
		return addObject( fieldSchemaNode, jsonObject );
	}

	@Override
	public void addNullObject(IndexObjectFieldReference fieldReference) {
		ElasticsearchIndexObjectFieldReference elasticsearchFieldReference = (ElasticsearchIndexObjectFieldReference) fieldReference;

		ElasticsearchIndexSchemaObjectFieldNode fieldSchemaNode = elasticsearchFieldReference.getSchemaNode();

		addObject( fieldSchemaNode, null );
	}

	@Override
	public void addValue(String relativeFieldName, Object value) {
		String absoluteFieldPath = FieldPaths.compose( schemaNode.absolutePath(), relativeFieldName );
		AbstractElasticsearchIndexSchemaFieldNode node = model.fieldOrNull( absoluteFieldPath, IndexFieldFilter.ALL );

		if ( node == null ) {
			throw log.unknownFieldForIndexing( absoluteFieldPath, model.getEventContext() );
		}

		addValueUnknownType( node.toValueField(), value );
	}

	@Override
	public DocumentElement addObject(String relativeFieldName) {
		String absoluteFieldPath = schemaNode.absolutePath( relativeFieldName );
		AbstractElasticsearchIndexSchemaFieldNode fieldSchemaNode =
				model.fieldOrNull( absoluteFieldPath, IndexFieldFilter.ALL );

		if ( fieldSchemaNode == null ) {
			throw log.unknownFieldForIndexing( absoluteFieldPath, model.getEventContext() );
		}

		ElasticsearchIndexSchemaObjectFieldNode objectFieldSchemaNode = fieldSchemaNode.toObjectField();

		JsonObject jsonObject = new JsonObject();
		addObject( objectFieldSchemaNode, jsonObject );

		return new ElasticsearchDocumentObjectBuilder( model, objectFieldSchemaNode, jsonObject );
	}

	@Override
	public void addNullObject(String relativeFieldName) {
		String absoluteFieldPath = schemaNode.absolutePath( relativeFieldName );
		AbstractElasticsearchIndexSchemaFieldNode fieldSchemaNode =
				model.fieldOrNull( absoluteFieldPath, IndexFieldFilter.ALL );

		if ( fieldSchemaNode == null ) {
			throw log.unknownFieldForIndexing( absoluteFieldPath, model.getEventContext() );
		}

		ElasticsearchIndexSchemaObjectFieldNode objectFieldSchemaNode = fieldSchemaNode.toObjectField();

		addObject( objectFieldSchemaNode, null );
	}

	public JsonObject build() {
		return content;
	}

	private <F> void addValue(ElasticsearchIndexSchemaValueFieldNode<F> node, F value) {
		ElasticsearchIndexSchemaObjectNode expectedParentNode = node.parent();
		checkTreeConsistency( expectedParentNode );

		if ( IndexFieldInclusion.EXCLUDED.equals( node.inclusion() ) ) {
			return;
		}

		JsonAccessor<JsonElement> accessor = node.relativeAccessor();
		ElasticsearchIndexValueFieldType<F> type = node.type();

		if ( !node.multiValued() && accessor.hasExplicitValue( content ) ) {
			throw log.multipleValuesForSingleValuedField( node.absolutePath() );
		}
		accessor.add( content, type.codec().encode( value ) );
	}

	@SuppressWarnings("unchecked") // We check types explicitly using reflection
	private void addValueUnknownType(ElasticsearchIndexSchemaValueFieldNode<?> node, Object value) {
		if ( value == null ) {
			addValue( node, null );
		}
		else {
			@SuppressWarnings("rawtypes")
			ElasticsearchIndexSchemaValueFieldNode typeCheckedNode =
					node.withValueType( value.getClass(), model.getEventContext() );
			addValue( typeCheckedNode, value );
		}
	}

	private DocumentElement addObject(ElasticsearchIndexSchemaObjectFieldNode node, JsonObject value) {
		ElasticsearchIndexSchemaObjectNode expectedParentNode = node.parent();
		checkTreeConsistency( expectedParentNode );

		if ( IndexFieldInclusion.EXCLUDED.equals( node.inclusion() ) ) {
			return NoOpDocumentElement.get();
		}

		JsonAccessor<JsonElement> accessor = node.relativeAccessor();

		if ( !node.multiValued() && accessor.hasExplicitValue( content ) ) {
			throw log.multipleValuesForSingleValuedField( node.absolutePath() );
		}
		accessor.add( content, value );

		if ( value == null ) {
			return NoOpDocumentElement.get(); // Will not be used
		}
		else {
			return new ElasticsearchDocumentObjectBuilder( model, node, value );
		}
	}

	private void checkTreeConsistency(ElasticsearchIndexSchemaObjectNode expectedParentNode) {
		if ( !Objects.equals( expectedParentNode, schemaNode ) ) {
			throw log.invalidFieldForDocumentElement( expectedParentNode.absolutePath(), schemaNode.absolutePath() );
		}
	}

}
