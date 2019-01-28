/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.library.cdi;

import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

@ApplicationScoped
public class EntityManagerFactoryProducer {

	@Inject
	private BeanManager beanManager;

	@Produces
	@ApplicationScoped
	@AutoIndexingEM
	public EntityManagerFactory autoIndexingEMF() {
		Map<String, Object> props = new HashMap<>();
		props.put( "javax.persistence.bean.manager", beanManager );
		return Persistence.createEntityManagerFactory( "elasticsearchAutoIndexing", props );
	}

	@Produces
	@ApplicationScoped
	@ManualIndexingEM
	public EntityManagerFactory manualIndexingEMF() {
		Map<String, Object> props = new HashMap<>();
		props.put( "javax.persistence.bean.manager", beanManager );
		return Persistence.createEntityManagerFactory( "elasticsearchManualIndexing", props );
	}

	public void close(@Disposes @Any EntityManagerFactory entityManagerFactory) {
		entityManagerFactory.close();
	}
}
