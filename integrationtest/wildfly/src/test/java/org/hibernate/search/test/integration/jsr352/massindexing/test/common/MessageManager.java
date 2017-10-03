/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jsr352.massindexing.test.common;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.jboss.ejb3.annotation.TransactionTimeout;

/**
 * @author Mincong Huang
 */
@Stateless
public class MessageManager {

	public static final String PERSISTENCE_UNIT_NAME = "primary_pu";

	@PersistenceContext(unitName = PERSISTENCE_UNIT_NAME)
	private EntityManager em;

	@TransactionTimeout(value = 5, unit = TimeUnit.MINUTES)
	public void persist(Iterable<Message> messages) {
		int i = 0;
		for ( Message m : messages ) {
			em.persist( m );
			if ( i % 50 == 0 ) {
				em.flush();
				em.clear();
			}
			i++;
		}
	}

	public void removeAll() {
		em.createQuery( "delete from Message" ).executeUpdate();
	}

	@SuppressWarnings("unchecked")
	public List<Message> findMessagesFor(Date date) {
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		Date dateDay = DateTools.round( date, DateTools.Resolution.DAY );
		NumericRangeQuery<Long> numericRangeQuery = NumericRangeQuery.newLongRange(
				"date", dateDay.getTime(), dateDay.getTime(), true, true );
		BooleanQuery booleanQuery = ( new BooleanQuery.Builder() )
				.add( numericRangeQuery, BooleanClause.Occur.MUST )
				.build();
		return ftem.createFullTextQuery( booleanQuery, Message.class )
				.getResultList();
	}

}
