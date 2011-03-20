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

package org.hibernate.search.spi.internals;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.search.Similarity;

import org.hibernate.search.batchindexing.Executors;
import org.hibernate.search.store.optimization.OptimizerStrategy;

/**
* @author Emmanuel Bernard
*/
public class DirectoryProviderData {
	private final ReentrantLock dirLock = new ReentrantLock();
	private OptimizerStrategy optimizerStrategy;
	private final Set<Class<?>> classes = new HashSet<Class<?>>( 2 );
	private Similarity similarity = null;
	private boolean exclusiveIndexUsage;
	private int maxQueueLength = Executors.QUEUE_MAX_LENGTH;

	public void setOptimizerStrategy(OptimizerStrategy optimizerStrategy) {
		this.optimizerStrategy = optimizerStrategy;
	}

	public void setSimilarity(Similarity similarity) {
		this.similarity = similarity;
	}

	public void setExclusiveIndexUsage(boolean exclusiveIndexUsage) {
		this.exclusiveIndexUsage = exclusiveIndexUsage;
	}

	public ReentrantLock getDirLock() {
		return dirLock;
	}

	public OptimizerStrategy getOptimizerStrategy() {
		return optimizerStrategy;
	}

	public Set<Class<?>> getClasses() {
		return classes;
	}

	public Similarity getSimilarity() {
		return similarity;
	}

	public boolean isExclusiveIndexUsage() {
		return exclusiveIndexUsage;
	}

	public void setMaxQueueLength(int maxQueueLength) {
		this.maxQueueLength = maxQueueLength;
	}

	public int getMaxQueueLength() {
		return maxQueueLength;
	}

}
