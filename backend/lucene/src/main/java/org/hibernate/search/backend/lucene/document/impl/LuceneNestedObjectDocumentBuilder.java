/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.util.List;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.BytesRef;


class LuceneNestedObjectDocumentBuilder extends AbstractLuceneNonFlattenedDocumentBuilder {

	/**
	 * Indexed, not tokenized, omits norms, indexes
	 * DOCS_ONLY, stored
	 */
	private static final FieldType TYPE_STORED_BINARY = new FieldType();

	static {
		TYPE_STORED_BINARY.setOmitNorms( true );
		TYPE_STORED_BINARY.setIndexOptions( IndexOptions.DOCS );
		TYPE_STORED_BINARY.setStored( true );
		TYPE_STORED_BINARY.setTokenized( false );

		// Using a binary type to allow doc values extraction. See LuceneChildrenCollector.FieldLeafCollector.
		TYPE_STORED_BINARY.setDocValuesType( DocValuesType.BINARY );

		TYPE_STORED_BINARY.freeze();
	}

	LuceneNestedObjectDocumentBuilder(LuceneIndexSchemaObjectNode schemaNode) {
		super( schemaNode );
	}

	@Override
	void contribute(String rootIndexName, MultiTenancyStrategy multiTenancyStrategy, String tenantId, String rootId,
			List<Document> nestedDocuments) {
		document.add( LuceneFields.searchableRetrievableMetadataField( LuceneFields.typeFieldName(), LuceneFields.TYPE_CHILD_DOCUMENT ) );
		document.add( LuceneFields.searchableRetrievableMetadataField( LuceneFields.rootIndexFieldName(), rootIndexName ) );
		// TODO HSEARCH-3657 use LuceneFields.* to create the field
		document.add( new Field( LuceneFields.rootIdFieldName(), new BytesRef( rootId ), TYPE_STORED_BINARY ) );
		document.add( LuceneFields.searchableRetrievableMetadataField( LuceneFields.nestedDocumentPathFieldName(), schemaNode.getAbsolutePath() ) );

		// all the ancestors of a subdocument must be added after it
		super.contribute( rootIndexName, multiTenancyStrategy, tenantId, rootId, nestedDocuments );
		nestedDocuments.add( document );
	}
}
