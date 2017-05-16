/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.spring.jta.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.search.test.integration.spring.jta.entity.Muffin;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MuffinDAO {
	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public void persist(Muffin muffin) {
		entityManager.persist( muffin );
	}

	@Transactional
	public void merge(Muffin muffin) {
		entityManager.merge( muffin );
	}

	@Transactional
	public void remove(Muffin muffin) {
		entityManager.remove( muffin );
	}
}
