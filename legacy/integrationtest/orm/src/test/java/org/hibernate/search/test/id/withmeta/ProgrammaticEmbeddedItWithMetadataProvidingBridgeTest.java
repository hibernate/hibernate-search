/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id.withmeta;

import java.lang.annotation.ElementType;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Davide D'Alto
 */
public class ProgrammaticEmbeddedItWithMetadataProvidingBridgeTest extends SearchTestBase {

	@Test
	@SuppressWarnings("unchecked")
	public void testFieldBridge() throws Exception {
		PersonPK emmanuelPk = new PersonPK();
		emmanuelPk.setFirstName( "Emmanuel" );
		emmanuelPk.setLastName( "Bernard" );
		PlainPerson emmanuel = new PlainPerson();
		emmanuel.setFavoriteColor( "Blue" );
		emmanuel.setId( emmanuelPk );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.save( emmanuel );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		List<PlainPerson> results = Search.getFullTextSession( s ).createFullTextQuery(
				new TermQuery( new Term( "id_lastName", "Bernard" ) )
		).list();
		assertEquals( 1, results.size() );
		emmanuel = (PlainPerson) results.get( 0 );
		emmanuel.setFavoriteColor( "Red" );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		results = Search.getFullTextSession( s ).createFullTextQuery(
				new TermQuery( new Term( "id_lastName", "Bernard" ) )
		).list();
		assertEquals( 1, results.size() );
		emmanuel = (PlainPerson) results.get( 0 );
		assertEquals( "Red", emmanuel.getFavoriteColor() );
		s.delete( results.get( 0 ) );
		tx.commit();
		s.close();
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		SearchMapping mapping = new SearchMapping();
		mapping
			.entity( PlainPerson.class )
				.indexed()
			.property( "id", ElementType.FIELD )
				.documentId()
				.bridge( PersonPKMetadataProviderBridge.class )
			.property( "", ElementType.FIELD )
				.field();
		cfg.put( Environment.MODEL_MAPPING, mapping );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[]{ PlainPerson.class };
	}
}
