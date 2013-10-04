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
package org.hibernate.search.backend.impl;

import java.util.List;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.engine.impl.WorkPlan;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class WorkQueue {

	private static final Log log = LoggerFactory.make();

	private WorkPlan plan;

	private List<LuceneWork> sealedQueue;

	//flag indicating if data has been sealed and not modified since
	private boolean sealedAndUnchanged;

	private final SearchFactoryImplementor searchFactoryImplementor;

	public WorkQueue(SearchFactoryImplementor searchFactoryImplementor) {
		this.searchFactoryImplementor = searchFactoryImplementor;
		this.plan = new WorkPlan( searchFactoryImplementor );
	}

	public WorkQueue(SearchFactoryImplementor searchFactoryImplementor, WorkPlan plan) {
		this.searchFactoryImplementor = searchFactoryImplementor;
		this.plan = plan;
	}

	public void add(Work work) {
		this.sealedAndUnchanged = false;
		plan.addWork( work );
	}

	public WorkQueue splitQueue() {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Splitting work queue with %d works", plan.size() );
		}
		WorkQueue subQueue = new WorkQueue( searchFactoryImplementor, plan );
		this.plan = new WorkPlan( searchFactoryImplementor );
		this.sealedAndUnchanged = false;
		return subQueue;
	}

	public List<LuceneWork> getSealedQueue() {
		if ( sealedQueue == null ) {
			throw new AssertionFailure( "Access a WorkQueue which has not been sealed" );
		}
		this.sealedAndUnchanged = false;
		return sealedQueue;
	}

	private void setSealedQueue(List<LuceneWork> sealedQueue) {
		//invalidate the working queue for serializability
		/*
		 * FIXME workaround for flush phase done later
		 *
		 * Due to sometimes flush applied after some beforeCompletion phase
		 * we cannot safely seal the queue, keep it opened as a temporary measure.
		 * This is not the proper fix unfortunately as we don't optimize the whole work queue but rather two subsets
		 *
		 * when the flush ordering is fixed, add the following line
		 * queue = Collections.EMPTY_LIST;
		 */
		this.sealedAndUnchanged = true;
		this.sealedQueue = sealedQueue;
	}

	public void clear() {
		if ( log.isTraceEnabled() ) {
			log.trace( "Clearing current work queue" );
		}
		plan.clear();
		this.sealedAndUnchanged = false;
		if ( sealedQueue != null ) {
			sealedQueue.clear();
		}
	}

	/**
	 * Returns an estimate of the to be performed operations
	 *
	 * @return the approximate size
	 *
	 * @see org.hibernate.search.engine.impl.WorkPlan#size()
	 */
	public int size() {
		return plan.size();
	}

	/**
	 * Compiles the work collected so far in an optimal execution plan,
	 * storing the list of lucene operations to be performed in the sealedQueue.
	 */
	public void prepareWorkPlan() {
		if ( !sealedAndUnchanged ) {
			plan.processContainedInAndPrepareExecution();
			List<LuceneWork> luceneWorkPlan = plan.getPlannedLuceneWork();
			setSealedQueue( luceneWorkPlan );
		}
	}

}
