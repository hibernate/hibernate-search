/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jmx.impl;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.jmx.IndexingProgressMonitorMBean;
import org.hibernate.search.jmx.StatisticsInfoMBean;
import org.hibernate.search.stat.Statistics;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Helper class to register JMX beans.
 *
 * @author Hardy Ferentschik
 */
public final class JMXRegistrar {
	private static final Log log = LoggerFactory.make();

	private JMXRegistrar() {
	}

	public static String buildMBeanName(String defaultName, String suffix) {
		String objectName = defaultName;
		if ( !StringHelper.isEmpty( suffix ) ) {
			objectName += "[" + suffix + "]";
		}
		return objectName;
	}

	/**
	 * Registers the specified object with the given name to the MBean server.
	 *
	 * @param <T> the type of the object interface
	 * @param object the object to register
	 * @param beanInterface the Management Interface exported by this MBean's implementation.
	 * @param name the object name to register the bean under
	 *
	 * @return The registered object name
	 */
	public static <T> String registerMBean(T object, Class<T> beanInterface, String name) {
		ObjectName objectName = createObjectName( name );
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			StandardMBean mbean = new StandardMBean( object, beanInterface );
			mbs.registerMBean( mbean, objectName );
		}
		catch (Exception e) {
			throw new SearchException( "Unable to enable MBean for Hibernate Search", e );
		}
		return objectName.toString();
	}

	/**
	 * Unregister the MBean with the specified name.
	 *
	 * @param name The name of the bean to unregister. The {@code name} cannot be {@code null}
	 *
	 * @throws java.lang.IllegalArgumentException In case the object name is {@code null}
	 */
	public static void unRegisterMBean(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "The object name cannot be null" );
		}
		ObjectName objectName = createObjectName( name );
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		if ( mbs.isRegistered( objectName ) ) {
			try {
				mbs.unregisterMBean( objectName );
			}
			catch (Exception e) {
				log.unableToUnregisterExistingMBean( name, e );
			}
		}
	}

	/**
	 * Checks whether a bean is registered under the given  name.
	 *
	 * @param name the object name to check (as string)
	 *
	 * @return {@code true} is there is a bean registered under the given name, {@code false} otherwise.
	 *
	 * @throws java.lang.IllegalArgumentException In case the object name is {@code null}
	 */
	public static boolean isNameRegistered(String name) {
		if ( name == null ) {
			throw new IllegalArgumentException( "The object name cannot be null" );
		}
		ObjectName objectName = createObjectName( name );
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		return mbs.isRegistered( objectName );
	}

	private static ObjectName createObjectName(String name) {
		ObjectName objectName;
		try {
			objectName = new ObjectName( name );
		}
		catch (MalformedObjectNameException e) {
			throw new SearchException( "Invalid JMX Bean name: " + name, e );
		}
		return objectName;
	}

	/**
	 * @author Hardy Ferentschik
	 */
	public static class StatisticsInfo implements StatisticsInfoMBean {
		private final Statistics delegate;

		public StatisticsInfo(Statistics delegate) {
			this.delegate = delegate;
		}

		@Override
		public void clear() {
			delegate.clear();
		}

		@Override
		public long getSearchQueryExecutionCount() {
			return delegate.getSearchQueryExecutionCount();
		}

		@Override
		public long getSearchQueryTotalTime() {
			return delegate.getSearchQueryTotalTime();
		}

		@Override
		public long getSearchQueryExecutionMaxTime() {
			return delegate.getSearchQueryExecutionMaxTime();
		}

		@Override
		public long getSearchQueryExecutionAvgTime() {
			return delegate.getSearchQueryExecutionAvgTime();
		}

		@Override
		public String getSearchQueryExecutionMaxTimeQueryString() {
			return delegate.getSearchQueryExecutionMaxTimeQueryString();
		}

		@Override
		public long getObjectLoadingTotalTime() {
			return delegate.getObjectLoadingTotalTime();
		}

		@Override
		public long getObjectLoadingExecutionMaxTime() {
			return delegate.getObjectLoadingExecutionMaxTime();
		}

		@Override
		public long getObjectLoadingExecutionAvgTime() {
			return delegate.getObjectLoadingExecutionAvgTime();
		}

		@Override
		public long getObjectsLoadedCount() {
			return delegate.getObjectsLoadedCount();
		}

		@Override
		public boolean isStatisticsEnabled() {
			return delegate.isStatisticsEnabled();
		}

		@Override
		public void setStatisticsEnabled(boolean b) {
			delegate.setStatisticsEnabled( b );
		}

		@Override
		public String getSearchVersion() {
			return delegate.getSearchVersion();
		}

		@Override
		public Set<String> getIndexedClassNames() {
			return delegate.getIndexedClassNames();
		}

		@Override
		public int getNumberOfIndexedEntities(String entity) {
			return delegate.getNumberOfIndexedEntities( entity );
		}

		@Override
		public Map<String, Integer> indexedEntitiesCount() {
			return delegate.indexedEntitiesCount();
		}
	}

	/**
	 * A JMX based mass indexer progress monitor. This monitor will allow you to follow mass indexing progress via JMX.
	 *
	 * @author Hardy Ferentschik
	 */
	public static class IndexingProgressMonitor implements IndexingProgressMonitorMBean, MassIndexerProgressMonitor {
		private static final Log log = LoggerFactory.make();

		private final AtomicLong documentsDoneCounter = new AtomicLong();
		private final AtomicLong documentsBuiltCounter = new AtomicLong();
		private final AtomicLong totalCounter = new AtomicLong();
		private final AtomicLong entitiesLoadedCounter = new AtomicLong();

		private final String registeredName;

		public IndexingProgressMonitor() {
			String name = IndexingProgressMonitorMBean.INDEXING_PROGRESS_MONITOR_MBEAN_OBJECT_NAME;
			if ( isNameRegistered( name ) ) {
				name = name + "@" + Integer.toHexString( hashCode() ); // make the name unique in case there are multiple mass indexers at the same time
			}
			registeredName = registerMBean( this, IndexingProgressMonitorMBean.class, name );
		}

		@Override
		public final void documentsAdded(long increment) {
			documentsDoneCounter.addAndGet( increment );
		}

		@Override
		public final void documentsBuilt(int number) {
			documentsBuiltCounter.addAndGet( number );
		}

		@Override
		public final void entitiesLoaded(int size) {
			entitiesLoadedCounter.addAndGet( size );
		}

		@Override
		public final void addToTotalCount(long count) {
			totalCounter.addAndGet( count );
		}

		@Override
		public final void indexingCompleted() {
			log.indexingCompletedAndMBeanUnregistered( totalCounter.get() );
			unRegisterMBean( registeredName );
		}

		@Override
		public final long getLoadedEntitiesCount() {
			return entitiesLoadedCounter.get();
		}

		@Override
		public final long getDocumentsAddedCount() {
			return documentsDoneCounter.get();
		}

		@Override
		public final long getNumberOfEntitiesToIndex() {
			return totalCounter.get();
		}
	}
}


