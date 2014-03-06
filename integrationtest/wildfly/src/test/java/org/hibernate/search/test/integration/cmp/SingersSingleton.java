/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.integration.cmp;

import java.util.List;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.CacheMode;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;

/**
 * A singleton session bean.
 *
 * @author Hardy Ferentschik
 */
@Singleton
public class SingersSingleton {
	@PersistenceContext(unitName = "cmt-test")
	private EntityManager entityManager;

	public void insertContact(String firstName, String lastName) {
		Singer singer = new Singer();
		singer.setFirstName( firstName );
		singer.setLastName( lastName );
		entityManager.persist( singer );
	}

	public boolean rebuildIndex() throws InterruptedException {
		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager( entityManager );
		try {
			fullTextEntityManager
					.createIndexer()
					.batchSizeToLoadObjects( 30 )
					.threadsToLoadObjects( 4 )
					.cacheMode( CacheMode.NORMAL )
					.startAndWait();
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	public List<?> listAllContacts() {
		Query query = entityManager.createQuery( "select s from Singer s" );
		return query.getResultList();
	}

	public List<?> searchAllContacts() {
		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager( entityManager );

		FullTextQuery query = fullTextEntityManager.createFullTextQuery(
				new MatchAllDocsQuery(),
				Singer.class
		);

		return query.getResultList();
	}
}


