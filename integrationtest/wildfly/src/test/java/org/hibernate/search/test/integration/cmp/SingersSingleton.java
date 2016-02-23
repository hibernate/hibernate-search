/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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


