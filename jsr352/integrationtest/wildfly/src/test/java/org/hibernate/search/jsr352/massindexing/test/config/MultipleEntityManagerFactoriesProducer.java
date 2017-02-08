/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.test.config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

/**
 * @author Yoann Rodiere
 */
@ApplicationScoped
public class MultipleEntityManagerFactoriesProducer {

	public static final String H2_ENTITY_MANAGER_FACTORY_BEAN_NAME = "h2_emf_bean";
	public static final String UNUSED_ENTITY_MANAGER_FACTORY_BEAN_NAME = "unused_emf_bean";

	@PersistenceUnit(unitName = "h2")
	private EntityManagerFactory h2PersistenceUnit;

	@PersistenceUnit(unitName = "unused_pu")
	private EntityManagerFactory unusedPersistenceUnit;

	@Produces
	@Singleton
	@Named(H2_ENTITY_MANAGER_FACTORY_BEAN_NAME)
	public EntityManagerFactory createH2PersistenceUnit() {
		return h2PersistenceUnit;
	}

	@Produces
	@Singleton
	@Named(UNUSED_ENTITY_MANAGER_FACTORY_BEAN_NAME)
	public EntityManagerFactory createUnusedPersistenceUnit() {
		return unusedPersistenceUnit;
	}

}
