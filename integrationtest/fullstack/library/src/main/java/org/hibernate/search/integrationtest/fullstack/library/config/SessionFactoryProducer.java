/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.library.config;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.hibernate.SessionFactory;

@Singleton
public class SessionFactoryProducer {

	private SessionFactory autoIndexingFactory;

	private SessionFactory manualIndexingFactory;

	@PostConstruct
	public void createFactories() {
		autoIndexingFactory = SessionFactoryConfig.sessionFactory( false );
		manualIndexingFactory = SessionFactoryConfig.sessionFactory( true );
	}

	@PreDestroy
	public void disposeFactories() {
		try {
			if ( autoIndexingFactory != null ) {
				autoIndexingFactory.close();
			}
		}
		finally {
			if ( manualIndexingFactory != null ) {
				manualIndexingFactory.close();
			}
		}
	}

	@Produces
	@AutoIndexing
	public SessionFactory getAutoIndexingFactory() {
		return autoIndexingFactory;
	}

	@Produces
	@ManualIndexing
	public SessionFactory getManualIndexingFactory() {
		return manualIndexingFactory;
	}
}
