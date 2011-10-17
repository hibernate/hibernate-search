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

import java.util.Properties;

import org.apache.lucene.store.Directory;

import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.BuildContext;

/**
 * Set up and provide a Lucene {@code Directory}
 * {@code equals()} and {@code hashCode()} must guaranty equality
 * between two providers pointing to the same underlying Lucene Store.
 * Besides that, {@code hashCode} ordering is used to avoid deadlock when locking a directory provider.
 * 
 * This class must be thread safe regarding {@code getDirectory()} calls
 *
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 */
public interface DirectoryProvider<TDirectory extends Directory> {

	/**
	 * @param directoryProviderName
	 * @param properties
	 * @param context
	 *
	 * get the information to initialize the directory and build its hashCode/equals method
	 */
	void initialize(String directoryProviderName, Properties properties, BuildContext context);

	/**
	 * Executed after initialize, this method set up the heavy process of starting up the DirectoryProvider
	 * IO processing as well as background processing are expected to be set up here
	 *
	 */
	void start(DirectoryBasedIndexManager indexManager);

	/**
	 * Executed when the search factory is closed. This method should stop any background process as well as
	 * releasing any resource.
	 * This method should avoid raising exceptions and log potential errors instead
	 */
	void stop();

	/**
	 * Returns an initialized Lucene Directory. This method call <b>must</b> be threadsafe
	 */
	TDirectory getDirectory();
}

