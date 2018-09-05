/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.metadata;

import java.net.URI;
import java.util.Date;
import java.util.Set;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.FacetEncodingType;
import org.hibernate.search.annotations.Facets;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.impl.ConfigContext;
import org.hibernate.search.engine.metadata.impl.AnnotationMetadataProvider;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.FacetMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.LuceneEmbeddedIndexManagerType;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-809")
public class DocumentFieldMetadataTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

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
	public void testStringFieldCanBeCOnfiguredForFaceting() {
		FacetMetadata facetMetadata = getSingleFacetMetadata( Foo.class, "name" );
		assertEquals( "Unexpected facet name", "name", facetMetadata.getAbsoluteName() );
		assertEquals( "Unexpected encoding type", FacetEncodingType.STRING, facetMetadata.getEncoding() );
	}

	@Test
	public void testDateFieldCanBeConfiguredForFaceting() {
		FacetMetadata facetMetadata = getSingleFacetMetadata( Foobar.class, "date" );
		assertEquals( "Unexpected facet name", "date", facetMetadata.getAbsoluteName() );
		assertEquals( "Unexpected encoding type", FacetEncodingType.LONG, facetMetadata.getEncoding() );
	}

	@Test
	public void testExplicitFacetName() {
		FacetMetadata facetMetadata = getSingleFacetMetadata( Fubar.class, "name" );
		assertEquals( "Unexpected facet name", "facet_name", facetMetadata.getAbsoluteName() );
		assertEquals( "Unexpected encoding type", FacetEncodingType.STRING, facetMetadata.getEncoding() );
	}

	@Test
	public void testFacetFieldTargetsSpecificFieldAnnotation() {
		FacetMetadata facetMetadata = getSingleFacetMetadata( Baz.class, "facet_value" );
		assertEquals( "Unexpected facet name", "facet_value", facetMetadata.getAbsoluteName() );
		assertEquals( "Unexpected encoding type", FacetEncodingType.DOUBLE, facetMetadata.getEncoding() );
	}

	@Test
	public void testMultipleFacetsAnnotation() {
		FacetMetadata facetMetadata = getSingleFacetMetadata( Qux.class, "value" );
		assertEquals( "Unexpected facet name", "value", facetMetadata.getAbsoluteName() );
		assertEquals( "Unexpected encoding type", FacetEncodingType.DOUBLE, facetMetadata.getEncoding() );

		facetMetadata = getSingleFacetMetadata( Qux.class, "facet_value" );
		assertEquals( "Unexpected facet name", "facet_value", facetMetadata.getAbsoluteName() );
		assertEquals( "Unexpected encoding type", FacetEncodingType.STRING, facetMetadata.getEncoding() );
	}

	@Test
	public void testAddingFacetToUnsupportedTypeThrowsException() {
		try {
			metadataProvider.getTypeMetadataFor( Bar.class, LuceneEmbeddedIndexManagerType.INSTANCE );
			fail( "Invalid facet configuration should throw exception. URI type cannot be faceted" );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000264" ) );
		}
	}

	@Test
	public void testAddingFacetToUnanalyzedFieldThrowsException() {
		try {
			metadataProvider.getTypeMetadataFor( Snafu.class, LuceneEmbeddedIndexManagerType.INSTANCE );
			fail( "Field targeted for faceting cannot be analyzed" );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message: " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000273" ) );
		}
	}

	@Test
	public void testNumericFieldReferencingNonExistingFieldThrowsException() {
		expectedException.expect( SearchException.class );
		expectedException.expectMessage( "HSEARCH000262" );

		metadataProvider.getTypeMetadataFor( TypeWithNumericFieldReferringToNonExistantField.class,
				LuceneEmbeddedIndexManagerType.INSTANCE );
	}

	@Test
	public void testNumericFieldWithoutFieldThrowsException() {
		expectedException.expect( SearchException.class );
		expectedException.expectMessage( "HSEARCH000262" );

		metadataProvider.getTypeMetadataFor( TypeWithNumericFieldWithoutField.class, LuceneEmbeddedIndexManagerType.INSTANCE );
	}

	@Test
	public void testSeveralNumericFieldsReferringToSameFieldThrowException() {
		expectedException.expect( SearchException.class );
		expectedException.expectMessage( "HSEARCH000300" );

		metadataProvider.getTypeMetadataFor( TypeWithSeveralNumericFieldsReferringToSameField.class,
				LuceneEmbeddedIndexManagerType.INSTANCE );
	}

	private FacetMetadata getSingleFacetMetadata(Class<?> type, String fieldName) {
		TypeMetadata typeMetadata = metadataProvider.getTypeMetadataFor( type, LuceneEmbeddedIndexManagerType.INSTANCE );
		DocumentFieldMetadata documentFieldMetadata = typeMetadata.getDocumentFieldMetadataFor( fieldName );

		assertTrue( "The field should be enabled for faceting", documentFieldMetadata.hasFacets() );

		Set<FacetMetadata> facetMetadataSet = documentFieldMetadata.getFacetMetadata();
		assertEquals( "Unexpected number of metadata instances", 1, facetMetadataSet.size() );

		return facetMetadataSet.iterator().next();
	}

	@Indexed
	public class Foo {
		@DocumentId
		private Integer id;

		@Field(analyze = Analyze.NO)
		@Facet
		private String name;
	}

	@Indexed
	public class Bar {
		@DocumentId
		private Integer id;

		@Field(analyze = Analyze.NO)
		@Facet
		private URI uri;
	}

	@Indexed
	public class Foobar {
		@DocumentId
		private Integer id;

		@Field(analyze = Analyze.NO)
		@Facet
		private Date date;
	}

	@Indexed
	public class Fubar {
		@DocumentId
		private Integer id;

		@Field(analyze = Analyze.NO)
		@Facet(name = "facet_name")
		private String name;
	}

	@Indexed
	public class Baz {
		@DocumentId
		private Integer id;

		@Fields({
				@Field(analyze = Analyze.NO),
				@Field(analyze = Analyze.NO, name = "facet_value")
		})
		@Facet(forField = "facet_value")
		private double value;
	}

	@Indexed
	public class Qux {
		@DocumentId
		private Integer id;

		@Fields({
				@Field(analyze = Analyze.NO),
				@Field(analyze = Analyze.NO, name = "facet_value")
		})
		@Facets({
				@Facet,
				@Facet(forField = "facet_value", encoding = FacetEncodingType.STRING)
		})
		private double value;
	}

	@Indexed
	public class Snafu {
		@DocumentId
		private Integer id;

		@Field
		@Facet
		private String name;
	}

	@Indexed
	public class TypeWithNumericFieldReferringToNonExistantField {

		@DocumentId
		private Integer id;

		@NumericField(forField = "nonExistant")
		@Field
		private short name;
	}

	@Indexed
	public class TypeWithNumericFieldWithoutField {

		@DocumentId
		private Integer id;

		@NumericField
		private short name;
	}

	@Indexed
	public class TypeWithSeveralNumericFieldsReferringToSameField {

		@DocumentId
		private Integer id;

		@NumericField(forField = "name")
		@NumericField(forField = "name") //Intentionally illegal
		@Field(name = "name")
		private short name;
	}
}
