/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.management.impl;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexManagementStrategyConfiguration;
import org.hibernate.search.backend.elasticsearch.index.admin.impl.ElasticsearchIndexAdministrationClient;
import org.hibernate.search.backend.elasticsearch.index.admin.impl.ElasticsearchIndexManagementExecutionOptions;
import org.hibernate.search.util.AssertionFailure;

public class ElasticsearchIndexManagementStrategy {

	private final ElasticsearchIndexManagementStrategyConfiguration strategyConfiguration;
	protected final ElasticsearchIndexManagementExecutionOptions executionOptions;

	public ElasticsearchIndexManagementStrategy(
			ElasticsearchIndexManagementStrategyConfiguration strategyConfiguration,
			ElasticsearchIndexManagementExecutionOptions executionOptions) {
		this.strategyConfiguration = strategyConfiguration;
		this.executionOptions = executionOptions;
	}

	public void onStart(ElasticsearchIndexAdministrationClient client) {
		switch ( strategyConfiguration ) {
			case CREATE:
				client.createIfAbsent( executionOptions );
				break;
			case DROP_AND_CREATE:
			case DROP_AND_CREATE_AND_DROP:
				client.dropAndCreate( executionOptions );
				break;
			case UPDATE:
				client.update( executionOptions );
				break;
			case VALIDATE:
				client.validate( executionOptions );
				break;
			default:
				throw new AssertionFailure( "Unexpected index management strategy: " + strategyConfiguration );
		}
	}

	public void onStop(ElasticsearchIndexAdministrationClient client) {
		switch ( strategyConfiguration ) {
			case DROP_AND_CREATE_AND_DROP:
				client.dropIfExisting( executionOptions );
				break;
			case CREATE:
			case DROP_AND_CREATE:
			case UPDATE:
			case VALIDATE:
				// Nothing to do
				break;
			default:
				throw new AssertionFailure( "Unexpected index management strategy: " + strategyConfiguration );
		}
	}
}
