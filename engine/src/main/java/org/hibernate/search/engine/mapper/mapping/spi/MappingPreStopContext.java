/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.spi;

import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

/**
 * A pre-stop context for mappings.
 */
public interface MappingPreStopContext {

	/**
	 * A collector of (non-fatal) failures, allowing notification of Hibernate Search
	 * that something went wrong and an exception should be thrown at some point,
	 * while still continuing the shutdown process.
	 *
	 * @return A failure collector.
	 */
	ContextualFailureCollector failureCollector();

}
