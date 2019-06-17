/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;


abstract class AbstractLuceneNonFlattenedDocumentBuilder extends AbstractLuceneDocumentBuilder
		implements LuceneDocumentBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	final Document document = new Document();
	private final Map<String, EncounteredFieldStatus> fieldStatus = new HashMap<>();

	AbstractLuceneNonFlattenedDocumentBuilder(LuceneIndexSchemaObjectNode schemaNode) {
		super( schemaNode );
	}

	@Override
	public void addField(IndexableField field) {
		document.add( field );
	}

	@Override
	public void addFieldName(String absoluteFieldPath) {
		// If the status was already ENCOUNTERED, just replace it.
		fieldStatus.put( absoluteFieldPath, EncounteredFieldStatus.ENCOUNTERED_AND_NAME_INDEXED );
	}

	@Override
	void checkNoValueYetForSingleValued(String absoluteFieldPath) {
		EncounteredFieldStatus previousValue = fieldStatus.putIfAbsent( absoluteFieldPath, EncounteredFieldStatus.ENCOUNTERED );
		if ( previousValue != null ) {
			throw log.multipleValuesForSingleValuedField( absoluteFieldPath );
		}
	}

	@Override
	void contribute(String rootIndexName, MultiTenancyStrategy multiTenancyStrategy, String tenantId, String rootId,
			Document currentDocument, List<Document> nestedDocuments) {
		for ( Map.Entry<String, EncounteredFieldStatus> entry : fieldStatus.entrySet() ) {
			EncounteredFieldStatus status = entry.getValue();
			if ( EncounteredFieldStatus.ENCOUNTERED_AND_NAME_INDEXED.equals( status ) ) {
				String fieldName = entry.getKey();
				document.add( new StringField( LuceneFields.fieldNamesFieldName(), fieldName, Field.Store.NO ) );
			}
		}

		multiTenancyStrategy.contributeToIndexedDocument( document, tenantId );

		super.contribute( rootIndexName, multiTenancyStrategy, tenantId, rootId, currentDocument, nestedDocuments );
	}

	private enum EncounteredFieldStatus {
		ENCOUNTERED,
		ENCOUNTERED_AND_NAME_INDEXED;
	}
}
