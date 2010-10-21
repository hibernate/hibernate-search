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
package org.hibernate.search.store.optimization;

import java.util.Properties;

import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.store.DirectoryProvider;

/**
 * @author Emmanuel Bernard
 */
public interface OptimizerStrategy {
	public void initialize(DirectoryProvider directoryProvider, Properties indexProperties, BuildContext initContext);

	/**
	 * has to be called in a thread safe way
	 */
	void optimizationForced();

	/**
	 * has to be called in a thread safe way
	 */
	boolean needOptimization();

	/**
	 * has to be called in a thread safe way
	 */
	public void addTransaction(long operations);

	/**
	 * has to be called in a thread safe way
	 */
	void optimize(Workspace workspace);

}
