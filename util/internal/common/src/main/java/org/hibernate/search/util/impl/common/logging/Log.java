/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.util.impl.common.logging;

import static org.jboss.logging.Logger.Level.ERROR;

import org.hibernate.search.util.impl.common.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.UTIL_ID_RANGE_MIN, max = MessageConstants.UTIL_ID_RANGE_MAX),
		// Exceptions for legacy messages from Search 5
		// TODO HSEARCH-3308 add exceptions here for legacy messages from Search 5.
		@ValidIdRange(min = 17, max = 17),
		@ValidIdRange(min = 18, max = 18)
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_1 = MessageConstants.ENGINE_ID_RANGE_MIN;

	@LogMessage(level = ERROR)
	@Message(id = 17, value = "Work discarded, thread was interrupted while waiting for space to schedule: %1$s")
	void interruptedWorkError(Runnable r);

	// TODO HSEARCH-3308 migrate relevant messages from Search 5 here

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET_2 = MessageConstants.UTIL_ID_RANGE_MIN;

	@Message(id = ID_OFFSET_2 + 0,
			value = "'%1$s' must not be null.")
	IllegalArgumentException mustNotBeNull(String objectDescription);

	@Message(id = ID_OFFSET_2 + 1,
			value = "'%1$s' must not be null or empty.")
	IllegalArgumentException collectionMustNotBeNullNorEmpty(String objectDescription);

	@Message(id = ID_OFFSET_2 + 2,
			value = "'%1$s' must be positive or zero.")
	IllegalArgumentException mustBePositiveOrZero(String objectDescription);

	@Message(id = ID_OFFSET_2 + 3,
			value = "'%1$s' must not be null or empty.")
	IllegalArgumentException stringMustNotBeNullNorEmpty(String objectDescription);
}
