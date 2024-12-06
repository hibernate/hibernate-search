/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.document.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexObjectField;

class AbstractLuceneObjectFieldBuilder extends AbstractLuceneDocumentElementBuilder {

	private final AbstractLuceneDocumentElementBuilder parent;

	AbstractLuceneObjectFieldBuilder(LuceneIndexModel model, LuceneIndexObjectField schemaNode,
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
