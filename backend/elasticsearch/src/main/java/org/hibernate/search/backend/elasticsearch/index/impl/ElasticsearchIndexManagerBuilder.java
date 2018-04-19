/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.util.Arrays;

import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaCollector;
import org.hibernate.search.backend.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchRootIndexSchemaCollectorImpl;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackendImpl;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.BuildContext;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexManagerBuilder implements IndexManagerBuilder<ElasticsearchDocumentObjectBuilder> {

	private final ElasticsearchBackendImpl backend;
	private final String indexName;
	private final BuildContext context;
	private final ConfigurationPropertySource propertySource;

	private final ElasticsearchRootIndexSchemaCollectorImpl collector;

	public ElasticsearchIndexManagerBuilder(ElasticsearchBackendImpl backend, String indexName,
			BuildContext context, ConfigurationPropertySource propertySource) {
		this.backend = backend;
		this.indexName = indexName;
		this.context = context;
		this.propertySource = propertySource;
		this.collector = new ElasticsearchRootIndexSchemaCollectorImpl( backend.getMultiTenancyStrategy() );
	}

	@Override
	public IndexSchemaCollector getSchemaCollector() {
		return collector;
	}

	@Override
	public ElasticsearchIndexManager build() {
		URLEncodedString encodedIndexName = URLEncodedString.fromString( indexName );
		// TODO find out what to do with type names: what's the point if there is only one type per index anyway?
		URLEncodedString encodedTypeName = URLEncodedString.fromString( "typeName" );

		ElasticsearchIndexModel model = new ElasticsearchIndexModel( encodedIndexName, collector );

		// TODO make sure index initialization is performed in parallel for all indexes?
		ElasticsearchWork<?> dropWork = backend.getWorkFactory().dropIndexIfExists( encodedIndexName );
		ElasticsearchWork<?> createWork = backend.getWorkFactory()
				.createIndex( encodedIndexName, encodedTypeName, model.getMapping() );
		backend.getStreamOrchestrator().submit( Arrays.asList( dropWork, createWork ) )
				.join();

		return new ElasticsearchIndexManager( backend, encodedIndexName, encodedTypeName, model );
	}

}
