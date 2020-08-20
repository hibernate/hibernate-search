/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Groups together tests to check the defaults used when a custom field is created using a
 * {@link MetadataProvidingFieldBridge}.
 * <p>
 * The entity is expected to have three fields: an id, an analyzed field and a not analyzed field.
 * Each of these fields will have a copy created using {@link AdditionalFieldBridge}.
 *
 * @author Davide D'Alto
 */
public abstract class CheckCustomFieldDefaultAnalyzer {

	private static final String ENTITY_ID = "GLaDOS";

	@Rule
	public final SearchFactoryHolder holder = new SearchFactoryHolder( getEntityType() );

	private final SearchITHelper helper = new SearchITHelper( holder );

	@Before
	public void before() throws Exception {
		helper.add( entity() );
	}

	private Object entity() {
		return entity( ENTITY_ID, "CHELL", "WELL DONE. HERE COME THE TEST RESULTS: 'YOU ARE A HORRIBLE PERSON." );
	}

	protected abstract Object entity(String id, String notAnalyzedField, String analyzedField);

	protected abstract Class<?> getEntityType();

	@Test
	public void shouldBeAbleToFindTheCustomIdField() throws Exception {
		helper.assertThat( "copy_of_id", "GLaDOS" )
				.from( getEntityType() )
				.matchesExactlyIds( ENTITY_ID );
	}

	@Test
	public void shouldNotAnalyzeCustomIdField() throws Exception {
		helper.assertThat( "copy_of_id", "glados" )
				.from( getEntityType() )
				.matchesNone();
	}

	@Test
	public void shouldBeAbleToFindNotAnalyzedCustomField() throws Exception {
		helper.assertThat( "copy_of_subject", "CHELL" )
				.from( getEntityType() )
				.matchesExactlyIds( ENTITY_ID );
	}

	@Test
	public void shouldNotAnalyzeCustomField() throws Exception {
		helper.assertThat( "copy_of_subject", "chell" )
				.from( getEntityType() )
				.matchesNone();
	}

	@Test
	// The analyzer is applied for the annotated field
	public void shouldBeAbleToFindAnalyzedAnnotatedField() throws Exception {
		helper.assertThat( "result", "HORRIBLE" )
				.from( getEntityType() )
				.matchesExactlyIds( ENTITY_ID );
	}

	@Test
	// The custom field will use the default analyzer instead of the one defined on the field
	public void shouldNotBeAbleToFindAnalyzedCustomField() throws Exception {
		helper.assertThat( "copy_of_result", "HORRIBLE" )
				.from( getEntityType() )
				.matchesNone();
	}

	@Test
	// The custom field used the default analyzer instead of the one defined on the field
	public void shouldBeAbleToFindAnalyzedCustomField() throws Exception {
		helper.assertThat( "copy_of_result", "horrible" )
				.from( getEntityType() )
				.matchesExactlyIds( ENTITY_ID );
	}

	public static class AdditionalFieldBridge implements MetadataProvidingFieldBridge, TwoWayFieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			luceneOptions.addFieldToDocument( name, (String) value, document );
			luceneOptions.addFieldToDocument( copyOf( name ), (String) value, document );
		}

		private String copyOf(String name) {
			return "copy_of_" + name;
		}

		@Override
		public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
			builder.field( copyOf( name ), FieldType.STRING );
		}

		@Override
		public Object get(String name, Document document) {
			return document.get( name );
		}

		@Override
		public String objectToString(Object object) {
			return (String) object;
		}
	}
}
