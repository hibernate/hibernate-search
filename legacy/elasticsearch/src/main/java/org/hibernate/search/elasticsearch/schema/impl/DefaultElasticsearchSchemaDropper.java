/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.exception.SearchException;

/**
 * The default {@link ElasticsearchSchemaDropper} implementation.
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchSchemaDropper implements ElasticsearchSchemaDropper {

	private final ElasticsearchSchemaAccessor schemaAccessor;

	public DefaultElasticsearchSchemaDropper(ElasticsearchSchemaAccessor schemaAccessor) {
		this.schemaAccessor = schemaAccessor;
	}

	@Override
	public void drop(URLEncodedString indexName, ExecutionOptions executionOptions) {
		schemaAccessor.dropIndex( indexName, executionOptions );
	}

	@Override
	public void dropIfExisting(URLEncodedString indexName, ExecutionOptions executionOptions) {
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
