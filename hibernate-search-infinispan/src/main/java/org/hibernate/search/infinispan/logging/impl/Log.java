/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.infinispan.logging.impl;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

import javax.naming.NamingException;

import org.hibernate.search.SearchException;
import org.infinispan.remoting.transport.Address;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

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

	@Message(id = 100057, value = "Received am Infinispan custom command with unknown id '%1$b'")
	SearchException unknownInfinispanCommandId(byte commandId);

	@LogMessage(level = DEBUG)
	@Message(id = 100058, value = "Apply LuceneWork %s delegating to local indexing engine")
	void applyingChangeListLocally(Object workList);

	@LogMessage(level = DEBUG)
	@Message(id = 100059, value = "Going to ship LuceneWork %s to a remote master indexer")
	void applyingChangeListRemotely(Object workList);

	@LogMessage(level = WARN)
	@Message(id = 100060, value = "Index named '%1$s' is ignoring configuration option 'directory_provider' set to '%2$s':"
			+ " overriden to use the Infinispan Directory")
	void ignoreDirectoryProviderProperty(String indexName, String directoryOption);

	@LogMessage(level = DEBUG)
	@Message(id = 100061, value = "Sent list of LuceneWork %s to node %s")
	void workListRemotedTo(Object workList, Address primaryNodeAddress);

	@LogMessage(level = ERROR)
	@Message(id = 100062, value = "Collision on active IndexManagers named '%s' on the same Infinispan CacheManager!")
	void replacingRegisteredIndexManager(String name);

	@LogMessage(level = WARN)
	@Message(id = 100063, value = "Received remote command for index '%s' but the related IndexManager is either"
			+ " not yet fully started or unknown. Index might be out of sync!")
	void messageForUnknownIndexManager(String indexName);

}
