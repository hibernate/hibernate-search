/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.coordination.common.spi;

import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

public interface CoordinationStrategyPreStopContext {

	/**
	 * A collector of (non-fatal) failures, allowing notification of Hibernate Search
	 * that something went wrong and an exception should be thrown at some point,
	 * while still continuing the shutdown process for some time to collect other errors
	 * that could be relevant to users.
	 *
	 * @return A failure collector.
	 */
	ContextualFailureCollector failureCollector();

}
