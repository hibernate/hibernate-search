/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.timeout.impl;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.common.timing.spi.Deadline;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * @author Emmanuel Bernard
 */
public final class ElasticsearchTimeoutManager extends TimeoutManager implements Deadline {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static ElasticsearchTimeoutManager noTimeout(TimingSource timingSource, JsonObject query) {
		return new ElasticsearchTimeoutManager( timingSource, query, null, null, Type.NONE );
	}

	public static ElasticsearchTimeoutManager softTimeout(TimingSource timingSource, JsonObject query, long timeout, TimeUnit timeUnit) {
		return new ElasticsearchTimeoutManager( timingSource, query, timeout, timeUnit, Type.LIMIT );
	}

	public static ElasticsearchTimeoutManager hardTimeout(TimingSource timingSource, JsonObject query, long timeout, TimeUnit timeUnit) {
		return new ElasticsearchTimeoutManager( timingSource, query, timeout, timeUnit, Type.EXCEPTION );
	}

	private final Long timeoutValue;
	private final TimeUnit timeoutUnit;
	private final JsonObject query;

	private ElasticsearchTimeoutManager(TimingSource timingSource, JsonObject query, Long timeoutValue, TimeUnit timeoutUnit, Type type) {
		super( timingSource, timeoutUnit == null ? null : timeoutUnit.toMillis( timeoutValue ), type );
		this.timeoutValue = timeoutValue;
		this.timeoutUnit = timeoutUnit;
		this.query = query;
	}

	@Override
	protected void onTimedOut() {
		if ( hasHardTimeout() ) {
			throw log.clientSideTimedOut( Duration.ofNanos( timeoutUnit.toNanos( timeoutValue ) ), query.toString() );
		}
	}

	public boolean defined() {
		return timeoutValue != null && timeoutUnit != null;
	}

	public String timeoutString() {
		return checkTimeLeftInMilliseconds() + "ms";
	}
}
