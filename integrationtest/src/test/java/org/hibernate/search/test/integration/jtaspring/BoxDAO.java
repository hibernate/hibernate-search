package org.hibernate.search.test.integration.jtaspring;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class BoxDAO {
	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public void persist(Box box) {
		entityManager.persist(box);
//		entityManager.flush();
	}

	@Transactional
	public Box merge(Box box) {
		Box result = entityManager.merge(box);
		
//		entityManager.flush();
		
		return result;
	}

	@Transactional
	public void remove(Box box) {
		entityManager.remove(box);
//		entityManager.flush();
	}
	
	public EntityManager getEntityManager() {
		return entityManager;
	}
}
