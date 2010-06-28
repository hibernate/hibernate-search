/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.backend.impl.lucene;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.List;

import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.BatchedQueueingProcessor;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;

/**
 * This will actually contain the Workspace and LuceneWork visitor implementation,
 * reused per-DirectoryProvider.
 * Both Workspace(s) and LuceneWorkVisitor(s) lifecycle are linked to the backend
 * lifecycle (reused and shared by all transactions).
 * The LuceneWorkVisitor(s) are stateless, the Workspace(s) are threadsafe.
 * 
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class LuceneBackendQueueProcessorFactory implements BackendQueueProcessorFactory {

	private SearchFactoryImplementor searchFactoryImp;
	
	/**
	 * Contains the Workspace and LuceneWork visitor implementation,
	 * reused per-DirectoryProvider.
	 * Both Workspace(s) and LuceneWorkVisitor(s) lifecycle are linked to the backend
	 * lifecycle (reused and shared by all transactions);
	 * the LuceneWorkVisitor(s) are stateless, the Workspace(s) are threadsafe.
	 */
	private final Map<DirectoryProvider,PerDPResources> resourcesMap = new HashMap<DirectoryProvider,PerDPResources>();

	/**
	 * copy of BatchedQueueingProcessor.sync
	 */
	private boolean sync;

	public void initialize(Properties props, WorkerBuildContext context) {
		this.searchFactoryImp = context.getUninitializedSearchFactory();
		this.sync = BatchedQueueingProcessor.isConfiguredAsSync( props );
		for (DirectoryProvider dp : context.getDirectoryProviders() ) {
			PerDPResources resources = new PerDPResources( context, dp );
			resourcesMap.put( dp, resources );
		}
	}

	public Runnable getProcessor(List<LuceneWork> queue) {
		return new LuceneBackendQueueProcessor( queue, searchFactoryImp, resourcesMap, sync );
	}

	public void close() {
		// needs to stop all used ThreadPools and cleanup locks
		for (PerDPResources res : resourcesMap.values() ) {
			res.shutdown();
		}
	}
	
}
