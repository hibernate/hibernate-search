/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.IndexableField;


class LuceneFlattenedObjectDocumentBuilder extends AbstractLuceneDocumentBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AbstractLuceneDocumentBuilder parent;

	private final Set<String> encounteredFields = new HashSet<>();

	LuceneFlattenedObjectDocumentBuilder(LuceneIndexModel model, LuceneIndexSchemaObjectNode schemaNode,
			AbstractLuceneDocumentBuilder parent) {
		super( model, schemaNode );
		this.parent = parent;
	}

	@Override
	public void addField(IndexableField field) {
		parent.addField( field );
	}

	@Override
	public void addFieldName(String absoluteFieldPath) {
		parent.addFieldName( absoluteFieldPath );
	}

	@Override
	void checkNoValueYetForSingleValued(String absoluteFieldPath) {
		boolean firstEncounter = encounteredFields.add( absoluteFieldPath );
		if ( !firstEncounter ) {
			throw log.multipleValuesForSingleValuedField( absoluteFieldPath );
		}
	}
}
