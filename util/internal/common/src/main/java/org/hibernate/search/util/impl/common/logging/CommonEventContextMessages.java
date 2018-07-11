/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.common.logging;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Common message bundle related to event contexts.
 */
@MessageBundle(projectCode = "HSEARCH")
public interface CommonEventContextMessages {

	@Message(value = "Context: ")
	String contextPrefix();

	@Message(value = ", ")
	String contextSeparator();

}
