/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.Properties;

import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentBuilder;
import org.hibernate.search.backend.elasticsearch.index.impl.ElasticsearchIndexManagerBuilder;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.StubElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.spi.Backend;
import org.hibernate.search.engine.backend.spi.BackendWorker;
import org.hibernate.search.engine.common.spi.BuildContext;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchBackend implements Backend<ElasticsearchDocumentBuilder> {

	private final ElasticsearchClient client;

	private final ElasticsearchWorkFactory workFactory;

	private final ElasticsearchWorkOrchestrator streamOrchestrator;

	private final ElasticsearchBackendWorker worker;

	public ElasticsearchBackend(ElasticsearchClient client, ElasticsearchWorkFactory workFactory) {
		this.client = client;
		this.workFactory = workFactory;
		this.streamOrchestrator = new StubElasticsearchWorkOrchestrator( client );
		this.worker = new ElasticsearchBackendWorker();
	}

	@Override
	public IndexManagerBuilder<ElasticsearchDocumentBuilder> createIndexManagerBuilder(
			String name, BuildContext context, Properties indexProperties) {
		return new ElasticsearchIndexManagerBuilder( this, name, context, indexProperties );
	}

	public ElasticsearchWorkFactory getWorkFactory() {
		return workFactory;
	}

	public ElasticsearchWorkOrchestrator createChangesetOrchestrator() {
		return new StubElasticsearchWorkOrchestrator( client );
	}

	public ElasticsearchWorkOrchestrator getStreamOrchestrator() {
		return streamOrchestrator;
	}

	@Override
	public BackendWorker getWorker() {
		return worker;
	}

	@Override
	public void close() {
		// TODO use a Closer
		client.close();
		streamOrchestrator.close();
	}

}
