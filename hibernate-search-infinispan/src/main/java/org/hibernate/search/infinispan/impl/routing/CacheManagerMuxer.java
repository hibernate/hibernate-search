/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.infinispan.impl.routing;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.infinispan.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * An Infinispan CacheManager needs to know to which Hibernate Search
 * instance an incoming RPC command needs to be routed.
 * This is a placeholder for such a reference, to be registered in the Cache's
 * ComponentRegistry.
 * We also need to deal with multiple Hibernate Search instances sharing a same
 * cache and/or Lucene index stored in that Cache.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class CacheManagerMuxer {

	private static final Log log = LoggerFactory.make( Log.class );

	/**
	 * Known active IndexManager to be looked up by name. 
	 */
	private final ConcurrentMap<String,IndexManager> storage = new ConcurrentHashMap<String,IndexManager>();

	public void disableIndexManager(String name) {
		storage.remove( name );
	}

	public void activateIndexManager(String name, IndexManager im) {
		IndexManager previousInstance = storage.put( name, im );
		if ( previousInstance != null ) {
			log.replacingRegisteredIndexManager( name );
		}
	}

	/**
	 * @param indexName the name of the IndexManager being requests (matches the index name)
	 * @return the matching IndexManager, if it exists and is able to process commands, or <code>null</code> otherwise.
	 */
	public IndexManager getActiveIndexManager(String indexName) {
		//Might not exist, caller has to deal with it.
		return storage.get( indexName );
	}

}
