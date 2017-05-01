/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.spi.impl;

import java.util.Map;
import java.util.Properties;

import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.integration.impl.SearchIntegration;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.TimingSource;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.spi.IndexingMode;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.IndexedTypeMap;
import org.hibernate.search.stat.Statistics;

/**
 * Represents the sharable state of a search factory
 *
 * @author Emmanuel Bernard
 */
public interface SearchFactoryState {

	IndexedTypeMap<DocumentBuilderContainedEntity> getDocumentBuildersContainedEntities();

	IndexedTypeMap<EntityIndexBinding> getIndexBindings();

	IndexingMode getIndexingMode();

	Worker getWorker();

	Map<String, FilterDef> getFilterDefinitions();

	FilterCachingStrategy getFilterCachingStrategy();

	Map<IndexManagerType, SearchIntegration> getIntegrations();

	int getCacheBitResultsSize();

	Properties getConfigurationProperties();

	/**
	 * @return The type hierarchy for all configured types, indexed or not.
	 */
	TypeHierarchy getConfiguredTypeHierarchy();

	/**
	 * @return The type hierarchy for configured types that are indexed
	 * (for instance because they are annotated with @Indexed)
	 */
	TypeHierarchy getIndexedTypeHierarchy();

	ServiceManager getServiceManager();

	boolean isTransactionManagerExpected();

	IndexManagerHolder getAllIndexesManager();

	ErrorHandler getErrorHandler();

	InstanceInitializer getInstanceInitializer();

	TimeoutExceptionFactory getDefaultTimeoutExceptionFactory();

	TimingSource getTimingSource();

	SearchMapping getProgrammaticMapping();

	boolean isIndexMetadataComplete();

	boolean isDeleteByTermEnforced();

	boolean isIdProvidedImplicit();

	boolean isMultitenancyEnabled();

	IndexManagerFactory getIndexManagerFactory();

	boolean enlistWorkerInTransaction();

	Statistics getStatistics();

	LuceneWorkSerializer getWorkSerializerState();
}
