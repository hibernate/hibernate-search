/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batchindexing.impl;

import java.util.Properties;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.batchindexing.spi.MassIndexerFactory;

/**
 * The {@link MassIndexer} implementation used when none is specified in the configuration.
 *
 * @author Davide D'Alto
 */
public class DefaultMassIndexerFactory implements MassIndexerFactory {

	private static final Class<?>[] OBJECT_ARRAY = { Object.class };

	@Override
	public void initialize(Properties properties) {
	}

	@Override
	public MassIndexer createMassIndexer(SearchIntegrator searchIntegrator, SessionFactoryImplementor sessionFactory,
			Class<?>... entities) {
		final Class<?>[] types = entities.length == 0 ? OBJECT_ARRAY : entities;
		return new MassIndexerImpl( searchIntegrator, sessionFactory, types );
	}

}
