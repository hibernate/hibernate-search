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

import org.hibernate.search.util.StringHelper;
import org.hibernate.search.cfg.spi.IndexManagerFactory;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.impl.NRTIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This is the default IndexManager implementation for Hibernate Search.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class DefaultIndexManagerFactory implements IndexManagerFactory {

	private static final Log log = LoggerFactory.make();

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
			String implName = indexManagerImplementationName.trim();
			IndexManager im = fromAlias( implName );
			if ( im == null ) {
				implName = aliasToFQN( implName );
				im = ClassLoaderHelper.instanceFromName( IndexManager.class, implName,
						IndexManagerHolder.class, "index manager" );
			}
			log.indexManagerAliasResolved( indexManagerImplementationName, im.getClass() );
			return im;
		}
	}

	/**
	 * Provide a way to expand known aliases to fully qualified class names.
	 * As opposed to {@link #fromAlias(String)} we can use this to expend to well
	 * known implementations which are optional on the classpath.
	 *
	 * @param implName
	 * @return the same name, or a fully qualified class name to use instead
	 */
	protected String aliasToFQN(final String implName) {
		// TODO Add the Infinispan implementor here
		return implName;
	}

	/**
	 * Extension point: allow to override aliases or add new ones to
	 * directly create class instances.
	 *
	 * @param implName the requested alias
	 * @return <code>null</code> if the alias is unknown.
	 */
	protected IndexManager fromAlias(String implName) {
		if ( "directory-based".equals( implName ) ) {
			return new DirectoryBasedIndexManager();
		}
		if ( "near-real-time".equals( implName ) ) {
			return new NRTIndexManager();
		}
		return null;
	}
}
