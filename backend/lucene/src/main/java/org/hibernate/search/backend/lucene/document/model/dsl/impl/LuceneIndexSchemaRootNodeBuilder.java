/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneRootIndexSchemaContributor;

public class LuceneIndexSchemaRootNodeBuilder extends AbstractLuceneIndexSchemaObjectNodeBuilder
		implements IndexSchemaRootNodeBuilder, LuceneRootIndexSchemaContributor {

	@Override
	public String getAbsolutePath() {
		return null;
	}

	@Override
	public void explicitRouting() {
		// TODO GSM support explicit routing?
		throw new UnsupportedOperationException( "explicitRouting not supported right now" );
	}

	@Override
	public void contribute(LuceneIndexSchemaNodeCollector collector) {
		LuceneIndexSchemaObjectNode node = LuceneIndexSchemaObjectNode.root();

		contributeChildren( node, collector );
	}
}
