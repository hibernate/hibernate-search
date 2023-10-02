/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.projection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.concurrency.ConcurrentRunner;
import org.hibernate.search.testsupport.concurrency.ConcurrentRunner.TaskFactory;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.Tags;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
class ProjectionConversionTest {


	@RegisterExtension
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( ExampleEntity.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@BeforeEach
	void storeTestData() {
		ExampleEntity entity = new ExampleEntity();
		entity.id = 1L;
		entity.someInteger = 5;
		entity.longEncodedAsText = String.valueOf( 20L );
		entity.unstoredField = "unstoredField";

		ExampleEntity embedded = new ExampleEntity();
		embedded.id = 2L;
		embedded.someInteger = 6;
		embedded.longEncodedAsText = String.valueOf( 21L );
		embedded.unstoredField = "unstoredFieldEmbedded";

		ConflictingMappedType second = new ConflictingMappedType();
		second.id = "a string";

		entity.embedded = embedded;
		embedded.containing = entity;
		entity.second = second;

		helper.add( entity );
	}

	@Test
	void projectingExplicitId() {
		projectionTestHelper( ProjectionConstants.ID, Long.valueOf( 1L ) );
	}

	@Test
	void projectingIdOnOverloadedMapping() {
		projectionTestHelper( "stringTypedId", Long.valueOf( 1L ) );
	}

	@Test
	void projectingIntegerField() {
		projectionTestHelper( "someInteger", Integer.valueOf( 5 ) );
	}

	@Test
	@Tag(Tags.ELASTICSEARCH_SUPPORT_IN_PROGRESS) // HSEARCH-2423 Projecting an unstored field should raise an exception
	void projectingUnstoredField() {
		assertThatThrownBy( () -> projectionTestHelper( "unstoredField", null ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'projection:field' on field 'unstoredField'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);
	}

	@Test
	void projectingEmbeddedIdByPropertyName() {
		projectionTestHelper( "embedded.id", Long.valueOf( 2L ) );
	}

	@Test
	void projectingEmbeddedIdOnOverloadedMapping() {
		projectionTestHelper( "embedded.stringTypedId", Long.valueOf( 2L ) );
	}

	@Test
	void projectingOnConflictingMappedIdField() {
		projectionTestHelper( "second.id", "a string" );
	}

	@Test
	void concurrentMixedProjections() throws Exception {
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
								projectingEmbeddedIdByPropertyName();
								projectingEmbeddedIdOnOverloadedMapping();
								projectingOnConflictingMappedIdField();
							}
						};
					}
				}
		).execute();
	}

	void projectionTestHelper(String projectionField, Object expectedValue) {
		helper.assertThatQuery()
				.from( ExampleEntity.class )
				.projecting( projectionField )
				.matchesExactlySingleProjections( expectedValue );
	}

	@Indexed
	public static class ExampleEntity {

		@DocumentId
		@Field(name = "stringTypedId", store = Store.YES)
		Long id;

		@Field(store = Store.YES)
		Integer someInteger;

		@Field(store = Store.YES)
		String longEncodedAsText;

		@Field(store = Store.NO)
		String unstoredField;

		@IndexedEmbedded(
				includePaths = {
						"id",
						"stringTypedId"
				},
				includeEmbeddedObjectId = true
		)
		ExampleEntity embedded;

		@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "embedded")))
		ExampleEntity containing;

		@IndexedEmbedded(includeEmbeddedObjectId = true)
		ConflictingMappedType second;

	}

	@Indexed
	public static class ConflictingMappedType {

		@DocumentId
		String id;

	}

}
