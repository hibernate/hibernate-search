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
package org.hibernate.search.cfg.spi;

import org.hibernate.search.indexes.spi.IndexManager;

/**
 * By implementing this integration point you can customize the creation of IndexManager
 * instances.
 * Example usage it to override ClassLoaders used to resolve implementation names,
 * define new short-hand aliases, change the default implementation.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public interface IndexManagerFactory {

	/**
	 * @return a new instance of the default IndexManager
	 */
	IndexManager createDefaultIndexManager();

	/**
	 * @param indexManagerImplementationName how this is resolved to an IndexManager type
	 *   is left to the implementor.
	 * @return a new IndexManager instance of the chosen type
	 */
	IndexManager createIndexManagerByName(String indexManagerImplementationName);

}
