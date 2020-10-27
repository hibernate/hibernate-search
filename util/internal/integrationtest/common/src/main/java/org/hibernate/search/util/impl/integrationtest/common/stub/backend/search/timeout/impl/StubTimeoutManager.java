/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.timeout.impl;

import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.cfg.spi.ConvertUtils;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

public class StubTimeoutManager extends TimeoutManager {

	public StubTimeoutManager(TimingSource timingSource, Long timeoutValue, TimeUnit timeoutUnit) {
		super( timingSource, ConvertUtils.toMilliseconds( timeoutValue, timeoutUnit ),
				timeoutUnit == null ? null : TimeUnit.MILLISECONDS,
				timeoutUnit == null ? null : Type.EXCEPTION );
	}
}
