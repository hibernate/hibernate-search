/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.search.timeout.impl.LuceneCounterAdapter;
import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

import org.apache.lucene.index.QueryTimeout;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.util.Counter;

public abstract class HibernateSearchMultiCollectorManager
		implements CollectorManager<Collector, HibernateSearchMultiCollectorManager.MultiCollectedResults> {

	protected HibernateSearchMultiCollectorManager(TimeoutManager timeoutManager) {
		this.timeoutManager = timeoutManager;
	}

	public static HibernateSearchMultiCollectorManager create(
			TimeoutManager timeoutManager, Map<CollectorKey<?, ?>, CollectorManager<Collector, ?>> collectorManagers) {
		if ( collectorManagers.size() == 1 ) {
			// avoid using a multi collector if we have just one collector,
			// as MultiCollector explicitly ignores the total hit count optimization
			Map.Entry<CollectorKey<?, ?>, CollectorManager<Collector, ?>> entry =
					collectorManagers.entrySet().iterator().next();
			return new HibernateSearchMultiCollectorManagerSingle( timeoutManager, entry.getKey(), entry.getValue() );
		}
		else {
			return new HibernateSearchMultiCollectorManagerMulti( timeoutManager, collectorManagers );
		}
	}

	private final TimeoutManager timeoutManager;

	public HibernateSearchQueryTimeout queryTimeout() {
		Deadline deadline = timeoutManager.deadlineOrNull();
		if ( deadline != null ) {
			return new HibernateSearchQueryTimeout( timeoutManager, deadline );
		}
		return null;
	}


	private static class HibernateSearchMultiCollectorManagerMulti extends HibernateSearchMultiCollectorManager {
		private final Map<CollectorKey<?, ?>, CollectorManager<Collector, ?>> collectorManagers;

		private HibernateSearchMultiCollectorManagerMulti(TimeoutManager timeoutManager,
				Map<CollectorKey<?, ?>, CollectorManager<Collector, ?>> collectorManagers) {
			super( timeoutManager );
			this.collectorManagers = collectorManagers;
		}

		@Override
		public Collector newCollector() throws IOException {
			Collector[] collectors = new Collector[collectorManagers.size()];
			int i = 0;
			for ( CollectorManager<? extends Collector, ?> collectorManager : collectorManagers.values() ) {
				collectors[i++] = collectorManager.newCollector();
			}
			return MultiCollector.wrap( collectors );
		}

		@Override
		public MultiCollectedResults reduce(Collection<Collector> collectors) throws IOException {
			final int size = collectors.size();

			Map<CollectorKey<?, ?>, Object> results = new HashMap<>();

			int i = 0;
			for ( Map.Entry<CollectorKey<?, ?>, CollectorManager<Collector, ?>> entry : collectorManagers.entrySet() ) {
				List<Collector> toReduce = new ArrayList<>( size );
				for ( Collector collector : collectors ) {
					toReduce.add( ( (MultiCollector) collector ).getCollectors()[i] );
				}
				results.put( entry.getKey(), entry.getValue().reduce( toReduce ) );
				i++;
			}
			return new MultiCollectedResults( results );
		}

	}


	private static class HibernateSearchMultiCollectorManagerSingle extends HibernateSearchMultiCollectorManager {
		private final CollectorKey<?, ?> key;
		private final CollectorManager<Collector, ?> collectorManager;

		private HibernateSearchMultiCollectorManagerSingle(TimeoutManager timeoutManager, CollectorKey<?, ?> key,
				CollectorManager<Collector, ?> collectorManager) {
			super( timeoutManager );
			this.key = key;
			this.collectorManager = collectorManager;
		}

		@Override
		public Collector newCollector() throws IOException {
			return collectorManager.newCollector();
		}

		@Override
		public MultiCollectedResults reduce(Collection<Collector> collectors) throws IOException {
			return new MultiCollectedResults( Collections.singletonMap( key, collectorManager.reduce( collectors ) ) );
		}
	}

	public static class MultiCollectedResults {

		public static final MultiCollectedResults EMPTY = new MultiCollectedResults( Collections.emptyMap() );
		private final Map<CollectorKey<?, ?>, Object> results;

		public MultiCollectedResults(Map<CollectorKey<?, ?>, Object> results) {
			this.results = results;
		}

		@SuppressWarnings("unchecked")
		public <C extends Collector, T> T get(CollectorKey<C, T> key) {
			return (T) results.get( key );
		}
	}

	@SuppressWarnings("unchecked")

	public static class Builder {

		private final CollectorExecutionContext executionContext;
		private final TimeoutManager timeoutManager;

		private final Map<CollectorKey<?, ?>, CollectorManager<Collector, ?>> components = new LinkedHashMap<>();

		public Builder(CollectorExecutionContext executionContext, TimeoutManager timeoutManager) {
			this.executionContext = executionContext;
			this.timeoutManager = timeoutManager;
		}

		public <C extends Collector, T> void add(CollectorKey<C, T> key,
				CollectorManager<? extends Collector, ? extends T> collectorManager) {
			components.put( key, (CollectorManager<Collector, ?>) collectorManager );
		}

		public void addAll(Set<CollectorFactory<?, ?, ?>> collectorFactories) throws IOException {
			for ( CollectorFactory<?, ?, ?> collectorFactory : collectorFactories ) {
				add( collectorFactory );
			}
		}

		public <C extends Collector, T, CM extends CollectorManager<C, T>> void add(CollectorFactory<C, T, CM> collectorFactory)
				throws IOException {
			CollectorManager<C, T> collectorManager = collectorFactory.createCollectorManager( executionContext );
			components.put( collectorFactory.getCollectorKey(), (CollectorManager<Collector, ?>) collectorManager );
		}

		public HibernateSearchMultiCollectorManager build() {
			if ( components.isEmpty() ) {
				return null;
			}

			return create( timeoutManager, components );
		}
	}

	public static final class HibernateSearchQueryTimeout implements QueryTimeout {
		private final Deadline deadline;
		private final Counter clock;
		private final long baseline;
		private final long timeout;

		private boolean reached = false;

		public HibernateSearchQueryTimeout(TimeoutManager timeoutManager, Deadline deadline) {
			this.deadline = deadline;
			this.clock = new LuceneCounterAdapter( timeoutManager.timingSource() );
			this.baseline = timeoutManager.timeoutBaseline();
			// The timeout starts from the given baseline, not from when the collector is first used.
			// This is important because some collectors are applied during a second search.
			this.timeout = baseline + deadline.checkRemainingTimeMillis();

		}

		public boolean isReached() {
			return reached;
		}

		@Override
		public boolean shouldExit() {
			if ( clock.get() > timeout ) {
				reached = true;
				deadline.forceTimeout( null );
				return true;
			}
			return false;
		}
	}
}
