/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.timeout.impl;

import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

import org.apache.lucene.util.Counter;

/**
 * @author Emmanuel Bernard
 */
public final class LuceneTimeoutManager extends TimeoutManager {

	public static LuceneTimeoutManager noTimeout(TimingSource timingSource) {
		return new LuceneTimeoutManager( timingSource, null, null, Type.NONE );
	}

	public static LuceneTimeoutManager softTimeout(TimingSource timingSource, long timeout,
			TimeUnit timeUnit) {
		return new LuceneTimeoutManager( timingSource, timeout, timeUnit, Type.LIMIT );
	}

	public static LuceneTimeoutManager hardTimeout(TimingSource timingSource, long timeout,
			TimeUnit timeUnit) {
		return new LuceneTimeoutManager( timingSource, timeout, timeUnit, Type.EXCEPTION );
	}

	private LuceneTimeoutManager(TimingSource timingSource, Long timeoutValue, TimeUnit timeoutUnit,
			Type type) {
		super( timingSource, timeoutValue, timeoutUnit, type );
	}

	public Counter createCounter() {
		return new LuceneCounterAdapter( timingSource );
	}

}
