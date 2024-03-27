/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.reporting.spi;

import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.EventContextElement;

/**
 * A failure collector without any context.
 * <p>
 * Allows to create a {@link ContextualFailureCollector}.
 * <p>
 * Failure collectors allow to register (non-fatal) failures occurring during bootstrap in particular,
 * so as to remember that a failure occurred and the process should be aborted at some point,
 * while still continuing the process for some time to collect other errors that could be relevant to users.
 * <p>
 * Implementations are thread-safe.
 */
public interface FailureCollector {

	ContextualFailureCollector withContext(EventContext context);

	ContextualFailureCollector withContext(EventContextElement contextElement);

}
