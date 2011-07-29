/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.backend.impl.blackhole;

import java.util.List;
import java.util.Properties;

import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessorFactory;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This backend does not do anything: the Documents are not
 * sent to any index but are discarded.
 * Useful to identify the bottleneck in indexing performance problems,
 * fully disabling the backend system but still building the Documents
 * needed to update an index (loading data from DB).
 *
 * @author Sanne Grinovero
 */
public class BlackHoleBackendQueueProcessorFactory implements BackendQueueProcessorFactory {
	
	private static final Log log = LoggerFactory.make();
	
	private final NoOp noOp = new NoOp();
	
	public Runnable getProcessor(List<LuceneWork> queue) {
		return noOp;
	}

	public void initialize(Properties props, WorkerBuildContext context, IndexManager indexManager) {
		// no-op
		log.initializedBlackholeBackend();
	}

	public void close() {
		// no-op
		log.closedBlackholeBackend();
	}

	private static class NoOp implements Runnable {

		public void run() {
			// no-op
			log.debug( "Discarding a list of LuceneWork" );
		}
		
	}

}
