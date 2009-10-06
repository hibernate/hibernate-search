/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.reader.ReaderProvider;
import org.hibernate.search.store.DirectoryProvider;

/**
 * Provide application wide operations as well as access to the underlying Lucene resources.
 * @author Emmanuel Bernard
 */
public interface SearchFactory {
	/**
	 * Provide the configured readerProvider strategy,
	 * hence access to a Lucene IndexReader
	 */
	ReaderProvider getReaderProvider();

	/**
	 * Provide access to the DirectoryProviders (hence the Lucene Directories)
	 * for a given entity
	 * In most cases, the returned type will be a one element array.
	 * But if the given entity is configured to use sharded indexes, then multiple
	 * elements will be returned. In this case all of them should be considered.
	 */
	DirectoryProvider[] getDirectoryProviders(Class<?> entity);

	/**
	 * Optimize all indexes
	 */
	void optimize();

	/**
	 * Optimize the index holding <code>entityType</code>
	 */
	void optimize(Class entityType);

	/**
	 * Experimental API
	 * retrieve an analyzer instance by its definition name
	 * 
	 * @throws SearchException if the definition name is unknown
	 */
	Analyzer getAnalyzer(String name);
	
	/**
	 * Retrieves the scoped analyzer for a given class.
	 * 
	 * @param clazz The class for which to retrieve the analyzer.
	 * @return The scoped analyzer for the specified class.
	 * @throws IllegalArgumentException in case <code>clazz == null</code> or the specified
	 * class is not an indexed entity.
	 */
	Analyzer getAnalyzer(Class<?> clazz);
}
