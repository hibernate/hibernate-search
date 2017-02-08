/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.test.entity;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.jboss.ejb3.annotation.TransactionTimeout;

/**
 * @author Mincong Huang
 * @author Gunnar Morling
 */
@Stateless
public class CompanyManager {

	@PersistenceContext(unitName = "h2")
	private EntityManager em;

	@TransactionTimeout(value = 5, unit = TimeUnit.MINUTES)
	public void persist(Iterable<Company> companies) {
		int i = 0;
		for ( Company company : companies ) {
			em.persist( company );
			if ( i % 50 == 0 ) {
				em.flush();
				em.clear();
			}
			i++;
		}
	}

	public List<Company> findCompanyByName(String name) {
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		Query luceneQuery = ftem.getSearchFactory().buildQueryBuilder()
				.forEntity( Company.class ).get()
				.keyword().onField( "name" ).matching( name )
				.createQuery();
		@SuppressWarnings("unchecked")
		List<Company> result = ftem.createFullTextQuery( luceneQuery ).getResultList();
		return result;
	}

	public List<Company> findAll() {
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		Query luceneQuery = ftem.getSearchFactory().buildQueryBuilder()
				.forEntity( Company.class ).get()
				.all()
				.createQuery();
		@SuppressWarnings("unchecked")
		List<Company> results = ftem.createFullTextQuery( luceneQuery ).getResultList();
		return results;
	}

	public EntityManager getEntityManager() {
		return em;
	}
}
