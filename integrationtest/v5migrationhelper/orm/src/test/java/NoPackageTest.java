/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.jupiter.api.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * Test using an entity which is in no package.
 * Useful for demos?
 * We had an embarrassing NPE in this case, so better test for this.
 */
@TestForIssue(jiraKey = "HSEARCH-2319")
class NoPackageTest extends SearchTestBase {

	@Test
	void testMultipleEntitiesPerIndex() {
		try ( Session s = openSession() ) {
			s.getTransaction().begin();
			NotPackagedEntity box = new NotPackagedEntity();
			box.title = "This feels dirty";
			s.persist( box );
			s.getTransaction().commit();
		}

		try ( Session s = openSession() ) {
			s.getTransaction().begin();
			TermQuery q = new TermQuery( new Term( "title", "dirty" ) );
			List results = Search.getFullTextSession( s ).createFullTextQuery( q, NotPackagedEntity.class ).list();
			assertThat( results ).hasSize( 1 );
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				NotPackagedEntity.class
		};
	}
}
