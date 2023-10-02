/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.fieldAccess;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;

import org.junit.jupiter.api.Test;

import org.apache.lucene.queryparser.classic.QueryParser;

/**
 * @author Emmanuel Bernard
 */
class FieldAccessTest extends SearchTestBase {

	@Test
	void testFields() throws Exception {
		Document doc = new Document( "Hibernate in Action", "Object/relational mapping with Hibernate",
				"blah blah blah" );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( doc );
		tx.commit();

		s.clear();

		FullTextSession session = Search.getFullTextSession( s );
		tx = session.beginTransaction();
		QueryParser p = new QueryParser( "noDefaultField", TestConstants.standardAnalyzer );
		List result = session.createFullTextQuery( p.parse( "Abstract:Hibernate" ) ).list();
		assertThat( result ).as( "Query by field" ).hasSize( 1 );
		s.delete( result.get( 0 ) );
		tx.commit();
		s.close();

	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Document.class };
	}
}
