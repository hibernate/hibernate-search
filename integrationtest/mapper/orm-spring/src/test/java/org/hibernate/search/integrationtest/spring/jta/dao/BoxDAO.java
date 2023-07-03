/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.jta.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.hibernate.search.integrationtest.spring.jta.entity.Box;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(timeout = 10) // Raise the timeout, because the default is very low in some tests
public class BoxDAO {
	@PersistenceContext
	private EntityManager entityManager;

	public void persist(Box box) {
		entityManager.persist( box );
	}

	public Box merge(Box box) {
		Box result = entityManager.merge( box );
		return result;
	}

	public void changeColor(long boxId, String newColor) {
		Box box = entityManager.find( Box.class, boxId );
		box.setColor( newColor );
	}

	public void remove(Box box) {
		entityManager.remove( box );
	}
}
