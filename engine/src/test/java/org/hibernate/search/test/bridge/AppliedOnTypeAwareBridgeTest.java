/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.util.ManualConfiguration;
import org.hibernate.search.test.util.ManualTransactionContext;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class AppliedOnTypeAwareBridgeTest {

	@Test
	public void testTypeIsSetForField() {
		SearchFactoryImplementor searchFactory = createSearchFactory( Foo.class );

		Foo foo = new Foo( 0l );

		Work<Foo> work = new Work<Foo>( foo, foo.getId(), WorkType.ADD, false );
		ManualTransactionContext tc = new ManualTransactionContext();
		searchFactory.getWorker().performWork( work, tc );
		tc.end();
	}

	@Test
	public void testTypeIsSetForGetter() {
		SearchFactoryImplementor searchFactory = createSearchFactory( Bar.class );

		Bar bar = new Bar( 0l );

		Work<Bar> work = new Work<Bar>( bar, bar.getId(), WorkType.ADD, false );
		ManualTransactionContext tc = new ManualTransactionContext();
		searchFactory.getWorker().performWork( work, tc );
		tc.end();
	}

	@Test
	public void testTypeIsSetForClass() {
		SearchFactoryImplementor searchFactory = createSearchFactory( Snafu.class );

		Snafu snafu = new Snafu( 0l );

		Work<Snafu> work = new Work<Snafu>( snafu, snafu.getId(), WorkType.ADD, false );
		ManualTransactionContext tc = new ManualTransactionContext();
		searchFactory.getWorker().performWork( work, tc );
		tc.end();
	}


	private SearchFactoryImplementor createSearchFactory(Class<?> clazz) {
		ManualConfiguration configuration = new ManualConfiguration()
				.addProperty( "hibernate.search.default.directory_provider", "ram" )
				.addClass( clazz );

		return new SearchFactoryBuilder().configuration( configuration ).buildSearchFactory();
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
			params = @Parameter(name = "type", value = "org.hibernate.search.test.bridge.AppliedOnTypeAwareBridgeTest$Snafu"))
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
		private Class<?> expectedType;
		private Class<?> actualType;

		@Override
		public void setAppliedOnType(Class<?> returnType) {
			actualType = returnType;
		}

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			assertTrue( "Type not set prior to calling #set of field bridge", actualType != null );
			assertEquals( "Unexpected type", expectedType, actualType );
		}

		@Override
		public void setParameterValues(Map<String, String> parameters) {
			String expectedTypeName = parameters.get( "type" );
			expectedType = ClassLoaderHelper.classForName(
					expectedTypeName,
					this.getClass().getClassLoader(),
					"Unable to load type"
			);
		}
	}
}


