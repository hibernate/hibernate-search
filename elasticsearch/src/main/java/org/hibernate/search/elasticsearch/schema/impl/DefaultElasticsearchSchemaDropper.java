/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.Properties;

import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.BuildContext;

/**
 * The default {@link ElasticsearchSchemaDropper} implementation.
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchSchemaDropper implements ElasticsearchSchemaDropper, Startable, Stoppable {

	private ServiceManager serviceManager;
	private ElasticsearchSchemaAccessor schemaAccessor;

	@Override
	public void start(Properties properties, BuildContext context) {
		serviceManager = context.getServiceManager();
		schemaAccessor = serviceManager.requestService( ElasticsearchSchemaAccessor.class );
	}

	@Override
	public void stop() {
		schemaAccessor = null;
		serviceManager.releaseService( ElasticsearchSchemaAccessor.class );
		serviceManager = null;
	}

	@Override
	public void drop(String indexName, ExecutionOptions executionOptions) {
		schemaAccessor.dropIndex( indexName, executionOptions );
	}

	@Override
	public void dropIfExisting(String indexName, ExecutionOptions executionOptions) {
		// Not actually needed, but do it to avoid cluttering the ES log
		if ( ! schemaAccessor.indexExists( indexName ) ) {
			return;
		}

		try {
			schemaAccessor.dropIndex( indexName, executionOptions );
		}
		catch (SearchException e) {
			// ignoring deletion of non-existing index
			if ( !e.getMessage().contains( "index_not_found_exception" ) ) {
				throw e;
			}
		}
	}

}
