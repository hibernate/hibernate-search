package org.hibernate.search.backend.elasticsearch.document.impl;

import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexCompositeNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexObjectField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexValueField;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonUtils;
import org.hibernate.search.backend.elasticsearch.logging.impl.IndexingLog;
import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.document.spi.NoOpDocumentElement;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class ElasticsearchDocumentObjectBuilder implements DocumentElement {

	private final ElasticsearchIndexModel model;

	private final ElasticsearchIndexCompositeNode schemaNode;

	private final ObjectNode content;

	public ElasticsearchDocumentObjectBuilder(ElasticsearchIndexModel model) {
		this( model, model.root(), new ObjectNode( JsonNodeFactory.instance ) );
	}

	ElasticsearchDocumentObjectBuilder(ElasticsearchIndexModel model, ElasticsearchIndexCompositeNode schemaNode,
			ObjectNode content) {
		this.model = model;
		this.schemaNode = schemaNode;
		this.content = content;
	}

	@Override
	public <F> void addValue(IndexFieldReference<F> fieldReference, F value) {
		ElasticsearchIndexFieldReference<F> elasticsearchFieldReference = (ElasticsearchIndexFieldReference<F>) fieldReference;
		ElasticsearchIndexValueField<F> fieldSchemaNode = elasticsearchFieldReference.getSchemaNode();
		addValue( fieldSchemaNode, value );
	}

	@Override
	public DocumentElement addObject(IndexObjectFieldReference fieldReference) {
		ElasticsearchIndexObjectFieldReference elasticsearchFieldReference =
				(ElasticsearchIndexObjectFieldReference) fieldReference;
		ElasticsearchIndexObjectField fieldSchemaNode = elasticsearchFieldReference.getSchemaNode();
		ObjectNode jsonObject = new ObjectNode( JsonNodeFactory.instance );
		return addObject( fieldSchemaNode, jsonObject );
	}

	@Override
	public void addNullObject(IndexObjectFieldReference fieldReference) {
		ElasticsearchIndexObjectFieldReference elasticsearchFieldReference =
				(ElasticsearchIndexObjectFieldReference) fieldReference;
		ElasticsearchIndexObjectField fieldSchemaNode = elasticsearchFieldReference.getSchemaNode();
		addObject( fieldSchemaNode, null );
	}

	@Override
	public void addValue(String relativeFieldName, Object value) {
		String absoluteFieldPath = FieldPaths.compose( schemaNode.absolutePath(), relativeFieldName );
		ElasticsearchIndexField node = model.fieldOrNull( absoluteFieldPath, IndexFieldFilter.ALL );
		if ( node == null ) {
			throw IndexingLog.INSTANCE.unknownFieldForIndexing( absoluteFieldPath, model.eventContext() );
		}
		addValueUnknownType( node.toValueField(), value );
	}

	@Override
	public DocumentElement addObject(String relativeFieldName) {
		String absoluteFieldPath = schemaNode.absolutePath( relativeFieldName );
		ElasticsearchIndexField fieldSchemaNode = model.fieldOrNull( absoluteFieldPath, IndexFieldFilter.ALL );
		if ( fieldSchemaNode == null ) {
			throw IndexingLog.INSTANCE.unknownFieldForIndexing( absoluteFieldPath, model.eventContext() );
		}
		ElasticsearchIndexObjectField objectFieldSchemaNode = fieldSchemaNode.toObjectField();
		ObjectNode jsonObject = new ObjectNode( JsonNodeFactory.instance );
		addObject( objectFieldSchemaNode, jsonObject );
		return new ElasticsearchDocumentObjectBuilder( model, objectFieldSchemaNode, jsonObject );
	}

	@Override
	public void addNullObject(String relativeFieldName) {
		String absoluteFieldPath = schemaNode.absolutePath( relativeFieldName );
		ElasticsearchIndexField fieldSchemaNode = model.fieldOrNull( absoluteFieldPath, IndexFieldFilter.ALL );
		if ( fieldSchemaNode == null ) {
			throw IndexingLog.INSTANCE.unknownFieldForIndexing( absoluteFieldPath, model.eventContext() );
		}
		ElasticsearchIndexObjectField objectFieldSchemaNode = fieldSchemaNode.toObjectField();
		addObject( objectFieldSchemaNode, null );
	}

	public ObjectNode build() {
		return content;
	}

	private <F> void addValue(ElasticsearchIndexValueField<F> node, F value) {
		ElasticsearchIndexCompositeNode expectedParentNode = node.parent();
		checkTreeConsistency( expectedParentNode );
		if ( TreeNodeInclusion.EXCLUDED.equals( node.inclusion() ) ) {
			return;
		}
		ElasticsearchIndexValueFieldType<F> type = node.type();
		String jsonPropertyName = node.relativeName();
		if ( !node.multiValued() && content.has( jsonPropertyName ) ) {
			throw IndexingLog.INSTANCE.multipleValuesForSingleValuedField( node.absolutePath() );
		}
		JsonNode encoded = type.codec().encode( value );
		if ( node.multiValued() && type.codec().canDecodeArrays() ) {
			// The outer array holds field values; an encoded array is one of those values.
			ArrayNode values = (ArrayNode) content.get( jsonPropertyName );
			if ( values == null ) {
				values = content.putArray( jsonPropertyName );
			}
			values.add( encoded );
		}
		else if ( node.multiValued() && !content.has( jsonPropertyName ) && encoded.isArray() ) {
			// Split-mode accumulation owns its root array. Never append into a borrowed input,
			// especially when a caller indexes the same array again. Nested values remain borrowed.
			ArrayNode values = content.putArray( jsonPropertyName );
			values.addAll( (ArrayNode) encoded );
		}
		else {
			JsonUtils.setOrAppendToArray( content, jsonPropertyName, encoded );
		}
	}

	// We check types explicitly using reflection
	@SuppressWarnings("unchecked")
	private void addValueUnknownType(ElasticsearchIndexValueField<?> node, Object value) {
		if ( value == null ) {
			addValue( node, null );
		}
		else {
			@SuppressWarnings("rawtypes")
			ElasticsearchIndexValueField typeCheckedNode = node.withValueType( value.getClass(), model.eventContext() );
			addValue( typeCheckedNode, value );
		}
	}

	private DocumentElement addObject(ElasticsearchIndexObjectField node, ObjectNode value) {
		ElasticsearchIndexCompositeNode expectedParentNode = node.parent();
		checkTreeConsistency( expectedParentNode );
		if ( TreeNodeInclusion.EXCLUDED.equals( node.inclusion() ) ) {
			return NoOpDocumentElement.get();
		}
		String jsonPropertyName = node.relativeName();
		if ( !node.multiValued() && content.has( jsonPropertyName ) ) {
			throw IndexingLog.INSTANCE.multipleValuesForSingleValuedField( node.absolutePath() );
		}
		JsonUtils.setOrAppendToArray( content, jsonPropertyName, value );
		if ( value == null ) {
			// Will not be used
			return NoOpDocumentElement.get();
		}
		else {
			return new ElasticsearchDocumentObjectBuilder( model, node, value );
		}
	}

	private void checkTreeConsistency(ElasticsearchIndexCompositeNode expectedParentNode) {
		if ( !Objects.equals( expectedParentNode, schemaNode ) ) {
			throw ElasticsearchClientLog.INSTANCE.invalidFieldForDocumentElement( expectedParentNode.absolutePath(),
					schemaNode.absolutePath() );
		}
	}
}
