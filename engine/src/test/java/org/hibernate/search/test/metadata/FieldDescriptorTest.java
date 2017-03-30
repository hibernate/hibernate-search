/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.metadata;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.FieldSettingsDescriptor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;
import org.hibernate.search.spatial.SpatialFieldBridge;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.analyzer.FooAnalyzer;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.util.impl.PassThroughAnalyzer;
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
		SearchConfiguration searchConfiguration = new SearchConfigurationForTest();
		ConfigContext configContext = new ConfigContext(
				searchConfiguration,
				new BuildContextForTest( searchConfiguration )
		);
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

		NumericEncodingType expectedNumericEncodingType = NumericEncodingType.INTEGER;
		assertEquals(
				"the numeric field should be encoded as " + expectedNumericEncodingType,
				expectedNumericEncodingType,
				numericFieldSettingsDescriptor.encodingType()
		);
	}

	@Test
	public void testFieldDescriptorWithCoordinates() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( SnafuWithCoordinates.class, "location" );

		assertNotNull( fieldDescriptor.getFieldBridge() );
		assertTrue( fieldDescriptor.getFieldBridge() instanceof SpatialFieldBridge );

		assertTrue( FieldSettingsDescriptor.Type.SPATIAL.equals( fieldDescriptor.getType() ) );
	}

	@Test
	public void testFieldDescriptorWithCoordinatesWithoutSpatialAnnotation() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( SnafuWithCoordinatesWithoutSpatial.class, "location" );

		assertNotNull( fieldDescriptor.getFieldBridge() );
		assertTrue( fieldDescriptor.getFieldBridge() instanceof SpatialFieldBridge );

		assertEquals( FieldSettingsDescriptor.Type.SPATIAL, fieldDescriptor.getType() );
	}

	@Test
	public void testFieldDescriptorSpatialFieldBridge() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "location" );

		assertNotNull( fieldDescriptor.getFieldBridge() );
		assertTrue( fieldDescriptor.getFieldBridge() instanceof SpatialFieldBridge );

		assertTrue( FieldSettingsDescriptor.Type.SPATIAL.equals( fieldDescriptor.getType() ) );
	}

	@Test
	public void testFieldDescriptorDefaultAnalyzer() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "my-snafu" );

		assertNotNull( fieldDescriptor.getAnalyzer() );
		assertTrue( fieldDescriptor.getAnalyzer() instanceof PassThroughAnalyzer );
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

	@Test
	public void testFieldDescriptorShortNumericFieldBridge() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "numericShortField" );

		assertNotNull( fieldDescriptor.getFieldBridge() );
		assertTrue( fieldDescriptor.getFieldBridge() instanceof NumericFieldBridge );

		assertTrue( FieldSettingsDescriptor.Type.NUMERIC.equals( fieldDescriptor.getType() ) );

		NumericFieldSettingsDescriptor numericFieldSettingsDescriptor = fieldDescriptor.as(
				NumericFieldSettingsDescriptor.class
		);
		int expectedPrecisionStep = 8;
		assertEquals(
				"the numeric step should be " + expectedPrecisionStep,
				expectedPrecisionStep,
				numericFieldSettingsDescriptor.precisionStep()
		);

		NumericEncodingType expectedNumericEncodingType = NumericEncodingType.INTEGER;
		assertEquals(
				"the short numeric field should be encoded as " + expectedNumericEncodingType,
				expectedNumericEncodingType,
				numericFieldSettingsDescriptor.encodingType()
		);
	}

	@Test
	public void testFieldDescriptorByteNumericFieldBridge() {
		FieldDescriptor fieldDescriptor = getFieldDescriptor( Snafu.class, "numericByteField" );

		assertNotNull( fieldDescriptor.getFieldBridge() );
		assertTrue( fieldDescriptor.getFieldBridge() instanceof NumericFieldBridge );

		assertTrue( FieldSettingsDescriptor.Type.NUMERIC.equals( fieldDescriptor.getType() ) );

		NumericFieldSettingsDescriptor numericFieldSettingsDescriptor = fieldDescriptor.as(
				NumericFieldSettingsDescriptor.class
		);
		int expectedPrecisionStep = 4;
		assertEquals(
				"the numeric step should be " + expectedPrecisionStep,
				expectedPrecisionStep,
				numericFieldSettingsDescriptor.precisionStep()
		);

		NumericEncodingType expectedNumericEncodingType = NumericEncodingType.INTEGER;
		assertEquals(
				"the short numeric field should be encoded as " + expectedNumericEncodingType,
				expectedNumericEncodingType,
				numericFieldSettingsDescriptor.encodingType()
		);
	}

	private FieldDescriptor getFieldDescriptor(Class<?> clazz, String fieldName) {
		IndexedTypeDescriptor typeDescriptor = DescriptorTestHelper.getTypeDescriptor( metadataProvider, clazz );
		return typeDescriptor.getIndexedField( fieldName );
	}
}
