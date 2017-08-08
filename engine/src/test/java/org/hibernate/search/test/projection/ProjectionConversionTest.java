/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.projection;

import java.util.Locale;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.LongBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.concurrency.ConcurrentRunner;
import org.hibernate.search.testsupport.concurrency.ConcurrentRunner.TaskFactory;
import org.hibernate.search.testsupport.junit.ElasticsearchSupportInProgress;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

/**
 * This test verifies the correct projection rules (like which FieldBridge)
 * needs to be applied on each projected field.
 * As witnessed by HSEARCH-1786 and HSEARCH-1814, it is possible that when
 * having multiple entity types or non trivial entity graphs that the same
 * field name has different mapping rules per type, and conflicts are possible
 * when the projection handling code is not careful about this.
 *
 * @author Sanne Grinovero (C) 2015 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1786")
public class ProjectionConversionTest {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( ExampleEntity.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Before
	public void storeTestData() {
		ExampleEntity entity = new ExampleEntity();
		entity.id = 1l;
		entity.someInteger = 5;
		entity.longEncodedAsText = 20l;
		entity.unstoredField = "unstoredField";
		entity.customBridgedKeyword = "lowercase-keyword";
		entity.customOneWayBridgedKeyword = "lowercase-keyword";
		entity.customTwoWayBridgedKeywordWithMetadataOverride = "lowercase-keyword";

		ExampleEntity embedded = new ExampleEntity();
		embedded.id = 2l;
		embedded.someInteger = 6;
		embedded.longEncodedAsText = 21l;
		embedded.unstoredField = "unstoredFieldEmbedded";
		embedded.customBridgedKeyword = "another-lowercase-keyword";
		embedded.customOneWayBridgedKeyword = "another-lowercase-keyword";
		embedded.customTwoWayBridgedKeywordWithMetadataOverride = "another-lowercase-keyword";

		ConflictingMappedType second = new ConflictingMappedType();
		second.id = "a string";
		second.customBridgedKeyword = 17l;

		entity.embedded = embedded;
		entity.second = second;

		helper.add( entity );
	}

	@Test
	public void projectingExplicitId() {
		projectionTestHelper( ProjectionConstants.ID, Long.valueOf( 1l ) );
	}

	@Test
	public void projectingIdByPropertyName() {
		projectionTestHelper( "id", Long.valueOf( 1l ) );
	}

	@Test
	public void projectingIdOnOverloadedMapping() {
		projectionTestHelper( "stringTypedId", Long.valueOf( 1l ) );
	}

	@Test
	public void projectingIntegerField() {
		projectionTestHelper( "someInteger", Integer.valueOf( 5 ) );
	}

	@Test
	public void projectingUnknownField() {
		projectionTestHelper( "someNonExistingField", null );
	}

	@Test
	@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2423 Projecting an unstored field should raise an exception
	public void projectingUnstoredField() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000323" );
		thrown.expectMessage( "unstoredField" );

		projectionTestHelper( "unstoredField", null );
	}

	@Test
	public void projectionWithCustomBridge() {
		projectionTestHelper( "customBridgedKeyword", "lowercase-keyword" );
	}

	@Test
	public void projectionWithCustomOneWayBridge() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000324" );
		thrown.expectMessage( "customOneWayBridgedKeyword" );
		thrown.expectMessage( CustomOneWayBridge.class.getName() );

		projectionTestHelper( "customOneWayBridgedKeyword", "lowercase-keyword" );
	}

	@Test
	public void projectionWithCustomBridgeOverridingMetadata() {
		projectionTestHelper( "customTwoWayBridgedKeywordWithMetadataOverride", "lowercase-keyword" );
	}

	@Test
	public void projectingEmbeddedIdByPropertyName() {
		projectionTestHelper( "embedded.id", Long.valueOf( 2l ) );
	}

	@Test
	public void projectingEmbeddedIdOnOverloadedMapping() {
		projectionTestHelper( "embedded.stringTypedId", Long.valueOf( 2l ) );
	}

	@Test
	public void projectingEmbeddedWithCustomBridge() {
		projectionTestHelper( "embedded.customBridgedKeyword", "another-lowercase-keyword" );
	}

	@Test
	public void projectingEmbeddedWithCustomOneWayBridge() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000324" );
		thrown.expectMessage( "embedded.customOneWayBridgedKeyword" );
		thrown.expectMessage( CustomOneWayBridge.class.getName() );

		projectionTestHelper( "embedded.customOneWayBridgedKeyword", "another-lowercase-keyword" );
	}

	@Test
	public void projectingEmbeddedWithCustomBridgeOverridingMetadata() {
		projectionTestHelper( "embedded.customTwoWayBridgedKeywordWithMetadataOverride", "another-lowercase-keyword" );
	}

	@Test
	public void projectingNotIncludedEmbeddedField() {
		projectionTestHelper( "embedded.someInteger", null );
	}

	@Test
	public void projectingOnConflictingMappingEmbeddedField() {
		projectionTestHelper( "second.customBridgedKeyword", Long.valueOf( 17l ) );
	}

	@Test
	public void projectingOnConflictingMappedIdField() {
		projectionTestHelper( "second.id", "a string" );
	}

	@Test
	public void concurrentMixedProjections() throws Exception {
		//The point of this test is to "simultaneously" project multiple different types
		new ConcurrentRunner( 1000, 20,
			new TaskFactory() {
				@Override
				public Runnable createRunnable(int i) throws Exception {
					return new Runnable() {
						@Override
						public void run() {
							projectingExplicitId();
							projectingIdOnOverloadedMapping();
							projectingIntegerField();
							projectingUnknownField();
							projectionWithCustomBridge();
							projectingEmbeddedIdByPropertyName();
							projectingEmbeddedIdOnOverloadedMapping();
							projectingEmbeddedWithCustomBridge();
							projectingOnConflictingMappingEmbeddedField();
							projectingOnConflictingMappedIdField();
						}
					};
				}
			}
		).execute();
	}

	void projectionTestHelper(String projectionField, Object expectedValue) {
		helper.assertThat()
				.from( ExampleEntity.class )
				.projecting( projectionField )
				.matchesExactlySingleProjections( expectedValue );
	}

	@Indexed
	public static class ExampleEntity {

		@DocumentId @Field(name = "stringTypedId", store = Store.YES)
		Long id;

		@Field(store = Store.YES)
		Integer someInteger;

		@Field(store = Store.YES) @FieldBridge(impl = LongBridge.class)
		Long longEncodedAsText;

		@Field(store = Store.NO)
		String unstoredField;

		@Field(store = Store.YES)
		@FieldBridge(impl = CustomTwoWayBridge.class)
		String customBridgedKeyword;

		@Field(store = Store.YES)
		@FieldBridge(impl = CustomOneWayBridge.class)
		String customOneWayBridgedKeyword;

		@Field(store = Store.YES)
		@FieldBridge(impl = CustomTwoWayBridgeOverridingDefaultFieldMetadata.class)
		String customTwoWayBridgedKeywordWithMetadataOverride;

		@IndexedEmbedded(
				includePaths = {
					"id", "stringTypedId", "customBridgedKeyword", "customOneWayBridgedKeyword",
					"customTwoWayBridgedKeywordWithMetadataOverride"
				},
				includeEmbeddedObjectId = true
		)
		ExampleEntity embedded;

		@IndexedEmbedded(includeEmbeddedObjectId = true)
		ConflictingMappedType second;

	}

	@Indexed
	public static class ConflictingMappedType {

		@DocumentId
		String id;

		@Field(store = Store.YES)
		Long customBridgedKeyword; //Misleading field name on purpose

	}

	public static class CustomTwoWayBridge implements TwoWayFieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			luceneOptions.addFieldToDocument( name, String.valueOf( value ).toUpperCase( Locale.ENGLISH ), document );
		}

		@Override
		public Object get(String name, Document document) {
			IndexableField field = document.getField( name );
			String stringValue = field.stringValue();
			return stringValue.toLowerCase( Locale.ENGLISH );
		}

		@Override
		public String objectToString(Object object) {
			return String.valueOf( object );
		}

	}

	public static class CustomOneWayBridge implements org.hibernate.search.bridge.FieldBridge, StringBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			luceneOptions.addFieldToDocument( name, String.valueOf( value ).toUpperCase( Locale.ENGLISH ), document );
		}

		@Override
		public String objectToString(Object object) {
			return String.valueOf( object );
		}

	}

	public static class CustomTwoWayBridgeOverridingDefaultFieldMetadata implements TwoWayFieldBridge, MetadataProvidingFieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			luceneOptions.addFieldToDocument( name + ".value", String.valueOf( value ).toUpperCase( Locale.ENGLISH ), document );
		}

		@Override
		public Object get(String name, Document document) {
			IndexableField field = document.getField( name + ".value" );
			String stringValue = field.stringValue();
			return stringValue.toLowerCase( Locale.ENGLISH );
		}

		@Override
		public String objectToString(Object object) {
			return String.valueOf( object );
		}

		@Override
		public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
			builder.field( name, FieldType.OBJECT );
			builder.field( name + ".value", FieldType.STRING );
		}

	}
}
