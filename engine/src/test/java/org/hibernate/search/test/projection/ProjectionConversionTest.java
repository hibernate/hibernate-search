/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.projection;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.LongBridge;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.concurrency.ConcurrentRunner;
import org.hibernate.search.testsupport.concurrency.ConcurrentRunner.TaskFactory;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * This test verifies the correct projection rules (like which FieldBridge)
 * needs to be applied on each projected field.
 * As witnessed by HSEARCH-1786 and HSEARCH-1814, it is possible that when
 * having multiple entity types or non trivial entity graphs that the same
 * field name has different mapping rules per type, and conflicts are possible
 * when the projection handling code is not careful about this.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2015 Red Hat Inc.
 */
@TestForIssue(jiraKey = "HSEARCH-1786")
public class ProjectionConversionTest {

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( ExampleEntity.class );

	@Before
	public void storeTestData() {
		ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();
		ExampleEntity entity = new ExampleEntity();
		entity.id = 1l;
		entity.someInteger = 5;
		entity.longEncodedAsText = 20l;
		entity.customBridgedKeyword = "lowercase-keyword";

		ExampleEntity embedded = new ExampleEntity();
		embedded.id = 2l;
		embedded.someInteger = 6;
		embedded.longEncodedAsText = 21l;
		embedded.customBridgedKeyword = "another-lowercase-keyword";

		ConflictingMappedType second = new ConflictingMappedType();
		second.id = "a string";
		second.customBridgedKeyword = 17l;

		entity.embedded = embedded;
		entity.second = second;

		Work work = new Work( entity, entity.id, WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		searchFactory.getWorker().performWork( work, tc );
		tc.end();
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
	public void projectionWithCustomBridge() {
		projectionTestHelper( "customBridgedKeyword", "lowercase-keyword" );
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
		ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();
		QueryBuilder queryBuilder = searchFactory.buildQueryBuilder().forEntity( ExampleEntity.class ).get();
		Query queryAllGuests = queryBuilder.all().createQuery();
		List<EntityInfo> queryEntityInfos = searchFactory.createHSQuery().luceneQuery( queryAllGuests )
				.targetedEntities( Arrays.asList( new Class<?>[] { ExampleEntity.class } ) )
				.projection( projectionField )
				.queryEntityInfos();

		Assert.assertEquals( 1, queryEntityInfos.size() );
		EntityInfo entityInfo = queryEntityInfos.get( 0 );
		Object projectedValue = entityInfo.getProjection()[0];
		Assert.assertEquals( expectedValue, projectedValue );
	}

	@Indexed
	public static class ExampleEntity {

		@DocumentId @Field(name = "stringTypedId", store = Store.YES)
		Long id;

		@Field(store = Store.YES)
		Integer someInteger;

		@Field(store = Store.YES) @FieldBridge(impl = LongBridge.class)
		Long longEncodedAsText;

		@Field(store = Store.YES) @FieldBridge(impl = CustomTwoWayBridge.class)
		String customBridgedKeyword;

		@IndexedEmbedded(includePaths = { "id", "stringTypedId", "customBridgedKeyword" }, includeEmbeddedObjectId = true)
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
}
