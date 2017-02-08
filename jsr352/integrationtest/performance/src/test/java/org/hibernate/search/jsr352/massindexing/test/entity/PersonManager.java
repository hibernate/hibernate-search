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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.jboss.ejb3.annotation.TransactionTimeout;

/**
 * @author Mincong Huang
 * @author Gunnar Morling
 */
@Stateless
public class PersonManager {

	@PersistenceContext(unitName = "h2")
	private EntityManager em;

	@TransactionTimeout(value = 5, unit = TimeUnit.MINUTES)
	public void persist(Iterable<Person> people) {
		int i = 0;
		for ( Person person : people ) {
			em.persist( person );
			if ( i % 50 == 0 ) {
				em.flush();
				em.clear();
			}
			i++;
		}
	}

	public List<Person> findPerson(String firstName) {
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		Query luceneQuery = ftem.getSearchFactory().buildQueryBuilder()
				.forEntity( Person.class ).get()
				.keyword().onField( "firstName" ).matching( firstName )
				.createQuery();
		@SuppressWarnings("unchecked")
		List<Person> result = ftem.createFullTextQuery( luceneQuery ).getResultList();
		return result;
	}

	public long rowCount() {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = builder.createQuery( Long.class );
		cq.select( builder.count( cq.from( Person.class ) ) );
		return em.createQuery( cq ).getSingleResult();
	}

	public EntityManager getEntityManager() {
		return em;
	}
}
