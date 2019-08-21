/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;

import org.apache.lucene.facet.FacetsConfig;

public class LuceneIndexEntryFactory {

	private final MultiTenancyStrategy multiTenancyStrategy;
	private final String indexName;
	private final FacetsConfig facetsConfig;

	public LuceneIndexEntryFactory(MultiTenancyStrategy multiTenancyStrategy, String indexName,
			FacetsConfig facetsConfig) {
		this.indexName = indexName;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.facetsConfig = facetsConfig;
	}

	public LuceneIndexEntry create(String tenantId, String id,
			DocumentContributor<LuceneRootDocumentBuilder> documentContributor) {
		LuceneRootDocumentBuilder builder = new LuceneRootDocumentBuilder(
				multiTenancyStrategy, indexName, facetsConfig
		);
		documentContributor.contribute( builder );
		return builder.build( tenantId, id );
	}

}
