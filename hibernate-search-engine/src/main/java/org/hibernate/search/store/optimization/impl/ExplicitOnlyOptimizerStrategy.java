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
package org.hibernate.search.store.optimization.impl;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.SearchException;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This OptimizerStrategy will only optimize the index when forced to,
 * using an explicit invocation to {@link SearchFactory#optimize()} or
 * {@link SearchFactory#optimize(Class)}
 * 
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class ExplicitOnlyOptimizerStrategy implements OptimizerStrategy {

	private static final Log log = LoggerFactory.make();

	protected String indexName;
	private final AtomicBoolean optimizerIsBusy = new AtomicBoolean();

	@Override
	public boolean performOptimization(IndexWriter writer) {
		boolean acquired = optimizerIsBusy.compareAndSet( false, true );
		if ( acquired ) {
			try {
				writer.forceMerge(1, true);
			}
			catch (IOException e) {
				throw new SearchException( "Unable to optimize directoryProvider: " + indexName, e );
			}
			finally {
				optimizerIsBusy.set( false );
			}
			return true;
		}
		else {
			log.optimizationSkippedStillBusy( indexName );
			return false;
		}
	}

	@Override
	public void addOperationWithinTransactionCount(long operations) {
	}

	@Override
	public void optimize(Workspace workspace) {
	}

	@Override
	public void initialize(IndexManager indexManager, Properties indexProperties) {
		this.indexName = indexManager.getIndexName();
	}
}
