/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.array;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import org.hibernate.SessionFactory;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * An abstract base for tests dealing with automatic indexing based on Hibernate ORM entity events
 * and involving an array.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractAutomaticIndexingArrayIT<TIndexed, TArray, TIndexField> {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private final ArrayModelPrimitives<TIndexed, TArray, TIndexField> primitives;
	private SessionFactory sessionFactory;

	AbstractAutomaticIndexingArrayIT(ArrayModelPrimitives<TIndexed, TArray, TIndexField> primitives) {
		this.primitives = primitives;
	}

	@BeforeAll
	void setup() {
		backendMock.expectSchema( primitives.getIndexName(), b -> b
				.field( "serializedArray", primitives.getExpectedIndexFieldType(),
						b2 -> b2.multiValued( true ) )
				.field( "elementCollectionArray", primitives.getExpectedIndexFieldType(),
						b2 -> b2.multiValued( true ) ) );

		sessionFactory =
				ormSetupHelper.start().withAnnotatedTypes( primitives.getIndexedClass() ).setup();
	}

	@Test
	void serializedArray_replaceArray() {
		with( sessionFactory ).runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TArray array1 = primitives.newArray( 2 );
			primitives.setElement( array1, 0, 0 );
			primitives.setElement( array1, 1, 1 );
			primitives.setSerializedArray( entity1, array1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b.field( "serializedArray",
							primitives.getExpectedIndexFieldValue( array1, 0 ),
							primitives.getExpectedIndexFieldValue( array1, 1 ) ) );
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing the array
		with( sessionFactory ).runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TArray array2 = primitives.newArray( 3 );
			primitives.setElement( array2, 0, 1 );
			primitives.setElement( array2, 1, 2 );
			primitives.setElement( array2, 2, 3 );
			primitives.setSerializedArray( entity1, array2 );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b.field( "serializedArray",
							primitives.getExpectedIndexFieldValue( array2, 0 ),
							primitives.getExpectedIndexFieldValue( array2, 1 ),
							primitives.getExpectedIndexFieldValue( array2, 2 ) ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void serializedArray_replaceElement() {
		with( sessionFactory ).runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TArray array = primitives.newArray( 2 );
			primitives.setElement( array, 0, 0 );
			primitives.setElement( array, 1, 1 );
			primitives.setSerializedArray( entity1, array );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b.field( "serializedArray",
							primitives.getExpectedIndexFieldValue( array, 0 ),
							primitives.getExpectedIndexFieldValue( array, 1 ) ) );
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing an element in the array
		with( sessionFactory ).runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TArray array = primitives.getSerializedArray( entity1 );
			primitives.setElement( array, 1, 2 );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b.field( "serializedArray",
							primitives.getExpectedIndexFieldValue( array, 0 ),
							primitives.getExpectedIndexFieldValue( array, 1 ) ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void elementCollectionArray_replaceArray() {
		with( sessionFactory ).runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TArray array1 = primitives.newArray( 2 );
			primitives.setElement( array1, 0, 0 );
			primitives.setElement( array1, 1, 1 );
			primitives.setElementCollectionArray( entity1, array1 );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b.field( "elementCollectionArray",
							primitives.getExpectedIndexFieldValue( array1, 0 ),
							primitives.getExpectedIndexFieldValue( array1, 1 ) ) );
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing the array
		with( sessionFactory ).runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TArray array2 = primitives.newArray( 3 );
			primitives.setElement( array2, 0, 1 );
			primitives.setElement( array2, 1, 2 );
			primitives.setElement( array2, 2, 3 );
			primitives.setElementCollectionArray( entity1, array2 );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b.field( "elementCollectionArray",
							primitives.getExpectedIndexFieldValue( array2, 0 ),
							primitives.getExpectedIndexFieldValue( array2, 1 ),
							primitives.getExpectedIndexFieldValue( array2, 2 ) ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void elementCollectionArray_replaceElement() {
		with( sessionFactory ).runInTransaction( session -> {
			TIndexed entity1 = primitives.newIndexed( 1 );

			TArray array = primitives.newArray( 2 );
			primitives.setElement( array, 0, 0 );
			primitives.setElement( array, 1, 1 );
			primitives.setElementCollectionArray( entity1, array );

			session.persist( entity1 );

			backendMock.expectWorks( primitives.getIndexName() )
					.add( "1", b -> b.field( "elementCollectionArray",
							primitives.getExpectedIndexFieldValue( array, 0 ),
							primitives.getExpectedIndexFieldValue( array, 1 ) ) );
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing an element in the array
		with( sessionFactory ).runInTransaction( session -> {
			TIndexed entity1 = session.get( primitives.getIndexedClass(), 1 );

			TArray array = primitives.getElementCollectionArray( entity1 );
			primitives.setElement( array, 1, 2 );

			backendMock.expectWorks( primitives.getIndexName() )
					.addOrUpdate( "1", b -> b.field( "elementCollectionArray",
							primitives.getExpectedIndexFieldValue( array, 0 ),
							primitives.getExpectedIndexFieldValue( array, 1 ) ) );
		} );
		backendMock.verifyExpectationsMet();
	}

}
