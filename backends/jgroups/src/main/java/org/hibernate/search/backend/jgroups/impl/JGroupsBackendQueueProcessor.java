/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.jgroups.impl;

import java.util.Collections;
import java.util.List;

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
	private final BackendQueueProcessor delegatedBackend;

	public JGroupsBackendQueueProcessor(NodeSelectorStrategy selectionStrategy,
			JGroupsBackendQueueTask jgroupsProcessor,
			BackendQueueProcessor delegatedBackend) {
		this.selectionStrategy = selectionStrategy;
		this.jgroupsProcessor = jgroupsProcessor;
		this.delegatedBackend = delegatedBackend;
	}

	@Override
	public void close() {
		if ( selectionStrategy.isIndexOwnerLocal() ) {
			//TODO verify all delegates have been closed when ownership was lost before [HSEARCH-2060]
			delegatedBackend.close();
		}
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		if ( selectionStrategy.isIndexOwnerLocal() ) {
			delegatedBackend.applyWork( workList, monitor );
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
			delegatedBackend.applyStreamWork( singleOperation, monitor );
		}
		else {
			//TODO optimize for single operation?
			jgroupsProcessor.sendLuceneWorkList( Collections.singletonList( singleOperation ) );
		}
	}

	public boolean blocksForACK() {
		return jgroupsProcessor.blocksForACK();
	}

	public BackendQueueProcessor getDelegatedBackend() {
		return delegatedBackend;
	}

	public long getMessageTimeout() {
		return jgroupsProcessor.getMessageTimeout();
	}

}
