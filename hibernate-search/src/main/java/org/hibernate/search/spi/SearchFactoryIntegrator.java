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

import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.Worker;

/**
 * This contract is considered experimental.
 *
 * This contract gives access to lower level APIs of Hibernate Search for
 * frameworks integrating with it. The piece of code creating the SearchFactory should
 * use this contract. It should however pass the higher level {@link SearchFactory} contract to
 * its clients.
 *
 * It also allows modification of some of the search factory internals:
 *  - today allow addition of new indexed classes.
 *
 * @experimental
 * @author Emmanuel Bernard
 */
public interface SearchFactoryIntegrator extends SearchFactory {
	/**
	 * Add the following classes to the SearchFactory
	 *
	 */
	void addClasses(Class<?>... classes);

	//TODO consider accepting SearchConfiguration or SearchMapping

	Worker getWorker();

	void close();
}
