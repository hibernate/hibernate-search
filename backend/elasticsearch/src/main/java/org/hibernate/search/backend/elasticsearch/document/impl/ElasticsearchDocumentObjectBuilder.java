/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexCompositeNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexObjectField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexValueField;
import org.hibernate.search.backend.elasticsearch.gson.impl.GsonUtils;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.document.spi.NoOpDocumentElement;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public class ElasticsearchDocumentObjectBuilder implements DocumentElement {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchIndexModel model;
	private final ElasticsearchIndexCompositeNode schemaNode;
	private final JsonObject content;

	public ElasticsearchDocumentObjectBuilder(ElasticsearchIndexModel model) {
		this( model, model.root(), new JsonObject() );
	}

	ElasticsearchDocumentObjectBuilder(ElasticsearchIndexModel model, ElasticsearchIndexCompositeNode schemaNode,
			JsonObject content) {
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

		JsonObject jsonObject = new JsonObject();
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
			throw log.unknownFieldForIndexing( absoluteFieldPath, model.eventContext() );
		}

		addValueUnknownType( node.toValueField(), value );
	}

	@Override
	public DocumentElement addObject(String relativeFieldName) {
		String absoluteFieldPath = schemaNode.absolutePath( relativeFieldName );
		ElasticsearchIndexField fieldSchemaNode =
				model.fieldOrNull( absoluteFieldPath, IndexFieldFilter.ALL );

		if ( fieldSchemaNode == null ) {
			throw log.unknownFieldForIndexing( absoluteFieldPath, model.eventContext() );
		}

		ElasticsearchIndexObjectField objectFieldSchemaNode = fieldSchemaNode.toObjectField();

		JsonObject jsonObject = new JsonObject();
		addObject( objectFieldSchemaNode, jsonObject );

		return new ElasticsearchDocumentObjectBuilder( model, objectFieldSchemaNode, jsonObject );
	}

	@Override
	public void addNullObject(String relativeFieldName) {
		String absoluteFieldPath = schemaNode.absolutePath( relativeFieldName );
		ElasticsearchIndexField fieldSchemaNode =
				model.fieldOrNull( absoluteFieldPath, IndexFieldFilter.ALL );

		if ( fieldSchemaNode == null ) {
			throw log.unknownFieldForIndexing( absoluteFieldPath, model.eventContext() );
		}

		ElasticsearchIndexObjectField objectFieldSchemaNode = fieldSchemaNode.toObjectField();

		addObject( objectFieldSchemaNode, null );
	}

	public JsonObject build() {
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
			throw log.multipleValuesForSingleValuedField( node.absolutePath() );
		}
		GsonUtils.setOrAppendToArray( content, jsonPropertyName, type.codec().encode( value ) );
	}

	@SuppressWarnings("unchecked") // We check types explicitly using reflection
	private void addValueUnknownType(ElasticsearchIndexValueField<?> node, Object value) {
		if ( value == null ) {
			addValue( node, null );
		}
		else {
			@SuppressWarnings("rawtypes")
			ElasticsearchIndexValueField typeCheckedNode =
					node.withValueType( value.getClass(), model.eventContext() );
			addValue( typeCheckedNode, value );
		}
	}

	private DocumentElement addObject(ElasticsearchIndexObjectField node, JsonObject value) {
		ElasticsearchIndexCompositeNode expectedParentNode = node.parent();
		checkTreeConsistency( expectedParentNode );

		if ( TreeNodeInclusion.EXCLUDED.equals( node.inclusion() ) ) {
			return NoOpDocumentElement.get();
		}

		String jsonPropertyName = node.relativeName();
		if ( !node.multiValued() && content.has( jsonPropertyName ) ) {
			throw log.multipleValuesForSingleValuedField( node.absolutePath() );
		}
		GsonUtils.setOrAppendToArray( content, jsonPropertyName, value );

		if ( value == null ) {
			return NoOpDocumentElement.get(); // Will not be used
		}
		else {
			return new ElasticsearchDocumentObjectBuilder( model, node, value );
		}
	}

	private void checkTreeConsistency(ElasticsearchIndexCompositeNode expectedParentNode) {
		if ( !Objects.equals( expectedParentNode, schemaNode ) ) {
			throw log.invalidFieldForDocumentElement( expectedParentNode.absolutePath(), schemaNode.absolutePath() );
		}
	}

}
