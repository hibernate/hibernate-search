/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;

/**
 * This index backend is able to switch dynamically between a standard
 * Lucene index writing backend and one which sends work remotely over
 * a JGroups channel.
 *
 * @author Lukasz Moren
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 * @author Ales Justin
 */
public class JGroupsBackendQueueProcessor implements BackendQueueProcessor {

	private final NodeSelectorStrategy selectionStrategy;
	private final JGroupsBackendQueueTask jgroupsProcessor;
	private final Supplier<BackendQueueProcessor> delegatedBackendFactory;
	private volatile BackendQueueProcessor delegate;

	public JGroupsBackendQueueProcessor(NodeSelectorStrategy selectionStrategy,
			JGroupsBackendQueueTask jgroupsProcessor,
			Supplier<BackendQueueProcessor> delegatedBackendFactory) {
		this.selectionStrategy = selectionStrategy;
		this.jgroupsProcessor = jgroupsProcessor;
		this.delegatedBackendFactory = delegatedBackendFactory;
		if ( selectionStrategy.isIndexOwnerLocal() ) {
			/*
			 * Eager initialization if we know from the start we are the master.
			 * This allows in particular the delegate backend to fail fast
			 * if there is a configuration issue.
			 */
			getOrCreateDelegate();
		}
	}

	@Override
	public synchronized void close() {
		if ( delegate != null ) {
			delegate.close();
		}
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		if ( selectionStrategy.isIndexOwnerLocal() ) {
			getOrCreateDelegate().applyWork( workList, monitor );
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
		if ( selectionStrategy.isIndexOwnerLocal() ) {
			getOrCreateDelegate().applyStreamWork( singleOperation, monitor );
		}
		else {
			//TODO optimize for single operation?
			jgroupsProcessor.sendLuceneWorkList( Collections.singletonList( singleOperation ) );
		}
	}

	private BackendQueueProcessor getOrCreateDelegate() {
		if ( delegate != null ) {
			return delegate;
		}
		synchronized ( this ) {
			if ( delegate != null ) {
				return delegate;
			}
			delegate = delegatedBackendFactory.get();
			return delegate;
		}
	}

	public boolean blocksForACK() {
		return jgroupsProcessor.blocksForACK();
	}

	public BackendQueueProcessor getExistingDelegate() {
		return delegate;
	}

	public long getMessageTimeout() {
		return jgroupsProcessor.getMessageTimeout();
	}

}
