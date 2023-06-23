/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

/**
 * Test using an entity which is in no package.
 * Useful for demos?
 * We had an embarrassing NPE in this case, so better test for this.
 */
@TestForIssue(jiraKey = "HSEARCH-2319")
public class NoPackageTest extends SearchTestBase {

	@Test
	public void testMultipleEntitiesPerIndex() throws Exception {
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
			assertEquals( 1, results.size() );
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				NotPackagedEntity.class
		};
	}
}
