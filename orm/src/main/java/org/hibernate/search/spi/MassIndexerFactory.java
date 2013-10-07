/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.spi;

import java.util.Properties;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;

/**
 * Contains methods that can be used to create a {@link MassIndexer}.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 *
 * @since 4.3
 */
public interface MassIndexerFactory {

	/**
	 * Called after the creation of the factory, can be used to read configuration parameters.
	 *
	 * @param properties
	 *            configuration properties
	 */
	void initialize(Properties properties);

	/**
	 * Create an instance of a {@link MassIndexer}.
	 *
	 * @param searchFactory
	 *            the Hibernate Search factory
	 * @param sessionFactory
	 *            the {@link org.hibernate.Session} factory
	 * @param entities
	 *            the classes of the entities that are going to be indexed
	 * @return a new MassIndexer
	 */
	MassIndexer createMassIndexer(SearchFactoryImplementor searchFactory, SessionFactoryImplementor sessionFactory,
			Class<?>... entities);

}
