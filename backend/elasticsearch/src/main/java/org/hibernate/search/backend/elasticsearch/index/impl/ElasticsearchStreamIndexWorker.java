/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import org.hibernate.search.backend.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.backend.index.spi.StreamIndexWorker;
import org.hibernate.search.engine.common.spi.SessionContext;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchStreamIndexWorker extends ElasticsearchIndexWorker
		implements StreamIndexWorker<ElasticsearchDocumentObjectBuilder> {

	private final ElasticsearchWorkOrchestrator orchestrator;

	ElasticsearchStreamIndexWorker(ElasticsearchWorkFactory factory, MultiTenancyStrategy multiTenancyStrategy,
			ElasticsearchWorkOrchestrator orchestrator,
			URLEncodedString indexName, URLEncodedString typeName,
			SessionContext sessionContext) {
		super( factory, multiTenancyStrategy, indexName, typeName, sessionContext );
		this.orchestrator = orchestrator;
	}

	@Override
	protected void collect(ElasticsearchWork<?> work) {
		orchestrator.submit( work );
	}

	@Override
	public void flush() {
		collect( factory.flush( indexName ) );
	}

	@Override
	public void optimize() {
		collect( factory.optimize( indexName ) );
	}

}
