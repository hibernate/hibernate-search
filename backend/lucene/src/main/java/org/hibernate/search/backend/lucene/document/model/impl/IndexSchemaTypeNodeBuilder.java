/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

class IndexSchemaTypeNodeBuilder extends AbstractIndexSchemaNodeBuilder {

	@Override
	public String getAbsolutePath() {
		return null;
	}

	protected void contribute(LuceneIndexSchemaNodeCollector collector) {
		LuceneIndexSchemaObjectNode node = LuceneIndexSchemaObjectNode.root();

		contributeChildren( node, collector );
	}
}
