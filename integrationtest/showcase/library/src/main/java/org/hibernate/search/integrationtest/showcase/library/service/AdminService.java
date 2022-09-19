/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.service;

import jakarta.persistence.EntityManager;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminService {

	@Autowired
	private EntityManager entityManager;

	public MassIndexer createMassIndexer() {
		return Search.session( entityManager ).massIndexer();
	}

	public void dropAndCreateSchema() {
		Search.session( entityManager ).schemaManager().dropAndCreate();
	}

	public void dropSchema() {
		Search.session( entityManager ).schemaManager().dropIfExisting();
	}
}
