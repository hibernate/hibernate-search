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
package org.hibernate.search.impl;

import java.util.Properties;

import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.impl.NRTIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This is the default {@code IndexManager} implementation for Hibernate Search.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 * @author Hardy Ferentschik
 */
public class DefaultIndexManagerFactory implements IndexManagerFactory, Startable {

	private static final Log log = LoggerFactory.make();

	private ServiceManager serviceManager;

	@Override
	public IndexManager createDefaultIndexManager() {
		return new DirectoryBasedIndexManager();
	}

	@Override
	public IndexManager createIndexManagerByName(String indexManagerImplementationName) {
		if ( StringHelper.isEmpty( indexManagerImplementationName ) ) {
			return createDefaultIndexManager();
		}
		else {
			indexManagerImplementationName = indexManagerImplementationName.trim();
			IndexManager indexManager = fromAlias( indexManagerImplementationName );
			if ( indexManager == null ) {
				indexManagerImplementationName = aliasToFQN( indexManagerImplementationName );
				Class<?> indexManagerClass = ClassLoaderHelper.classForName(
						indexManagerImplementationName,
						serviceManager
				);
				indexManager = ClassLoaderHelper.instanceFromClass(
						IndexManager.class,
						indexManagerClass,
						"index manager"
				);
			}
			log.indexManagerAliasResolved( indexManagerImplementationName, indexManager.getClass() );
			return indexManager;
		}
	}

	@Override
	public void start(Properties properties, BuildContext context) {
		this.serviceManager = context.getServiceManager();
	}

	/**
	 * Provide a way to expand known aliases to fully qualified class names.
	 * As opposed to {@link #fromAlias(String)} we can use this to expand to well
	 * known implementations which are optional on the classpath.
	 *
	 * @param alias the alias to replace with the fully qualified class name of the implementation
	 *
	 * @return the same name, or a fully qualified class name to use instead
	 */
	protected String aliasToFQN(final String alias) {
		// TODO Add the Infinispan implementor here
		return alias;
	}

	/**
	 * Extension point: allow to override aliases or add new ones to
	 * directly create class instances.
	 *
	 * @param alias the requested alias
	 *
	 * @return return the index manager for the given alias or {@code null} if the alias is unknown.
	 */
	protected IndexManager fromAlias(String alias) {
		if ( "directory-based".equals( alias ) ) {
			return new DirectoryBasedIndexManager();
		}
		if ( "near-real-time".equals( alias ) ) {
			return new NRTIndexManager();
		}
		return null;
	}
}
