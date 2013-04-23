package org.hibernate.search.test.integration.jtaspring;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MuffinDAO {
	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public void persist(Muffin muffin) {
		entityManager.persist(muffin);
	}

	@Transactional
	public void merge(Muffin muffin) {
		entityManager.merge(muffin);
	}

	@Transactional
	public void remove(Muffin muffin) {
		entityManager.remove(muffin);
	}
}
