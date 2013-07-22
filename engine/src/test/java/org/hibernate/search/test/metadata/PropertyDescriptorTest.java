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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.IndexDescriptor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.PropertyDescriptor;
import org.hibernate.search.metadata.impl.IndexedTypeDescriptorImpl;
import org.hibernate.search.test.util.ManualConfiguration;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-436")
public class PropertyDescriptorTest {

	private static final List<String> TEST_INDEX_NAMES = Arrays.asList(
			"index-0", "index-0", "index-0" );
	private AnnotationMetadataProvider metadataProvider;

	@Before
	public void setUp() {
		ConfigContext configContext = new ConfigContext( new ManualConfiguration() );
		metadataProvider = new AnnotationMetadataProvider( new JavaReflectionManager(), configContext );
	}

	@Test
	public void testIdProperty() {
		PropertyDescriptor propertyDescriptor = getPropertyDescriptor( Fubar.class, "id" );
		String propertyName = propertyDescriptor.getName();
		assertEquals( "Wrong property name", "id", propertyName );
		assertTrue(
				"This property should should host the id field",
				propertyDescriptor.isId()
		);
	}

	@Test
	public void testIndexInformation() {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, Foo.class );

		Set<IndexDescriptor> indexDescriptors = typeDescriptor.getIndexDescriptors();
		assertEquals( "Wrong index name", TEST_INDEX_NAMES.get( 0 ), indexDescriptors.iterator().next().getName() );
	}

	@Test
	public void testSharedIndexInformation() {
		TypeMetadata typeMetadata = metadataProvider.getTypeMetadataFor( Foo.class );
		IndexedTypeDescriptor typeDescriptor = new IndexedTypeDescriptorImpl(
				typeMetadata,
				DescriptorTestHelper.getDummyShardedIndexManager()
		);
		assertTrue( typeDescriptor.isSharded() );

		Set<IndexDescriptor> indexDescriptors = typeDescriptor.getIndexDescriptors();
		assertTrue( indexDescriptors.size() == 3 );
		for ( IndexDescriptor indexDescriptor : indexDescriptors ) {
			String shardName = indexDescriptor.getName();
			assertTrue( "Missing shard name: " + shardName, TEST_INDEX_NAMES.contains( indexDescriptor.getName() ) );
		}
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

	private PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String propertyName) {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, clazz );
		return typeDescriptor.getProperty( propertyName );
	}
}
