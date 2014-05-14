/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jtaspring;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SnertDAO {

	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public void persist(Snert snert) {
		entityManager.persist( snert );
	}

	@Transactional
	public void merge(Snert snert) {
		entityManager.merge( snert );
	}

	@Transactional
	public void remove(Snert snert) {
		entityManager.remove( snert );
	}
}
