/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.util.impl.common.logging;

import static org.jboss.logging.Logger.Level.ERROR;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "HSEARCH-UTIL")
public interface Log extends BasicLogger {

	// Pre-existing message
	@LogMessage(level = ERROR)
	@Message(id = 17, value = "Work discarded, thread was interrupted while waiting for space to schedule: %1$s")
	void interruptedWorkError(Runnable r);

	@Message(id = 18, value = "'%1$s' must not be null.")
	IllegalArgumentException mustNotBeNull(String objectDescription);
}
