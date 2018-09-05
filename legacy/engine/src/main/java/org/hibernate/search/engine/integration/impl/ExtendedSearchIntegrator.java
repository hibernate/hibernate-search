/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.integration.impl;

import java.util.Map;
import java.util.Properties;

import org.hibernate.search.analyzer.spi.ScopedAnalyzerReference;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.spi.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.spi.TimingSource;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.stat.spi.StatisticsImplementor;

/**
 * Interface which gives access to runtime configuration. Intended to be used by Search components.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public interface ExtendedSearchIntegrator extends SearchIntegrator {

	DocumentBuilderContainedEntity getDocumentBuilderContainedEntity(IndexedTypeIdentifier entityType);

	FilterCachingStrategy getFilterCachingStrategy();

	FilterDef getFilterDefinition(String name);

	int getFilterCacheBitResultsSize();

	/**
	 * Given a set of target entities, return the set of configured subtypes.
	 * <p>
	 * "Configured" types are types that Hibernate Search was instructed to take into consideration,
	 * i.e. types returned by {@link SearchConfiguration#getClassMappings()}.
	 *
	 * @param types
	 * @return the set of configured subtypes
	 */
	IndexedTypeSet getConfiguredTypesPolymorphic(IndexedTypeSet types);

	/**
	 * Given a set of target entities, return the set of configured subtypes that are indexed.
	 * <p>
	 * "Configured" types are types that Hibernate Search was instructed to take into consideration,
	 * i.e. types returned by {@link SearchConfiguration#getClassMappings()}.
	 * <p>
	 * "Indexed" types are configured types that happened to be annotated with {@code @Indexed},
	 * or similarly configured through a programmatic mapping.
	 * <p>
	 * Note: the fact that a given type is configured or indexed doesn't mean that its subtypes are, too.
	 * Each type must be configured explicitly.
	 *
	 * @param types the target set
	 * @return the set of configured subtypes that are indexed
	 */
	IndexedTypeSet getIndexedTypesPolymorphic(IndexedTypeSet types);

	/**
	 * @return {@code true} if JMX is enabled
	 */
	boolean isJMXEnabled();

	/**
	 * Retrieve the statistics implementor instance for this factory.
	 *
	 * @return The statistics implementor.
	 */
	StatisticsImplementor getStatisticsImplementor();

	/**
	 * @return {@code true} if we are allowed to inspect entity state to skip some indexing operations.
	 * Can be disabled to get pre-3.4 behavior which always rebuilds the document.
	 */
	boolean isDirtyChecksEnabled();

	/**
	 * @return Returns the {@code IndexManagerHolder} which gives access to all index managers known to this factory
	 */
	IndexManagerHolder getIndexManagerHolder();

	/**
	 * @return an instance of {@code InstanceInitializer} for class/object initialization.
	 */
	InstanceInitializer getInstanceInitializer();

	/**
	 * @return the globally configured TimingSource, which we use to implement timeouts during query execution.
	 */
	TimingSource getTimingSource();

	/**
	 * @return the configuration properties for this factory
	 */
	Properties getConfigurationProperties();

	/**
	 * Returns the default {@code DatabaseRetrievalMethod}.
	 *
	 * This is either the system default or the default specified via the configuration property
	 * {@link org.hibernate.search.cfg.Environment#DATABASE_RETRIEVAL_METHOD}.
	 *
	 * @return returns the default {@code DatabaseRetrievalMethod}.
	 */
	DatabaseRetrievalMethod getDefaultDatabaseRetrievalMethod();

	/**
	 * Returns the default {@code ObjectLookupMethod}.
	 *
	 * This is either the system default or the default specified via the configuration property
	 * {@link org.hibernate.search.cfg.Environment#OBJECT_LOOKUP_METHOD}.
	 *
	 * @return returns the default {@code OBJECT_LOOKUP_METHOD}.
	 */
	ObjectLookupMethod getDefaultObjectLookupMethod();

	/**
	 * @return whether index uninverting should be done when running queries with sorts not covered by the configured sortable
	 * fields. If not allowed, an exception will be raised in this situation.
	 */
	boolean isIndexUninvertingAllowed();

	/**
	 * Returns a map of all known integrations keyed against the indexed manager type
	 *
	 * @return a map of all integrations keyed against the indexed manager type. The empty
	 * map is returned if there are no indexed types (hence no integrations).
	 */
	Map<IndexManagerType, SearchIntegration> getIntegrations();

	/**
	 * Retrieve the integration information for a given index manager type.
	 *
	 * @param indexManagerType the index manager type for which to retrieve the integration
	 *
	 * @return The corresponding integration
	 *
	 * @throws org.hibernate.search.exception.SearchException if the index manager type is unknown
	 */
	SearchIntegration getIntegration(IndexManagerType indexManagerType);

	/**
	 * Retrieve the scoped analyzer reference for a given indexed type.
	 *
	 * @param type The type for which to retrieve the analyzer.
	 *
	 * @return The scoped analyzer for the specified class.
	 *
	 * @throws java.lang.IllegalArgumentException in case {@code type == null} or the specified
	 * type is not an indexed entity.
	 */
	ScopedAnalyzerReference getAnalyzerReference(IndexedTypeIdentifier type);

}
