/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.massindexing;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.search.Query;
import org.hibernate.CacheMode;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.jboss.ejb3.annotation.TransactionTimeout;

/**
 * @author Gunnar Morling
 */
@Stateless
public class ConcertManager {

	@PersistenceContext
	private EntityManager entityManager;

	@TransactionTimeout(value = 5, unit = TimeUnit.MINUTES)
	public void saveConcerts(Iterable<Concert> concerts) {
		int i = 0;
		for ( Concert concert : concerts ) {
			entityManager.persist( concert );

			if ( i % 50 == 0 ) {
				entityManager.flush();
				entityManager.clear();
			}

			i++;
		}
	}

	public List<Concert> findConcertsByArtist(String artist) {
		FullTextEntityManager fem = Search.getFullTextEntityManager( entityManager );

		Query luceneQuery = fem.getSearchFactory().buildQueryBuilder()
				.forEntity( Concert.class ).get()
					.keyword().onField( "artist" ).matching( artist )
				.createQuery();

		@SuppressWarnings("unchecked")
		List<Concert> result = fem.createFullTextQuery( luceneQuery ).getResultList();

		return result;
	}

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void indexConcerts() {
		try {
			Search.getFullTextEntityManager( entityManager )
				.createIndexer()
				.batchSizeToLoadObjects( 1 )
				.threadsToLoadObjects( 1 )
				.transactionTimeout( 10 )
				.cacheMode( CacheMode.IGNORE )
				.startAndWait();
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
	}
}
