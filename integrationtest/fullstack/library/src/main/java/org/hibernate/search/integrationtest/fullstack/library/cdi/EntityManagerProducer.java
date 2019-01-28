/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.library.cdi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

@ApplicationScoped
public class EntityManagerProducer {

	@Inject
	@AutoIndexingEM
	private EntityManagerFactory autoIndexingEMF;

	@Inject
	@ManualIndexingEM
	private EntityManagerFactory manualIndexingEMF;

	@Produces
	@RequestScoped
	@AutoIndexingEM
	public EntityManagerWrapper autoIndexingEM() {
		return new EntityManagerWrapper( autoIndexingEMF.createEntityManager() );
	}

	@Produces
	@RequestScoped
	@ManualIndexingEM
	public EntityManagerWrapper manualIndexingEM() {
		return new EntityManagerWrapper( manualIndexingEMF.createEntityManager() );
	}

	public void close(@Disposes @Any EntityManagerWrapper wrapper) {
		wrapper.close();
	}
}
