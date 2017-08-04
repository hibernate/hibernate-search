/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.util.Properties;

import org.hibernate.search.engine.backend.document.model.spi.IndexModelCollectorImplementor;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.ElasticsearchIndexModelCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModelCollectorImpl;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackend;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.common.spi.BuildContext;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexManagerBuilder implements IndexManagerBuilder<ElasticsearchDocumentBuilder> {

	private final ElasticsearchBackend backend;
	private final String indexName;
	private final BuildContext context;
	private final Properties indexProperties;

	private final ElasticsearchIndexModelCollectorImpl collector =
			new ElasticsearchIndexModelCollectorImpl();

	public ElasticsearchIndexManagerBuilder(ElasticsearchBackend backend, String indexName,
			BuildContext context, Properties indexProperties) {
		this.backend = backend;
		this.indexName = indexName;
		this.context = context;
		this.indexProperties = indexProperties;
	}

	@Override
	public IndexModelCollectorImplementor getModelCollector() {
		return collector;
	}

	@Override
	public ElasticsearchIndexManager build() {
		ElasticsearchIndexModel model = new ElasticsearchIndexModel( collector );
		// TODO use the context and properties somehow
		return new ElasticsearchIndexManager( backend, indexName, model );
	}

}
