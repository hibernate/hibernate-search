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
package org.hibernate.search.backend.impl.jgroups;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jgroups.Receiver;

import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;

/**
 * Backend factory used in JGroups clustering mode in master node.
 * Wraps {@link LuceneBackendQueueProcessor} with providing extra
 * functionality to receive Lucene works from slave nodes.
 *
 * @author Lukasz Moren
 * @see org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor
 * @see org.hibernate.search.backend.impl.jgroups.SlaveJGroupsBackendQueueProcessor
 */
public class MasterJGroupsBackendQueueProcessor extends JGroupsBackendQueueProcessor {

	private static final Log log = LoggerFactory.make();

	private LuceneBackendQueueProcessor luceneBackendQueueProcessor;
	private Receiver masterListener;

	@Override
	public void initialize(Properties props, WorkerBuildContext context, DirectoryBasedIndexManager indexManager) {
		super.initialize( props, context, indexManager );
		initLuceneBackendQueueProcessor( props, context );
		registerMasterListener( context.getUninitializedSearchFactory() );
	}

	private void registerMasterListener(SearchFactoryImplementor searchFactory) {
		//register JGroups receiver in master node to get Lucene docs from slave nodes
		masterListener = new JGroupsMasterMessageListener( searchFactory );
		channel.setReceiver( masterListener );
	}

	private void initLuceneBackendQueueProcessor(Properties props, WorkerBuildContext context) {
		luceneBackendQueueProcessor = new LuceneBackendQueueProcessor();
		luceneBackendQueueProcessor.initialize( props, context, indexManager );
	}

	public Receiver getMasterListener() {
		return masterListener;
	}

	@Override
	public void close() {
		super.close();
		luceneBackendQueueProcessor.close();
	}

	@Override
	public void applyWork(List<LuceneWork> workList) {
		luceneBackendQueueProcessor.applyWork( workList );
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation) {
		luceneBackendQueueProcessor.applyStreamWork( singleOperation );
	}

	@Override
	public Lock getExclusiveWriteLock() {
		log.warnSuspiciousBackendDirectoryCombination( indexName );
		return new ReentrantLock(); // keep the invoker happy, still it's useless
	}
}
