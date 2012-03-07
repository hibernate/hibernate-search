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

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.engine.spi.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.engine.spi.TimingSource;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.spi.InstanceInitializer;

import java.util.Map;
import java.util.Properties;

/**
 * Represents the sharable state of a search factory
 * 
 * @author Emmanuel Bernard
 */
public interface SearchFactoryState {
	Map<Class<?>, DocumentBuilderContainedEntity<?>> getDocumentBuildersContainedEntities();

	Map<Class<?>, EntityIndexBinder> getIndexBindingForEntity();

	String getIndexingStrategy();

	Worker getWorker();

	Map<String, FilterDef> getFilterDefinitions();

	FilterCachingStrategy getFilterCachingStrategy();

	Map<String, Analyzer> getAnalyzers();

	int getCacheBitResultsSize();

	Properties getConfigurationProperties();

	PolymorphicIndexHierarchy getIndexHierarchy();

	ServiceManager getServiceManager();

	boolean isTransactionManagerExpected();

	IndexManagerHolder getAllIndexesManager();

	ErrorHandler getErrorHandler();

	InstanceInitializer getInstanceInitializer();

	TimeoutExceptionFactory getDefaultTimeoutExceptionFactory();

	TimingSource getTimingSource();

	SearchMapping getProgrammaticMapping();

	boolean isIndexMetadataComplete();
}
