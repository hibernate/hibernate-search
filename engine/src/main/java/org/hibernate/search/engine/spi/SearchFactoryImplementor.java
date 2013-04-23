/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.engine.spi;

import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.impl.batch.BatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.SearchFactoryIntegrator;
import org.hibernate.search.stat.spi.StatisticsImplementor;

/**
 * Interface which gives access to the metadata. Intended to be used by Search components.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public interface SearchFactoryImplementor extends SearchFactoryIntegrator {
	
	Map<Class<?>, EntityIndexBinder> getIndexBindingForEntity();

	<T> DocumentBuilderContainedEntity<T> getDocumentBuilderContainedEntity(Class<T> entityType);

	FilterCachingStrategy getFilterCachingStrategy();

	FilterDef getFilterDefinition(String name);

	String getIndexingStrategy();

	int getFilterCacheBitResultsSize();

	Set<Class<?>> getIndexedTypesPolymorphic(Class<?>[] classes);

	BatchBackend makeBatchBackend(MassIndexerProgressMonitor progressMonitor);

	boolean isJMXEnabled();

	/**
	 * Retrieve the statistics implementor instance for this factory.
	 *
	 * @return The statistics implementor.
	 */
	public StatisticsImplementor getStatisticsImplementor();

	/**
	 * @return true if we are allowed to inspect entity state to
	 * potentially skip some indexing operations.
	 * Can be disabled to get pre-3.4 behavior (always rebuild document)
	 */
	boolean isDirtyChecksEnabled();
	
	IndexManagerHolder getAllIndexesManager();

	/**
	 * @return returns an instance of {@code InstanceInitializer} for class/object initialization.
	 */
	InstanceInitializer getInstanceInitializer();

	TimingSource getTimingSource();
}
