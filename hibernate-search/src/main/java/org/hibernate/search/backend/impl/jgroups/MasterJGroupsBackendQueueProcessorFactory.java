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

import org.jgroups.Receiver;

import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessorFactory;

/**
 * Backend factory used in JGroups clustering mode in master node.
 * Wraps {@link LuceneBackendQueueProcessorFactory} with providing extra
 * functionality to receive Lucene works from slave nodes.
 *
 * @author Lukasz Moren
 * @see org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessorFactory
 * @see org.hibernate.search.backend.impl.jgroups.SlaveJGroupsBackendQueueProcessorFactory
 */
public class MasterJGroupsBackendQueueProcessorFactory extends JGroupsBackendQueueProcessorFactory {

	private LuceneBackendQueueProcessorFactory luceneBackendQueueProcessorFactory;
	private Receiver masterListener;

	@Override
	public void initialize(Properties props, WorkerBuildContext context) {
		super.initialize( props, context );
		initLuceneBackendQueueProcessorFactory( props, context );
		registerMasterListener();
	}

	public Runnable getProcessor(List<LuceneWork> queue) {
		return luceneBackendQueueProcessorFactory.getProcessor( queue );
	}

	private void registerMasterListener() {
		//register JGroups receiver in master node to get Lucene docs from slave nodes
		masterListener = new JGroupsMasterMessageListener( searchFactory );
		channel.setReceiver( masterListener );
	}

	private void initLuceneBackendQueueProcessorFactory(Properties props, WorkerBuildContext context) {
		luceneBackendQueueProcessorFactory = new LuceneBackendQueueProcessorFactory();
		luceneBackendQueueProcessorFactory.initialize( props, context );
	}

	public Receiver getMasterListener() {
		return masterListener;
	}

	@Override
	public void close() {
		super.close();
		luceneBackendQueueProcessorFactory.close();
	}
}
