/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import java.util.Properties;

import org.hibernate.search.backend.spi.Backend;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;


/**
 * @author Yoann Rodiere
 */
public class JGroupsBackend implements Backend {

	private Properties properties;

	@Override
	public void initialize(Properties properties, WorkerBuildContext context) {
		this.properties = properties;
	}

	@Override
	@SuppressWarnings("deprecation")
	public BackendQueueProcessor createQueueProcessor(IndexManager indexManager, WorkerBuildContext context) {
		NodeSelectorStrategy nodeSelectorStrategy = createNodeSelectorStrategy( indexManager );
		BackendQueueProcessor queueProcessor = new JGroupsBackendQueueProcessor( nodeSelectorStrategy );
		queueProcessor.initialize( properties, context, indexManager );
		return queueProcessor;
	}

	protected NodeSelectorStrategy createNodeSelectorStrategy(IndexManager indexManager) {
		return new AutoNodeSelector( indexManager.getIndexName() );
	}

}
