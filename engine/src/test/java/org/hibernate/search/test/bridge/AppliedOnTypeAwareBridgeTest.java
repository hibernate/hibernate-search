/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.bridge;

import java.util.Map;

import org.apache.lucene.document.Document;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.bridge.AppliedOnTypeAwareBridge;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class AppliedOnTypeAwareBridgeTest {

	@Test
	public void testTypeIsSetForField() {
		SearchIntegrator searchFactory = createSearchIntegrator( Foo.class );

		Foo foo = new Foo( 0l );

		Work work = new Work( foo, foo.getId(), WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		searchFactory.getWorker().performWork( work, tc );
		tc.end();
	}

	@Test
	public void testTypeIsSetForGetter() {
		SearchIntegrator searchFactory = createSearchIntegrator( Bar.class );

		Bar bar = new Bar( 0l );

		Work work = new Work( bar, bar.getId(), WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		searchFactory.getWorker().performWork( work, tc );
		tc.end();
	}

	@Test
	public void testTypeIsSetForClass() {
		SearchIntegrator searchFactory = createSearchIntegrator( Snafu.class );

		Snafu snafu = new Snafu( 0l );

		Work work = new Work( snafu, snafu.getId(), WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		searchFactory.getWorker().performWork( work, tc );
		tc.end();
	}

	private SearchIntegrator createSearchIntegrator(Class<?> clazz) {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addClass( clazz );

		return new SearchIntegratorBuilder().configuration( configuration ).buildSearchIntegrator();
	}

	@Indexed
	public static class Foo {
		@DocumentId
		private Long id;

		@Field(bridge = @org.hibernate.search.annotations.FieldBridge(impl = TypeAssertingFieldBridge.class,
				params = @Parameter(name = "type", value = "java.lang.Integer")))
		private Integer test;

		public Foo(Long id) {
			this.id = id;
		}

		public long getId() {
			return id;
		}
	}

	@Indexed
	public static class Bar {
		@DocumentId
		private Long id;

		private Object test;

		public Bar(Long id) {
			this.id = id;
		}

		public long getId() {
			return id;
		}

		@Field(bridge = @org.hibernate.search.annotations.FieldBridge(impl = TypeAssertingFieldBridge.class,
				params = @Parameter(name = "type", value = "java.lang.Object")))
		public Object getTest() {
			return test;
		}
	}

	@Indexed
	@ClassBridge(impl = AppliedOnTypeAwareBridgeTest.TypeAssertingFieldBridge.class,
			params = @Parameter(name = "type",
					value = "org.hibernate.search.test.bridge.AppliedOnTypeAwareBridgeTest$Snafu"))
	public static class Snafu {
		@DocumentId
		private Long id;

		public Snafu(Long id) {
			this.id = id;
		}

		public long getId() {
			return id;
		}
	}

	public static class TypeAssertingFieldBridge implements FieldBridge, AppliedOnTypeAwareBridge, ParameterizedBridge {
		private String expectedTypeName;
		private String actualTypeName;

		@Override
		public void setAppliedOnType(Class<?> returnType) {
			actualTypeName = returnType.getName();
		}

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			assertTrue( "Type not set prior to calling #set of field bridge", actualTypeName != null );
			assertEquals( "Unexpected type", expectedTypeName, actualTypeName );
		}

		@Override
		public void setParameterValues(Map<String, String> parameters) {
			expectedTypeName = parameters.get( "type" );
		}
	}
}


