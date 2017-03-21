/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.spring.jta.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.search.test.integration.spring.jta.entity.Box;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class BoxDAO {
	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public void persist(Box box) {
		entityManager.persist( box );
	}

	@Transactional
	public Box merge(Box box) {
		Box result = entityManager.merge( box );
		return result;
	}

	@Transactional
	public void remove(Box box) {
		entityManager.remove( box );
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}
}
