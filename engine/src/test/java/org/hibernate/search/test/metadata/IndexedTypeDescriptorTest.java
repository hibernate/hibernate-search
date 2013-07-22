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
package org.hibernate.search.test.metadata;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.engine.impl.DefaultBoostStrategy;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.PropertyDescriptor;
import org.hibernate.search.test.util.ManualConfiguration;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-436")
public class IndexedTypeDescriptorTest {

	private AnnotationMetadataProvider metadataProvider;

	@Before
	public void setUp() {
		ConfigContext configContext = new ConfigContext( new ManualConfiguration() );
		metadataProvider = new AnnotationMetadataProvider( new JavaReflectionManager(), configContext );
	}

	@Test
	public void testIsIndexed() {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, Foo.class );
		assertEquals( "Wrong indexed type", Foo.class, typeDescriptor.getType() );
		assertTrue( typeDescriptor.isIndexed() );
	}

	@Test
	public void testDefaultStaticBoost() {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, Foo.class );
		assertEquals( "The default boost should be 1.0f", 1.0f, typeDescriptor.getStaticBoost(), 0f );
	}

	@Test
	public void testGetPropertyThrowsExceptionForNullParameter() {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, Foo.class );
		try {
			typeDescriptor.getProperty( null );
			fail( "Passing null as parameter is not allowed" );
		}
		catch (IllegalArgumentException e) {
			assertTrue( "Wrong exception: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000181" ) );
		}
	}

	@Test
	public void testGetFieldsForPropertyThrowsExceptionForNullParameter() {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, Foo.class );
		try {
			typeDescriptor.getFieldsForProperty( null );
			fail( "Passing null as parameter is not allowed" );
		}
		catch (IllegalArgumentException e) {
			assertTrue( "Wrong exception: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000181" ) );
		}
	}

	@Test
	public void testGetIndexedFieldThrowsExceptionForNullParameter() {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, Foo.class );
		try {
			typeDescriptor.getIndexedField( null );
			fail( "Passing null as parameter is not allowed" );
		}
		catch (IllegalArgumentException e) {
			assertTrue( "Wrong exception: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000182" ) );
		}
	}

	@Test
	public void testExplicitStaticBoost() {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, Fubar.class );
		assertEquals( "The default boost should be 42.0f", 42.0f, typeDescriptor.getStaticBoost(), 0f );
	}

	@Test
	public void testDefaultDynamicBoost() {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, Foo.class );
		assertTrue( typeDescriptor.getDynamicBoost() instanceof DefaultBoostStrategy );
	}

	@Test
	public void testExplicitDynamicBoost() {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, Fubar.class );
		assertTrue( typeDescriptor.getDynamicBoost() instanceof Fubar.DoublingBoost );
	}

	@Test
	public void testFieldAnnotationOnFieldAndGetterCreatesTwoFieldDescriptors() {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, Susfu.class );
		Set<FieldDescriptor> fieldDescriptors = typeDescriptor.getFieldsForProperty( "susfu" );
		assertEquals( "There should be two field descriptors", 2, fieldDescriptors.size() );
	}

	@Test
	public void testRetrievingPropertyDescriptors() {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, Snafu.class );

		Set<PropertyDescriptor> propertyDescriptors = typeDescriptor.getIndexedProperties();
		assertEquals( "There should be 5 properties defined in Snafu", 5, propertyDescriptors.size() );
		Set<String> expectedPropertyNames = new HashSet<String>();
		expectedPropertyNames.add( "id" );
		expectedPropertyNames.add( "snafu" );
		expectedPropertyNames.add( "numericField" );
		expectedPropertyNames.add( "nullValue" );
		expectedPropertyNames.add( "custom" );

		for ( PropertyDescriptor propertyDescriptor : propertyDescriptors ) {
			assertTrue(
					"Unexpected property name: " + propertyDescriptor.getName(),
					expectedPropertyNames.contains( propertyDescriptor.getName() )
			);
		}
	}
}
