/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.Set;

import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.spi.BackendWorker;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchBackendWorker implements BackendWorker {

	@Override
	public void search(Object tenantId, Set<IndexManager<?>> indexManagers) {
		// TODO Search queries
		throw new UnsupportedOperationException( "Search queries not implemented yet" );
	}

}
