/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.indexes.impl;

import java.util.Properties;

import org.hibernate.search.Environment;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.backend.impl.lucene.NRTWorkspaceImpl;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.spi.DirectoryBasedReaderProvider;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * IndexManager implementation taking advantage of the Near-Real-Time
 * features of Lucene.
 * When using this work mode the IndexWriter does not need a full
 * flush at the end of each operation: new IndexReaders are able to
 * inspect the unflushed changes still pending in the IndexWriter buffers.
 * 
 * This improves write performance as the IndexWriter doesn't need
 * to commit as often, but has two limitations:
 * <ul>
 * <li>unsaved index data might be lost in case of crashes</li>
 * <li>is not useful for non-local (clustered) backends</li>
 * </ul>
 * 
 * @since 4.0
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class NRTIndexManager extends DirectoryBasedIndexManager {

	private static final Log log = LoggerFactory.make();
	private NRTWorkspaceImpl nrtWorkspace;

	@Override
	protected BackendQueueProcessor createBackend(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		String backend = cfg.getProperty( Environment.WORKER_BACKEND );
		if ( backend != null ) {
			log.ignoringBackendOptionForIndex( indexName, "near-real-time" );
		}
		LuceneBackendQueueProcessor backendQueueProcessor = new LuceneBackendQueueProcessor();
		nrtWorkspace = new NRTWorkspaceImpl( this, buildContext, cfg );
		backendQueueProcessor.setCustomWorkspace( nrtWorkspace );
		backendQueueProcessor.initialize( cfg, buildContext, this );
		return backendQueueProcessor;
	}

	@Override
	protected DirectoryBasedReaderProvider createIndexReader(String indexName, Properties cfg, WorkerBuildContext buildContext) {
		return nrtWorkspace;
	}

}
