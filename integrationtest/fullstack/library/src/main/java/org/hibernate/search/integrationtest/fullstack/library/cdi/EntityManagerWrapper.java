/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.fullstack.library.cdi;

import javax.persistence.EntityManager;

import org.hibernate.search.integrationtest.fullstack.library.repo.EntityManagerProvider;

/**
 * Wrapper of {@link EntityManager}
 *
 * This is a workaround to allow the unwrapping of the Hibernate session from a JPA entityManager injected using Weld in a Java SE environment.
 * It simulates what the JBoss JPA module does in the Java EE environment.
 * See the Weld issue: https://issues.jboss.org/browse/WELD-2245
 */
public class EntityManagerWrapper implements EntityManagerProvider {

	private EntityManager entityManager;

	public EntityManagerWrapper(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public EntityManager entityManager() {
		return entityManager;
	}

	public void close() {
		if ( entityManager != null ) {
			entityManager.close();
		}
	}
}
