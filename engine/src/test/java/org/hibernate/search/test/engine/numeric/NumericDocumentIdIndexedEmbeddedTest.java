/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.numeric;

import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SearchITHelper.AssertBuildingHSQueryContext;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;

public class NumericDocumentIdIndexedEmbeddedTest {

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	private SearchIntegrator integrator;

	private final SearchITHelper helper = new SearchITHelper( () -> this.integrator );

	@TestForIssue(jiraKey = "HSEARCH-2545")
	@Test
	public void testIndexAndSearchNumericField() {
		integrator = integratorResource.create( new SearchConfigurationForTest().addClasses( A.class, B.class, C.class ) );
		/*
		 * We mainly want to test that the search factory will initialize without error,
		 * but also checking that the field is actually numeric won't hurt.
		 */
		A a = new A();
		a.id = 0L;
		B b = new B();
		b.id = 1L;
		b.a = a;
		C c = new C();
		c.id = 2L;
		c.b = b;
		helper.add( a, c );

		// Range Queries including lower and upper bounds
		assertRangeQuery( C.class, "b.a.id", a.id, a.id ).as( "Query id " ).hasResultSize( 1 );
	}

	private AssertBuildingHSQueryContext assertRangeQuery(Class<?> entityClass, String fieldName, Object from, Object to) {
		Query query = NumericFieldUtils.createNumericRangeQuery( fieldName, from, to, true, true );
		return helper.assertThat( query ).from( entityClass );
	}

	@Indexed
	private static class A {
		@DocumentId
		@NumericField
		@SortableField
		private Long id;
	}

	private static class B {
		@DocumentId
		private Long id;

		@IndexedEmbedded(includePaths = "id")
		private A a;
	}

	@Indexed
	private static class C {
		@DocumentId
		private Long id;

		@IndexedEmbedded(includePaths = "a.id")
		private B b;
	}

}
