/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.document.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;

public class LuceneIndexEntryFactory {

	private final LuceneIndexModel model;
	private final MultiTenancyStrategy multiTenancyStrategy;

	public LuceneIndexEntryFactory(LuceneIndexModel model, MultiTenancyStrategy multiTenancyStrategy) {
		this.model = model;
		this.multiTenancyStrategy = multiTenancyStrategy;
	}

	public LuceneIndexEntry create(String tenantId, String id, String routingKey,
			DocumentContributor documentContributor) {
		LuceneRootDocumentBuilder builder = new LuceneRootDocumentBuilder(
				model, multiTenancyStrategy
		);
		documentContributor.contribute( builder );
		return builder.build( tenantId, id, routingKey );
	}

}
