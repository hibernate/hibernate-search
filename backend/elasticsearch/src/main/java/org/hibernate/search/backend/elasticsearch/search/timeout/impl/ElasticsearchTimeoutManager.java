/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.timeout.impl;

import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

/**
 * @author Emmanuel Bernard
 */
public final class ElasticsearchTimeoutManager extends TimeoutManager {

	public static ElasticsearchTimeoutManager noTimeout(TimingSource timingSource) {
		return new ElasticsearchTimeoutManager( timingSource, null, null, Type.NONE );
	}

	public static ElasticsearchTimeoutManager softTimeout(TimingSource timingSource, long timeout,
			TimeUnit timeUnit) {
		return new ElasticsearchTimeoutManager( timingSource, timeout, timeUnit, Type.LIMIT );
	}

	public static ElasticsearchTimeoutManager hardTimeout(TimingSource timingSource, long timeout,
			TimeUnit timeUnit) {
		return new ElasticsearchTimeoutManager( timingSource, timeout, timeUnit, Type.EXCEPTION );
	}

	private ElasticsearchTimeoutManager(TimingSource timingSource, Long timeoutValue,
			TimeUnit timeoutUnit, Type type) {
		super( timingSource, timeoutValue, timeoutUnit, type );
	}

}
