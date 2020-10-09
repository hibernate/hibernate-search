/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectFieldNode;

class AbstractLuceneObjectFieldBuilder extends AbstractLuceneDocumentElementBuilder {

	private final AbstractLuceneDocumentElementBuilder parent;

	AbstractLuceneObjectFieldBuilder(LuceneIndexModel model, LuceneIndexSchemaObjectFieldNode schemaNode,
			AbstractLuceneDocumentElementBuilder parent, LuceneDocumentContentImpl documentContent) {
		super( model, schemaNode, documentContent );
		this.parent = parent;
	}

	@Override
	void ensureDynamicValueDetectedByExistsPredicateOnObjectField() {
		documentContent.addFieldName( schemaNode.absolutePath() );
		if ( schemaNode.dynamic() ) {
			// If this object field is dynamic,
			// the parent object's metadata will not include it, so we must propagate the information.
			parent.ensureDynamicValueDetectedByExistsPredicateOnObjectField();
		}
	}
}
