/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaCollector;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaNestingContext;
import org.hibernate.search.backend.lucene.document.model.dsl.LuceneIndexSchemaElement;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;

public class LuceneRootIndexSchemaCollectorImpl
		extends AbstractLuceneIndexSchemaCollector<IndexSchemaTypeNodeBuilder>
		implements IndexSchemaCollector {

	public LuceneRootIndexSchemaCollectorImpl() {
		super( new IndexSchemaTypeNodeBuilder() );
	}

	@Override
	public LuceneIndexSchemaElement withContext(IndexSchemaNestingContext context) {
		/*
		 * Note: this ignores any previous nesting context, but that's alright since
		 * nesting context composition is handled in the engine.
		 */
		return new LuceneIndexSchemaElementImpl( nodeBuilder, context );
	}

	@Override
	public void explicitRouting() {
		// TODO GSM support explicit routing?
		throw new UnsupportedOperationException( "explicitRouting not supported right now" );
	}

	public void contribute(LuceneIndexSchemaNodeCollector collector) {
		nodeBuilder.contribute( collector );
	}
}
