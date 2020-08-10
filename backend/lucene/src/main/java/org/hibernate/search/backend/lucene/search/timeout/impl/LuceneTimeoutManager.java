/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.timeout.impl;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;
import org.apache.lucene.util.Counter;

/**
 * @author Emmanuel Bernard
 */
public final class LuceneTimeoutManager extends TimeoutManager {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static LuceneTimeoutManager noTimeout(TimingSource timingSource, Query query) {
		return new LuceneTimeoutManager( timingSource, query, null, null, Type.NONE );
	}

	public static LuceneTimeoutManager softTimeout(TimingSource timingSource, Query query, long timeout, TimeUnit timeUnit) {
		return new LuceneTimeoutManager( timingSource, query, timeout, timeUnit, Type.LIMIT );
	}

	public static LuceneTimeoutManager hardTimeout(TimingSource timingSource, Query query, long timeout, TimeUnit timeUnit) {
		return new LuceneTimeoutManager( timingSource, query, timeout, timeUnit, Type.EXCEPTION );
	}

	private final Long timeoutValue;
	private final TimeUnit timeoutUnit;
	private final Query query;

	private LuceneTimeoutManager(TimingSource timingSource, Query query, Long timeoutValue, TimeUnit timeoutUnit, Type type) {
		super( timingSource, timeoutUnit == null ? null : timeoutUnit.toMillis( timeoutValue ), type );
		this.timeoutValue = timeoutValue;
		this.timeoutUnit = timeoutUnit;
		this.query = query;
	}

	public Counter createCounter() {
		return new LuceneCounterAdapter( timingSource );
	}

	@Override
	protected void onTimedOut() {
		if ( hasHardTimeout() ) {
			throw log.timedOut( Duration.ofNanos( timeoutUnit.toNanos( timeoutValue ) ), query.toString() );
		}
	}
}
