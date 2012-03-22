/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.backend.impl.jgroups;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class AutoJGroupsBackendQueueProcessor extends JGroupsBackendQueueProcessor {

	private static final Log log = LoggerFactory.make();

	private JGroupsBackendQueueTask jgroupsProcessor;

	private AutoNodeSelector autoNodeSelector;

	private final LuceneBackendQueueProcessor luceneBackendQueueProcessor = new LuceneBackendQueueProcessor();

	@Override
	public void initialize(Properties props, WorkerBuildContext context, DirectoryBasedIndexManager indexManager) {
		super.initialize( props, context, indexManager );
		luceneBackendQueueProcessor.initialize( props, context, indexManager );
		GlobalMasterSelector masterNodeSelector = context.requestService( MasterSelectorServiceProvider.class );
		autoNodeSelector = new AutoNodeSelector( indexName );
		masterNodeSelector.setNodeSelectorStrategy( indexName, autoNodeSelector );
		jgroupsProcessor = new JGroupsBackendQueueTask( this, indexManager );
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		if ( autoNodeSelector.isIndexOwnerLocal() ) {
			luceneBackendQueueProcessor.applyWork( workList, monitor );
		}
		else {
			if ( workList == null ) {
				throw new IllegalArgumentException( "workList should not be null" );
			}
			jgroupsProcessor.sendLuceneWorkList( workList );
		}
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor) {
		if ( autoNodeSelector.isIndexOwnerLocal() ) {
			luceneBackendQueueProcessor.applyStreamWork( singleOperation, monitor );
		}
		else {
			//TODO optimize for single operation?
			jgroupsProcessor.sendLuceneWorkList( Collections.singletonList( singleOperation ) );
		}
	}

	@Override
	public Lock getExclusiveWriteLock() {
		return luceneBackendQueueProcessor.getExclusiveWriteLock();
	}

	@Override
	public void close() {
		super.close();
		luceneBackendQueueProcessor.close();
	}

}