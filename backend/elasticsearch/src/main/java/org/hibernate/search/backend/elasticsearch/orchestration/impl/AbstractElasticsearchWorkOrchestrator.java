/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
