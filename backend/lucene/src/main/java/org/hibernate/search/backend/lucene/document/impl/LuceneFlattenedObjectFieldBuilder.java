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
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectFieldNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class LuceneFlattenedObjectFieldBuilder extends AbstractLuceneDocumentElementBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<String> encounteredFields = new HashSet<>();

	LuceneFlattenedObjectFieldBuilder(LuceneIndexModel model, LuceneIndexSchemaObjectFieldNode schemaNode,
			LuceneDocumentContentImpl documentContent) {
		// The document content is not ours: it's the parent's.
		super( model, schemaNode, documentContent );
	}

	@Override
	void checkNoValueYetForSingleValued(String absoluteFieldPath) {
		// We cannot rely on the document content which is shared between all flattened objects,
		// because the single-valued field may have one value for each flattened object.
		boolean firstEncounter = encounteredFields.add( absoluteFieldPath );
		if ( !firstEncounter ) {
			throw log.multipleValuesForSingleValuedField( absoluteFieldPath );
		}
	}
}
