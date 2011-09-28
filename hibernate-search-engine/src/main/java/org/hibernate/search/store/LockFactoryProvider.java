/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.store;

import java.io.File;
import java.util.Properties;

import org.apache.lucene.store.LockFactory;

/**
 * To use a custom implementation of org.apache.lucene.store.LockFactory
 * you need to implement this interface and define the fully qualified
 * classname of the factory implementation as a DirectoryProvider parameter
 * for the locking_strategy key.
 * The implementation must have a no-arg constructor.
 *
 * @author Sanne Grinovero
 */
public interface LockFactoryProvider {
	
	/**
	 * Creates a LockFactory implementation.
	 * A different LockFactory is created for each DirectoryProvider.
	 * @param indexDir path to the indexBase setting, or null for
	 * DirectoryProviders which don't rely on filesystem
	 * @param dirConfiguration the properties set on the current DirectoryProvider
	 * @return the created LockFactory
	 */
	LockFactory createLockFactory(File indexDir, Properties dirConfiguration);

}
