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

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.FieldSettingsDescriptor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor;
import org.hibernate.search.test.util.FooAnalyzer;
import org.hibernate.search.test.util.ManualConfiguration;
import org.hibernate.search.test.util.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-436")
public class FieldDescriptorTest {

	private AnnotationMetadataProvider metadataProvider;

	@Before
	public void setUp() {
		ConfigContext configContext = new ConfigContext( new ManualConfiguration() );
		metadataProvider = new AnnotationMetadataProvider( new JavaReflectionManager(), configContext );
	}

	@Test
	public void testFieldDescriptorLuceneOptions() {
		String fieldName = "my-snafu";
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, fieldName );

		assertEquals( "Wrong field name", fieldName, fieldDescriptor.getName() );
		assertEquals( Index.NO, fieldDescriptor.getIndex() );
		assertEquals( Analyze.NO, fieldDescriptor.getAnalyze() );
		assertEquals( Store.YES, fieldDescriptor.getStore() );
		assertEquals( Norms.NO, fieldDescriptor.getNorms() );
		assertEquals( TermVector.WITH_POSITIONS, fieldDescriptor.getTermVector() );
		assertEquals( 10.0f, fieldDescriptor.getBoost(), 0 );

		assertFalse( fieldDescriptor.indexNull() );
		assertNull( fieldDescriptor.indexNullAs() );

		assertTrue( FieldSettingsDescriptor.Type.BASIC.equals( fieldDescriptor.getType() ) );
	}

	@Test
	public void testFieldDescriptorDefaultNullIndexOptions() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "my-snafu" );

		assertFalse( fieldDescriptor.indexNull() );
		assertNull( fieldDescriptor.indexNullAs() );
	}

	@Test
	public void testFieldDescriptorNullIndexOptions() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "nullValue" );

		assertTrue( fieldDescriptor.indexNull() );
		assertEquals( "snafu", fieldDescriptor.indexNullAs() );
	}

	@Test
	public void testCastingNonNumericFieldDescriptorToNumericOneThrowsException() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "my-snafu" );

		assertFalse( FieldSettingsDescriptor.Type.NUMERIC.equals( fieldDescriptor.getType() ) );
		try {
			fieldDescriptor.as( NumericFieldSettingsDescriptor.class );
			fail( "A basic field descriptor cannot be narrowed to a numeric one" );
		}
		catch (ClassCastException e) {
			assertTrue( "Wrong exception: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000180" ) );
		}
	}

	@Test
	public void testFieldDescriptorAsWithNullParameterThrowsException() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "my-snafu" );
		try {
			fieldDescriptor.as( null );
			fail( "null is not a valid type" );
		}
		catch (ClassCastException e) {
			assertTrue( "Wrong exception: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000180" ) );
		}
	}

	@Test
	public void testFieldDescriptorExplicitNumericOptions() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "numericField" );

		assertTrue( FieldSettingsDescriptor.Type.NUMERIC.equals( fieldDescriptor.getType() ) );

		NumericFieldSettingsDescriptor numericFieldSettingsDescriptor = fieldDescriptor.as(
				NumericFieldSettingsDescriptor.class
		);
		int expectedPrecisionStep = 16;
		assertEquals(
				"the numeric step should be " + expectedPrecisionStep,
				expectedPrecisionStep,
				numericFieldSettingsDescriptor.precisionStep()
		);
	}

	@Test
	public void testFieldDescriptorDefaultAnalyzer() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "my-snafu" );

		assertNotNull( fieldDescriptor.getAnalyzer() );
		assertTrue( fieldDescriptor.getAnalyzer() instanceof StandardAnalyzer );
	}

	@Test
	public void testFieldDescriptorExplicitAnalyzer() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "custom" );

		assertNotNull( fieldDescriptor.getAnalyzer() );
		assertTrue( fieldDescriptor.getAnalyzer() instanceof FooAnalyzer );
	}

	@Test
	public void testFieldDescriptorDefaultFieldBridge() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "my-snafu" );

		assertNotNull( fieldDescriptor.getFieldBridge() );
		assertTrue( fieldDescriptor.getFieldBridge() instanceof StringBridge );
	}

	private FieldDescriptor getFieldDescriptor(Class<?> clazz, String fieldName) {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, clazz );
		return typeDescriptor.getIndexedField( fieldName );
	}
}


