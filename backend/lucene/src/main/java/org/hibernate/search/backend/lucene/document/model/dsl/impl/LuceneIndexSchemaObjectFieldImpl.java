/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaNestingContext;
import org.hibernate.search.backend.lucene.document.model.dsl.LuceneIndexSchemaObjectField;

/**
 * @author Guillaume Smet
 */
class LuceneIndexSchemaObjectFieldImpl extends LuceneIndexSchemaElementImpl
		implements LuceneIndexSchemaObjectField {

	private IndexSchemaObjectPropertyNodeBuilder nodeBuilder;

	LuceneIndexSchemaObjectFieldImpl(IndexSchemaObjectPropertyNodeBuilder nodeBuilder,
			IndexSchemaNestingContext nestingContext) {
		super( nodeBuilder, nestingContext );

		this.nodeBuilder = nodeBuilder;
	}

	@Override
	public IndexObjectFieldAccessor createAccessor() {
		return nodeBuilder.getAccessor();
	}
}
