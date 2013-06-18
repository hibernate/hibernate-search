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
package org.hibernate.search.test.util;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.store.optimization.OptimizerStrategy;

public class LeakingOptimizer implements OptimizerStrategy {

	private static final AtomicLong totalOperations = new AtomicLong();

	@Override
	public boolean performOptimization(IndexWriter writer) {
		return false;
	}

	@Override
	public void addOperationWithinTransactionCount(long increment) {
		totalOperations.addAndGet( increment );
	}

	@Override
	public void optimize(Workspace workspace) {
	}

	@Override
	public void initialize(IndexManager indexManager, Properties indexProperties) {
	}

	public static long getTotalOperations() {
		return totalOperations.get();
	}

	public static void reset() {
		totalOperations.set( 0l );
	}

}
