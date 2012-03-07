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
package org.hibernate.search.spi;

/**
 * Build context for the worker and other backend
 * Available after all index, entity metadata are built.
 *
 * @author Emmanuel Bernard
 */
public interface WorkerBuildContext extends BuildContext {

	/**
	 * @return {@code true} if a transaction manager is expected, {@code false} otherwise.
	 *
	 * @see org.hibernate.search.cfg.spi.SearchConfiguration#isTransactionManagerExpected
	 */
	boolean isTransactionManagerExpected();

	/**
	 * @return {@code true} if it is safe to assume that the information we have about
	 * index metadata is accurate. This should be set to false for example if the index
	 * could contain Documents related to types not known to this SearchFactory instance.
	 *
	 * @see org.hibernate.search.cfg.spi.SearchConfiguration#isIndexMetadateComplete
	 */
	boolean isIndexMetadataComplete();

	/**
	 * @return An instance of the {@code InstanceInitializer} interface.
	 */
	InstanceInitializer getInstanceInitializer();
}
