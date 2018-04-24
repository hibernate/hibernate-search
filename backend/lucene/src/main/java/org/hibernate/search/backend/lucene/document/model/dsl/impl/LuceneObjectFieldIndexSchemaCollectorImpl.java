/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.ObjectFieldIndexSchemaCollector;
import org.hibernate.search.backend.lucene.document.model.dsl.LuceneIndexSchemaObjectField;
import org.hibernate.search.util.AssertionFailure;

public class LuceneObjectFieldIndexSchemaCollectorImpl
		extends AbstractLuceneIndexSchemaCollector<IndexSchemaObjectPropertyNodeBuilder>
		implements ObjectFieldIndexSchemaCollector {

	LuceneObjectFieldIndexSchemaCollectorImpl(IndexSchemaObjectPropertyNodeBuilder nodeBuilder) {
		super( nodeBuilder );
	}

	@Override
	public LuceneIndexSchemaObjectField withContext(IndexSchemaNestingContext context) {
		/*
		 * Note: this ignores any previous nesting context, but that's alright since
		 * nesting context composition is handled in the engine.
		 */
		return new LuceneIndexSchemaObjectFieldImpl( nodeBuilder, context );
	}

	@Override
	public void explicitRouting() {
		throw new AssertionFailure( "explicitRouting() was called on a non-root schema collector; this should never happen." );
	}
}
