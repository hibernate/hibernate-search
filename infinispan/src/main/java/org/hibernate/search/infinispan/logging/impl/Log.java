/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.infinispan.logging.impl;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

import javax.naming.NamingException;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Hibernate Search Infinispan's log abstraction layer on top of JBoss Logging.
 *
 * @author Davide D'Alto
 * @since 4.0
 */
@MessageLogger(projectCode = "HSEARCH")
public interface Log extends org.hibernate.search.util.logging.impl.Log {

	@LogMessage(level = ERROR)
	@Message(id = 100055, value = "Unable to retrieve CacheManager from JNDI [%s]")
	void unableToRetrieveCacheManagerFromJndi(String jndiNamespace, @Cause NamingException ne);

	@LogMessage(level = ERROR)
	@Message(id = 100056, value = "Unable to release initial context")
	void unableToReleaseInitialContext(@Cause NamingException ne);

	@LogMessage(level = WARN)
	@Message(id = 100057, value = "Interrupted while waiting for asynchronous delete operations to be flushed on the index. "
			+ "Some stale segments might remain in the index.")
	void interruptedWhileWaitingForAsyncDeleteFlush();

}
