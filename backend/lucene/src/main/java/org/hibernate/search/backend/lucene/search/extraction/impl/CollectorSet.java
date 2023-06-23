/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.search.timeout.impl.LuceneCounterAdapter;
import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.TimeLimitingCollector;

public class CollectorSet {

	private final Collector composed;
	private final Map<CollectorKey<?>, Collector> components;

	private CollectorSet(Collector composed, Map<CollectorKey<?>, Collector> components) {
		this.composed = composed;
		this.components = components;
	}

	public Collector getComposed() {
		return composed;
	}

	@SuppressWarnings("unchecked")
	public <C extends Collector> C get(CollectorKey<C> key) {
		return (C) components.get( key );
	}

	public static class Builder {

		private final CollectorExecutionContext executionContext;
		private final TimeoutManager timeoutManager;

		private final Map<CollectorKey<?>, Collector> components = new LinkedHashMap<>();

		public Builder(CollectorExecutionContext executionContext, TimeoutManager timeoutManager) {
			this.executionContext = executionContext;
			this.timeoutManager = timeoutManager;
		}

		public <C extends Collector> void add(CollectorKey<C> key, C collector) {
			components.put( key, collector );
		}

		public void addAll(Set<CollectorFactory<?>> collectorFactories) throws IOException {
			for ( CollectorFactory<?> collectorFactory : collectorFactories ) {
				Collector collector = collectorFactory.createCollector( executionContext );
				components.put( collectorFactory.getCollectorKey(), collector );
			}
		}

		public CollectorSet build() {
			if ( components.isEmpty() ) {
				return new CollectorSet( null, components );
			}

			Collector composed = wrapTimeLimitingCollectorIfNecessary(
					// avoid to use a multi collector if we have just one collector,
					// as MultiCollector explicitly ignores the total hit count optimization
					( components.size() == 1 )
							? components.values().iterator().next()
							: MultiCollector.wrap( components.values() ),
					timeoutManager
			);

			return new CollectorSet( composed, components );
		}

		private Collector wrapTimeLimitingCollectorIfNecessary(Collector collector, TimeoutManager timeoutManager) {
			final Deadline deadline = timeoutManager.deadlineOrNull();
			if ( deadline != null ) {
				TimeLimitingCollector wrapped = new TimeLimitingCollector( collector,
						new LuceneCounterAdapter( timeoutManager.timingSource() ),
						deadline.checkRemainingTimeMillis() );
				// The timeout starts from the given baseline, not from when the collector is first used.
				// This is important because some collectors are applied during a second search.
				wrapped.setBaseline( timeoutManager.timeoutBaseline() );
				return wrapped;
			}
			return collector;
		}
	}

}
