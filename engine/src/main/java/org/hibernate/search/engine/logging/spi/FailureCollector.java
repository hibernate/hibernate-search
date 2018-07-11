/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.logging.spi;

import org.hibernate.search.util.FailureContext;
import org.hibernate.search.util.FailureContextElement;

/**
 * A failure collector without any context.
 * <p>
 * Allows to create a {@link ContextualFailureCollector}.
 * <p>
 * Failure collectors allow to register (non-fatal) failures occurring during bootstrap in particular,
 * so as to remember that a failure occurred and the process should be aborted at some point,
 * while still continuing the process for some time to collect other errors that could be relevant to users.
 */
public interface FailureCollector {

	ContextualFailureCollector withContext(FailureContext context);

	ContextualFailureCollector withContext(FailureContextElement contextElement);

}
