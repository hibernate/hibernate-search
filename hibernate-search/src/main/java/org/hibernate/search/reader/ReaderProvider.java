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
package org.hibernate.search.reader;

import java.util.Properties;

import org.apache.lucene.index.IndexReader;

import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.DirectoryProvider;

/**
 * Responsible for providing and managing the lifecycle of a read only reader. The implementation must have a
 * no-arg constructor.
 * <p/>
 * Note that the reader must be closed once opened.
 *
 * @author Emmanuel Bernard
 */
public interface ReaderProvider {
	/**
	 * Open a read-only reader on all the listed directory providers.
	 * The opened reader has to be closed through {@link #closeReader(IndexReader)}.
	 * The opening can be virtual.
	 */
	IndexReader openReader(DirectoryProvider... directoryProviders);

	/**
	 * Close a reader previously opened by {@link #openReader}.
	 * The closing can be virtual.
	 */
	void closeReader(IndexReader reader);

	/**
	 * Initialize the reader provider before its use.
	 */
	void initialize(Properties props, BuildContext context);

	/**
	 * Called when a <code>SearchFactory</code> is destroyed. This method typically releases resources.
	 * It is guaranteed to be executed after readers are released by queries (assuming no user error). 
	 */
	void destroy();
}
