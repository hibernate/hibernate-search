/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;

/**
 * @author Guillaume Smet
 */
abstract class AbstractLuceneNonFlattenedDocumentBuilder extends AbstractLuceneDocumentBuilder
		implements LuceneDocumentBuilder {

	final Document document = new Document();
	private final Set<String> fieldNames = new HashSet<>();

	AbstractLuceneNonFlattenedDocumentBuilder(LuceneIndexSchemaObjectNode schemaNode) {
		super( schemaNode );
	}

	@Override
	public void addField(IndexableField field) {
		document.add( field );
	}

	@Override
	public void addFieldName(String absoluteFieldPath) {
		fieldNames.add( absoluteFieldPath );
	}

	@Override
	void contribute(String rootIndexName, MultiTenancyStrategy multiTenancyStrategy, String tenantId, String rootId,
			Document currentDocument, List<Document> nestedDocuments) {
		for ( String fieldName : fieldNames ) {
			document.add( new StringField( LuceneFields.fieldNamesFieldName(), fieldName, Field.Store.NO ) );
		}

		multiTenancyStrategy.contributeToIndexedDocument( document, tenantId );

		super.contribute( rootIndexName, multiTenancyStrategy, tenantId, rootId, currentDocument, nestedDocuments );
	}
}
