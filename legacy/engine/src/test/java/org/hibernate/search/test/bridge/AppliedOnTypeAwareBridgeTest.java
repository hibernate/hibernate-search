/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.bridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.lucene.document.Document;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.bridge.AppliedOnTypeAwareBridge;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Hardy Ferentschik
 */
public class AppliedOnTypeAwareBridgeTest {

	@Rule
	public final SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	private SearchIntegrator searchFactory;

	private final SearchITHelper helper = new SearchITHelper( () -> this.searchFactory );

	@Test
	public void testTypeIsSetForField() {
		searchFactory = createSearchIntegrator( Foo.class );

		Foo foo = new Foo( 0l );

		helper.add( foo );
	}

	@Test
	public void testTypeIsSetForGetter() {
		searchFactory = createSearchIntegrator( Bar.class );

		Bar bar = new Bar( 0l );

		helper.add( bar );
	}

	@Test
	public void testTypeIsSetForClass() {
		searchFactory = createSearchIntegrator( Snafu.class );

		Snafu snafu = new Snafu( 0l );

		helper.add( snafu );
	}

	private SearchIntegrator createSearchIntegrator(Class<?> clazz) {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest()
				.addClass( clazz );

		return integratorResource.create( configuration );
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
	}

	@Indexed
	public static class Bar {
		@DocumentId
		private Long id;

		private Object test;

		public Bar(Long id) {
			this.id = id;
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


