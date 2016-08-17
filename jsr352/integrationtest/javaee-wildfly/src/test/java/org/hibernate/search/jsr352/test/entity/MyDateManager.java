/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.test.entity;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;

/**
 *
 * @author Mincong Huang
 */
@Stateless
public class MyDateManager {

	@PersistenceContext(name = "h2")
	private EntityManager em;

	public void persist(MyDate myDate) {
		em.persist( myDate );
	}

	public List<MyDate> findDateByWeekday(String weekday) {
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		Query luceneQuery = ftem.getSearchFactory().buildQueryBuilder()
				.forEntity( MyDate.class ).get()
				.keyword().onField( "weekday" ).matching( weekday )
				.createQuery();
		@SuppressWarnings("unchecked")
		List<MyDate> result = ftem.createFullTextQuery( luceneQuery ).getResultList();
		return result;
	}

	public EntityManager getEntityManager() {
		return em;
	}
}
