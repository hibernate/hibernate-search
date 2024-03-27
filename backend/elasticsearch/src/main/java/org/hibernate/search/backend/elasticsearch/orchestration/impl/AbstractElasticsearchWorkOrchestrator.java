/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import org.hibernate.search.backend.elasticsearch.link.impl.ElasticsearchLink;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.engine.backend.orchestration.spi.AbstractWorkOrchestrator;

abstract class AbstractElasticsearchWorkOrchestrator<W>
		extends AbstractWorkOrchestrator<W> {

	protected final ElasticsearchLink link;

	AbstractElasticsearchWorkOrchestrator(String name, ElasticsearchLink link) {
		super( name );
		this.link = link;
	}

	protected final ElasticsearchWorkExecutionContext createWorkExecutionContext() {
		return new ElasticsearchWorkExecutionContextImpl( link.getClient(), link.getGsonProvider() );
	}
}
