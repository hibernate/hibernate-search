/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.jta.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.hibernate.search.integrationtest.spring.jta.entity.Snert;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(timeout = 10) // Raise the timeout, because the default is very low in some tests
public class SnertDAO {

	@PersistenceContext
	private EntityManager entityManager;

	public void persist(Snert snert) {
		entityManager.persist( snert );
	}

	public void merge(Snert snert) {
		entityManager.merge( snert );
	}

	public void remove(Snert snert) {
		entityManager.remove( snert );
	}
}
