/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.elasticsearch.logging.impl;

import java.util.Map;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "HSEARCH-ES")
public interface Log extends BasicLogger {

	@Message(id = 1, value = "[%1$s] Executing %2$s with parameters %3$s and body <%4$s>")
	@LogMessage(level = Level.TRACE)
	void executingWork(String host, String workType, Map<String, String> parameters, String bodyAsString);

}
