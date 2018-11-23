/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing;

import java.util.Properties;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Contains methods that can be used to create a {@link MassIndexer}.
 *
 * @author Davide D'Alto
 *
 * @since 4.3
 */
public interface MassIndexerFactory {
	String MASS_INDEXER_FACTORY_CLASSNAME = "hibernate.search.massindexer.factoryclass";

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
	 * @param sessionFactory
	 *            the {@link org.hibernate.Session} factory
	 * @param entities
	 *            the classes of the entities that are going to be indexed
	 * @return a new {@link MassIndexer}
	 */
	MassIndexer createMassIndexer(SessionFactoryImplementor sessionFactory, Class<?>... entities);

}
