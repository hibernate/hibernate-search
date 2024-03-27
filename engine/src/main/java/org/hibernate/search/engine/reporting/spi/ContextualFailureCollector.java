/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.reporting.spi;

import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

/**
 * A failure collector with an implicit context.
 * <p>
 * Implementations are thread-safe.
 *
 * @see FailureCollector
 */
public interface ContextualFailureCollector extends FailureCollector, EventContextProvider {

	boolean hasFailure();

	void add(Throwable t);

	void add(String failureMessage);

}
