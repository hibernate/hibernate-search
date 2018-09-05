/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jsr352.massindexing.test.config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.search.test.integration.jsr352.massindexing.test.common.MessageManager;

/**
 * @author Yoann Rodiere
 */
@ApplicationScoped
public class MultipleEntityManagerFactoriesProducer {

	public static final String PRIMARY_PERSISTENCE_UNIT_NAME = MessageManager.PERSISTENCE_UNIT_NAME;
	public static final String UNUSED_PERSISTENCE_UNIT_NAME = "unused_pu";

	public static final String PRIMARY_ENTITY_MANAGER_FACTORY_BEAN_NAME = "primary_emf_bean";
	private static final String UNUSED_ENTITY_MANAGER_FACTORY_BEAN_NAME = "unused_emf_bean";

	@PersistenceUnit(unitName = PRIMARY_PERSISTENCE_UNIT_NAME)
	private EntityManagerFactory h2PersistenceUnit;

	@PersistenceUnit(unitName = UNUSED_PERSISTENCE_UNIT_NAME)
	private EntityManagerFactory unusedPersistenceUnit;

	@Produces
	@Singleton
	@Named(PRIMARY_ENTITY_MANAGER_FACTORY_BEAN_NAME)
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
