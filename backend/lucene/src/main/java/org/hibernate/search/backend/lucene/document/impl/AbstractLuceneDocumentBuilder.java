/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.spi.NoOpDocumentElement;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.Document;


abstract class AbstractLuceneDocumentBuilder implements LuceneDocumentBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final LuceneIndexSchemaObjectNode schemaNode;

	private List<LuceneFlattenedObjectDocumentBuilder> flattenedObjectDocumentBuilders;

	private List<LuceneNestedObjectDocumentBuilder> nestedObjectDocumentBuilders;

	AbstractLuceneDocumentBuilder(LuceneIndexSchemaObjectNode schemaNode) {
		this.schemaNode = schemaNode;
	}

	@Override
	public <F> void addValue(IndexFieldReference<F> fieldReference, F value) {
		LuceneIndexFieldReference<F> luceneFieldReference = (LuceneIndexFieldReference<F>) fieldReference;
		if ( !luceneFieldReference.isEnabled() ) {
			return;
		}

		LuceneIndexSchemaFieldNode<F> fieldSchemaNode = luceneFieldReference.getSchemaNode();
		checkTreeConsistency( fieldSchemaNode.getParent() );
		if ( !fieldSchemaNode.isMultiValued() ) {
			checkNoValueYetForSingleValued( fieldSchemaNode.getAbsoluteFieldPath() );
		}

		fieldSchemaNode.getType().getCodec().encode( this, fieldSchemaNode.getAbsoluteFieldPath(), value );
	}

	@Override
	public DocumentElement addObject(IndexObjectFieldReference fieldReference) {
		LuceneIndexObjectFieldReference luceneFieldReference = (LuceneIndexObjectFieldReference) fieldReference;
		if ( !luceneFieldReference.isEnabled() ) {
			return NoOpDocumentElement.get();
		}

		LuceneIndexSchemaObjectNode fieldSchemaNode = luceneFieldReference.getSchemaNode();
		checkTreeConsistency( fieldSchemaNode.getParent() );
		if ( !fieldSchemaNode.isMultiValued() ) {
			checkNoValueYetForSingleValued( fieldSchemaNode.getAbsolutePath() );
		}

		switch ( luceneFieldReference.getStorage() ) {
			case NESTED:
				LuceneNestedObjectDocumentBuilder nestedDocumentBuilder = new LuceneNestedObjectDocumentBuilder( fieldSchemaNode );
				addNestedObjectDocumentBuilder( nestedDocumentBuilder );
				return nestedDocumentBuilder;
			default:
				LuceneFlattenedObjectDocumentBuilder flattenedDocumentBuilder =
						new LuceneFlattenedObjectDocumentBuilder( fieldSchemaNode, this );
				addFlattenedObjectDocumentBuilder( flattenedDocumentBuilder );
				return flattenedDocumentBuilder;
		}
	}

	@Override
	public void addNullObject(IndexObjectFieldReference fieldReference) {
		LuceneIndexObjectFieldReference luceneFieldReference = (LuceneIndexObjectFieldReference) fieldReference;
		if ( !luceneFieldReference.isEnabled() ) {
			return;
		}

		LuceneIndexSchemaObjectNode fieldSchemaNode = luceneFieldReference.getSchemaNode();
		checkTreeConsistency( fieldSchemaNode.getParent() );
		if ( !fieldSchemaNode.isMultiValued() ) {
			checkNoValueYetForSingleValued( fieldSchemaNode.getAbsolutePath() );
		}

		// We do not add any value for null objects
	}

	@Override
	public void addValue(String relativeFieldName, Object value) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public DocumentElement addObject(String relativeFieldName) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public void addNullObject(String relativeFieldName) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	abstract void checkNoValueYetForSingleValued(String absoluteFieldPath);

	private void addNestedObjectDocumentBuilder(LuceneNestedObjectDocumentBuilder nestedObjectDocumentBuilder) {
		if ( nestedObjectDocumentBuilders == null ) {
			nestedObjectDocumentBuilders = new ArrayList<>();
		}

		nestedObjectDocumentBuilders.add( nestedObjectDocumentBuilder );
	}

	private void addFlattenedObjectDocumentBuilder(
			LuceneFlattenedObjectDocumentBuilder flattenedObjectDocumentBuilder) {
		if ( flattenedObjectDocumentBuilders == null ) {
			flattenedObjectDocumentBuilders = new ArrayList<>();
		}

		flattenedObjectDocumentBuilders.add( flattenedObjectDocumentBuilder );
	}

	private void checkTreeConsistency(LuceneIndexSchemaObjectNode expectedParentNode) {
		if ( !Objects.equals( expectedParentNode, schemaNode ) ) {
			throw log.invalidFieldForDocumentElement( expectedParentNode.getAbsolutePath(), schemaNode.getAbsolutePath() );
		}
	}

	void contribute(MultiTenancyStrategy multiTenancyStrategy, String tenantId, String routingKey,
			String rootId, List<Document> nestedDocuments) {
		if ( flattenedObjectDocumentBuilders != null ) {
			for ( LuceneFlattenedObjectDocumentBuilder flattenedObjectDocumentBuilder : flattenedObjectDocumentBuilders ) {
				flattenedObjectDocumentBuilder.contribute(
						multiTenancyStrategy, tenantId, routingKey,
						rootId, nestedDocuments
				);
			}
		}

		if ( nestedObjectDocumentBuilders != null ) {
			for ( LuceneNestedObjectDocumentBuilder nestedObjectDocumentBuilder : nestedObjectDocumentBuilders ) {
				nestedObjectDocumentBuilder.contribute(
						multiTenancyStrategy, tenantId, routingKey,
						rootId, nestedDocuments
				);
			}
		}
	}
}
