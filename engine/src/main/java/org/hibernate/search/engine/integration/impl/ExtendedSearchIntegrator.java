/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.integration.impl;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.spi.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.TimingSource;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.stat.spi.StatisticsImplementor;

/**
 * Interface which gives access to runtime configuration. Intended to be used by Search components.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public interface ExtendedSearchIntegrator extends SearchIntegrator {

	/**
	 * Returns a map of all known entity index binding (indexed entities) keyed against the indexed type
	 *
	 * @return a map of all known entity index binding (indexed entities) keyed against the indexed type. The empty
	 * map is returned if there are no indexed types.
	 */
	Map<Class<?>, EntityIndexBinding> getIndexBindings();

	DocumentBuilderContainedEntity getDocumentBuilderContainedEntity(Class<?> entityType);

	FilterCachingStrategy getFilterCachingStrategy();

	FilterDef getFilterDefinition(String name);

	int getFilterCacheBitResultsSize();

	Set<Class<?>> getIndexedTypesPolymorphic(Class<?>[] classes);

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
}
