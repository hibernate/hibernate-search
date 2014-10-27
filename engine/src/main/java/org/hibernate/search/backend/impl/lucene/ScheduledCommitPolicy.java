/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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


import org.hibernate.search.batchindexing.impl.Executors;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Commit policy that will commit at a regular intervals defined by configuration or immediately on
 * explicit flush requests
 *
 * @author gustavonalle
 */
public final class ScheduledCommitPolicy extends AbstractCommitPolicy {

	public static final int DEFAULT_DELAY_MS = 1000;

	private final ScheduledExecutorService scheduledExecutorService;
	private final int delay;

	public ScheduledCommitPolicy( IndexWriterHolder indexWriterHolder, String indexName, int delay ) {
		super( indexWriterHolder );
		this.delay = delay;
		this.scheduledExecutorService = Executors.newScheduledThreadPool( "Commit Scheduler for index " + indexName );
		scheduledExecutorService.scheduleWithFixedDelay( new CommitTask(), 1000, delay, TimeUnit.MILLISECONDS );
	}

	public int getDelay() {
		return delay;
	}

	@Override
	public void onChangeSetApplied( boolean someFailureHappened, boolean streaming ) {
		if ( someFailureHappened ) {
			indexWriterHolder.forceLockRelease();
		}
	}

	public ScheduledExecutorService getScheduledExecutorService() {
		return scheduledExecutorService;
	}

	@Override
	public void onFlush() {
		indexWriterHolder.commitIndexWriter();
	}

	@Override
	public void onClose() {
		scheduledExecutorService.shutdown();
	}

	private final class CommitTask implements Runnable {

		@Override
		public void run() {
			if ( getIndexWriter() != null ) {
				indexWriterHolder.commitIndexWriter();
			}
		}
	}

}
