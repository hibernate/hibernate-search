/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.timeout.impl;

import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

public class StubTimeoutManager extends TimeoutManager {

	public StubTimeoutManager(TimingSource timingSource, Long timeoutValue, TimeUnit timeoutUnit) {
		super( timingSource, timeoutValue, timeoutUnit, timeoutUnit == null ? null : Type.EXCEPTION );
	}
}
