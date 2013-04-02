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
package org.hibernate.search.test.util;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;

/**
 * This backend wraps the default Lucene backend to leak out the last performed list of work for testing purposes: tests
 * can inspect the queue being sent to a backend.
 *
 * @author Sanne Grinovero
 *
 */
public class LeakingLuceneBackend extends LuceneBackendQueueProcessor {

	private static volatile List<LuceneWork> lastProcessedQueue = new ArrayList<LuceneWork>();

	@Override
	public void close() {
		lastProcessedQueue = new ArrayList<LuceneWork>();
		super.close();
	}

	public static List<LuceneWork> getLastProcessedQueue() {
		return lastProcessedQueue;
	}

	public static void reset() {
		lastProcessedQueue = new ArrayList<LuceneWork>();
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		super.applyWork( workList, monitor );
		lastProcessedQueue = workList;
	}

}
