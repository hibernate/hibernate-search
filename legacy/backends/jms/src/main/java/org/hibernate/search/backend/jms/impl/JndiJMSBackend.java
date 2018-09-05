/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jms.impl;

import java.util.Properties;

import org.hibernate.search.backend.spi.Backend;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;


/**
 * @author Yoann Rodiere
 */
public class JndiJMSBackend implements Backend {

	private Properties properties;

	@Override
	public void initialize(Properties properties, WorkerBuildContext context) {
		this.properties = properties;
	}

	@Override
	public boolean isTransactional() {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public BackendQueueProcessor createQueueProcessor(IndexManager indexManager, WorkerBuildContext context) {
		BackendQueueProcessor queueProcessor = new JndiJMSBackendQueueProcessor();
		queueProcessor.initialize( properties, context, indexManager );
		return queueProcessor;
	}

}
