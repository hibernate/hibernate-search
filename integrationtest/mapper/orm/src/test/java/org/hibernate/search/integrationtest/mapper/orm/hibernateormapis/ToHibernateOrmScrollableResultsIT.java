/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.hibernateormapis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.QueryTimeoutException;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubNextScrollWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.assertj.core.api.InstanceOfAssertFactories;

/**
 * Test the compatibility layer between our APIs and Hibernate ORM APIs
 * for the {@link Query} class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToHibernateOrmScrollableResultsIT {

	private static final int ENTITY_COUNT = 1000;
	private static final int DEFAULT_FETCH_SIZE = 100;

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );
	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backendMock.expectAnySchema( IndexedEntity.NAME );
		sessionFactory = ormSetupHelper.start().withAnnotatedTypes( IndexedEntity.class ).setup();
	}

	@BeforeEach
	void initData() {
		backendMock.inLenientMode( () -> with( sessionFactory ).runInTransaction( session -> {
			for ( int i = 0; i < ENTITY_COUNT; i++ ) {
				IndexedEntity indexed = new IndexedEntity();
				indexed.setId( i );
				indexed.setText( "this is text (" + i + ")" );
				session.persist( indexed );
			}
		} ) );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void next() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();
				assertThat( scroll.get() ).isNull();

				for ( int i = 0; i < ENTITY_COUNT; i++ ) {
					expectScrollNextIfNecessary( i, DEFAULT_FETCH_SIZE, ENTITY_COUNT );
					assertThat( scroll.next() ).isTrue();
					backendMock.verifyExpectationsMet();

					assertThat( scroll.getRowNumber() ).isEqualTo( i );
					assertThat( scroll.isFirst() ).isEqualTo( i == 0 );
					assertThat( scroll.isLast() ).isEqualTo( i == ( ENTITY_COUNT - 1 ) );
					assertThat( scroll.get() )
							.isEqualTo( session.getReference( IndexedEntity.class, i ) );
				}

				assertThat( scroll.next() ).isFalse();
				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				// Call next() again after reaching the end: should not do anything
				assertThat( scroll.next() ).isFalse();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				expectScrollClose();
			}
		} );
	}

	@Test
	void previous() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				// Going to the previous element means going backwards: it's forbidden
				assertThatThrownBy( () -> scroll.previous() )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining(
								"Cannot scroll backwards with Hibernate Search scrolls: they are forwards-only",
								"Ensure you always increment the scroll position, and never decrement it" );

				// We're still on the same element
				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( 100, references( 0, DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.next() ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 0 );
				assertThat( scroll.isFirst() ).isTrue();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 0 ) );

				// Going to the previous element means going backwards: it's forbidden
				assertThatThrownBy( () -> scroll.previous() )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining(
								"Cannot scroll backwards with Hibernate Search scrolls: they are forwards-only",
								"Ensure you always increment the scroll position, and never decrement it" );

				// We're still on the same element
				assertThat( scroll.getRowNumber() ).isEqualTo( 0 );
				assertThat( scroll.isFirst() ).isTrue();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 0 ) );

				expectScrollClose();
			}
		} );
	}

	@Test
	void scrollMode_forwardsOnly() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll( ScrollMode.FORWARD_ONLY ) ) {
				backendMock.verifyExpectationsMet();
				for ( int i = 0; i < ENTITY_COUNT; i++ ) {
					expectScrollNextIfNecessary( i, DEFAULT_FETCH_SIZE, ENTITY_COUNT );
					assertThat( scroll.next() ).isTrue();
					backendMock.verifyExpectationsMet();
					assertThat( scroll.get() )
							.isEqualTo( session.getReference( IndexedEntity.class, i ) );
				}

				expectScrollClose();
			}
		} );
	}

	@Test
	void scrollMode_invalid() {
		with( sessionFactory ).runInTransaction( session -> {
			Query<IndexedEntity> query = createSimpleQuery( session );

			assertThatThrownBy( () -> query.scroll( ScrollMode.SCROLL_SENSITIVE ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Cannot use scroll() with scroll mode 'SCROLL_SENSITIVE' with Hibernate Search queries",
							"only ScrollMode.FORWARDS_ONLY is supported" );
			assertThatThrownBy( () -> query.scroll( ScrollMode.SCROLL_INSENSITIVE ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Cannot use scroll() with scroll mode 'SCROLL_INSENSITIVE' with Hibernate Search queries",
							"only ScrollMode.FORWARDS_ONLY is supported" );
		} );
	}

	@Test
	void fetchSize() {
		with( sessionFactory ).runInTransaction( session -> {
			int customFetchSize = 10;
			backendMock.expectScrollObjects( Collections.singletonList( IndexedEntity.NAME ),
					customFetchSize, b -> {} );
			try ( ScrollableResults<?> scroll = Search.toOrmQuery( Search.session( session )
					.search( IndexedEntity.class )
					.where( f -> f.matchAll() )
					.loading( o -> o.fetchSize( customFetchSize ) )
					.toQuery() )
					.scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				for ( int i = 0; i < ENTITY_COUNT; i++ ) {
					expectScrollNextIfNecessary( i, customFetchSize, ENTITY_COUNT );
					assertThat( scroll.next() ).isTrue();
					backendMock.verifyExpectationsMet();

					assertThat( scroll.getRowNumber() ).isEqualTo( i );
					assertThat( scroll.isFirst() ).isEqualTo( i == 0 );
					assertThat( scroll.isLast() ).isEqualTo( i == ( ENTITY_COUNT - 1 ) );
					assertThat( scroll.get() )
							.isEqualTo( session.getReference( IndexedEntity.class, i ) );
				}

				assertThat( scroll.next() ).isFalse();
				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				expectScrollClose();
			}
		} );
	}

	@Test
	void maxResults() {
		with( sessionFactory ).runInTransaction( session -> {
			int maxResults = 200;
			Query<IndexedEntity> query = Search.toOrmQuery( Search.session( session )
					.search( IndexedEntity.class )
					.where( f -> f.matchAll() )
					.toQuery() );
			query.setMaxResults( maxResults );
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = query.scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				for ( int i = 0; i < maxResults; i++ ) {
					expectScrollNextIfNecessary( i, DEFAULT_FETCH_SIZE, maxResults );
					assertThat( scroll.next() ).isTrue();
					backendMock.verifyExpectationsMet();

					assertThat( scroll.getRowNumber() ).isEqualTo( i );
					assertThat( scroll.isFirst() ).isEqualTo( i == 0 );
					assertThat( scroll.isLast() ).isEqualTo( i == ( maxResults - 1 ) );
					assertThat( scroll.get() )
							.isEqualTo( session.getReference( IndexedEntity.class, i ) );
				}

				assertThat( scroll.next() ).isFalse();
				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				expectScrollClose();
			}
		} );
	}

	@Test
	void scroll() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.scroll( 10 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 9 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 9 ) );

				// Scroll 0 positions: should not do anything
				assertThat( scroll.scroll( 0 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 9 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 9 ) );

				// No call to the underlying scroll.next() is expected here
				assertThat( scroll.scroll( 50 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 59 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 59 ) );

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( DEFAULT_FETCH_SIZE, 2 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 2 * DEFAULT_FETCH_SIZE, 3 * DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.scroll( 200 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 259 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 259 ) );

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 3 * DEFAULT_FETCH_SIZE, 4 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 4 * DEFAULT_FETCH_SIZE, 5 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 5 * DEFAULT_FETCH_SIZE, 6 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 6 * DEFAULT_FETCH_SIZE, 7 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 7 * DEFAULT_FETCH_SIZE, 8 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 8 * DEFAULT_FETCH_SIZE, 9 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 9 * DEFAULT_FETCH_SIZE, 10 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.afterLast() );
				assertThat( scroll.scroll( 10000 ) ).isFalse();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				// Calling scroll(<positive number>) again after reaching the end should not do anything
				assertThat( scroll.scroll( 0 ) ).isFalse();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				assertThat( scroll.scroll( 1 ) ).isFalse();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				expectScrollClose();
			}
		} );
	}

	@Test
	void scroll_backwards() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				// scroll(<negative integer>) means going backwards: it's forbidden
				assertThatThrownBy( () -> scroll.scroll( -1 ) )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining(
								"Cannot scroll backwards with Hibernate Search scrolls: they are forwards-only",
								"Ensure you always increment the scroll position, and never decrement it" );

				// We're still on the same element
				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.scroll( 10 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 9 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 9 ) );

				// scroll(<negative integer>) means going backwards: it's forbidden
				assertThatThrownBy( () -> scroll.scroll( -5 ) )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining(
								"Cannot scroll backwards with Hibernate Search scrolls: they are forwards-only",
								"Ensure you always increment the scroll position, and never decrement it" );

				// We're still on the same element
				assertThat( scroll.getRowNumber() ).isEqualTo( 9 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 9 ) );

				expectScrollClose();
			}
		} );
	}

	@Test
	void setRowNumber() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.setRowNumber( 10 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 10 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 10 ) );

				// No call to the underlying scroll.next() is expected here
				assertThat( scroll.setRowNumber( 50 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 50 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 50 ) );

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( DEFAULT_FETCH_SIZE, 2 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 2 * DEFAULT_FETCH_SIZE, 3 * DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.setRowNumber( 220 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 220 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 220 ) );

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 3 * DEFAULT_FETCH_SIZE, 4 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 4 * DEFAULT_FETCH_SIZE, 5 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 5 * DEFAULT_FETCH_SIZE, 6 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 6 * DEFAULT_FETCH_SIZE, 7 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 7 * DEFAULT_FETCH_SIZE, 8 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 8 * DEFAULT_FETCH_SIZE, 9 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 9 * DEFAULT_FETCH_SIZE, 10 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.afterLast() );
				assertThat( scroll.setRowNumber( 10000 ) ).isFalse();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				expectScrollClose();
			}
		} );
	}

	@Test
	void setRowNumber_backwards() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.setRowNumber( 10 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 10 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 10 ) );

				// setRowNumber(<previous row number>) means going backwards: it's forbidden
				assertThatThrownBy( () -> scroll.setRowNumber( 5 ) )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining(
								"Cannot scroll backwards with Hibernate Search scrolls: they are forwards-only",
								"Ensure you always increment the scroll position, and never decrement it" );

				// We're still on the same element
				assertThat( scroll.getRowNumber() ).isEqualTo( 10 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 10 ) );

				expectScrollClose();
			}
		} );
	}

	@Test
	void setRowNumber_relativeToEnd() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.setRowNumber( 10 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 10 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 10 ) );

				// setRowNumber(<negative integer>) means going to a position relative to the end: it's forbidden
				assertThatThrownBy( () -> scroll.setRowNumber( -500 ) )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining(
								"Cannot set the scroll position relative to the end with Hibernate Search scrolls",
								"Ensure you always pass a positive number to setRowNumber()" );

				// We're still on the same element
				assertThat( scroll.getRowNumber() ).isEqualTo( 10 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 10 ) );

				expectScrollClose();
			}
		} );
	}

	@Test
	void position() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.position( 10 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 10 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 10 ) );

				// No call to the underlying scroll.next() is expected here
				assertThat( scroll.position( 50 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 50 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 50 ) );

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( DEFAULT_FETCH_SIZE, 2 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 2 * DEFAULT_FETCH_SIZE, 3 * DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.position( 220 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 220 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 220 ) );

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 3 * DEFAULT_FETCH_SIZE, 4 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 4 * DEFAULT_FETCH_SIZE, 5 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 5 * DEFAULT_FETCH_SIZE, 6 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 6 * DEFAULT_FETCH_SIZE, 7 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 7 * DEFAULT_FETCH_SIZE, 8 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 8 * DEFAULT_FETCH_SIZE, 9 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 9 * DEFAULT_FETCH_SIZE, 10 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.afterLast() );
				assertThat( scroll.position( 10000 ) ).isFalse();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				expectScrollClose();
			}
		} );
	}

	@Test
	void position_backwards() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.position( 10 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 10 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 10 ) );

				// position(<previous row number>) means going backwards: it's forbidden
				assertThatThrownBy( () -> scroll.position( 5 ) )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining(
								"Cannot scroll backwards with Hibernate Search scrolls: they are forwards-only",
								"Ensure you always increment the scroll position, and never decrement it" );

				// We're still on the same element
				assertThat( scroll.getRowNumber() ).isEqualTo( 10 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 10 ) );

				expectScrollClose();
			}
		} );
	}

	@Test
	void position_relativeToEnd() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.position( 10 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 10 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 10 ) );

				// position(<negative integer>) means going to a position relative to the end: it's forbidden
				assertThatThrownBy( () -> scroll.position( -500 ) )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining(
								"Cannot set the scroll position relative to the end with Hibernate Search scrolls",
								"Ensure you always pass a positive number to position()" );

				// We're still on the same element
				assertThat( scroll.getRowNumber() ).isEqualTo( 10 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 10 ) );

				expectScrollClose();
			}
		} );
	}

	@Test
	void beforeFirst_fromBeforeFirst() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				// Calling beforeFirst() when we're before the first element should not do anything
				scroll.beforeFirst();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				// next() still works after a call to beforeFirst()
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.next() ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 0 );
				assertThat( scroll.isFirst() ).isTrue();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 0 ) );

				expectScrollClose();
			}
		} );
	}

	@Test
	void beforeFirst_fromFirstOrAfter() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.next() ).isTrue();
				backendMock.verifyExpectationsMet();

				// We're now on the first element
				assertThat( scroll.getRowNumber() ).isEqualTo( 0 );
				assertThat( scroll.isFirst() ).isTrue();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 0 ) );

				// Going before the first element would means going backwards: it's forbidden
				assertThatThrownBy( () -> scroll.beforeFirst() )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining(
								"Cannot scroll backwards with Hibernate Search scrolls: they are forwards-only",
								"Ensure you always increment the scroll position, and never decrement it" );

				expectScrollClose();
			}
		} );
	}

	@Test
	void first() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				scroll.first();
				backendMock.verifyExpectationsMet();

				// We're now on the first element
				assertThat( scroll.getRowNumber() ).isEqualTo( 0 );
				assertThat( scroll.isFirst() ).isTrue();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 0 ) );

				// Calling first() when we're on the first element should not do anything
				scroll.first();

				assertThat( scroll.getRowNumber() ).isEqualTo( 0 );
				assertThat( scroll.isFirst() ).isTrue();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 0 ) );

				// next() still works after a call to first()
				assertThat( scroll.next() ).isTrue();

				assertThat( scroll.getRowNumber() ).isEqualTo( 1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 1 ) );

				// Going to the first element would means going backwards: it's forbidden
				assertThatThrownBy( () -> scroll.first() )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining(
								"Cannot scroll backwards with Hibernate Search scrolls: they are forwards-only",
								"Ensure you always increment the scroll position, and never decrement it" );

				expectScrollClose();
			}
		} );
	}

	@Test
	void first_fromAfterFirst() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.next() ).isTrue();
				assertThat( scroll.next() ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 1 ) );

				// Going to the first element would means going backwards: it's forbidden
				assertThatThrownBy( () -> scroll.first() )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining(
								"Cannot scroll backwards with Hibernate Search scrolls: they are forwards-only",
								"Ensure you always increment the scroll position, and never decrement it" );

				expectScrollClose();
			}
		} );
	}

	@Test
	void last() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( DEFAULT_FETCH_SIZE, 2 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 2 * DEFAULT_FETCH_SIZE, 3 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 3 * DEFAULT_FETCH_SIZE, 4 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 4 * DEFAULT_FETCH_SIZE, 5 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 5 * DEFAULT_FETCH_SIZE, 6 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 6 * DEFAULT_FETCH_SIZE, 7 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 7 * DEFAULT_FETCH_SIZE, 8 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 8 * DEFAULT_FETCH_SIZE, 9 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 9 * DEFAULT_FETCH_SIZE, 10 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.afterLast() );
				assertThat( scroll.last() ).isTrue();
				backendMock.verifyExpectationsMet();

				// We're now on the last element
				assertThat( scroll.getRowNumber() ).isEqualTo( ENTITY_COUNT - 1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isTrue();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, ENTITY_COUNT - 1 ) );

				// Calling last() when we're on the last element should not do anything
				scroll.last();

				assertThat( scroll.getRowNumber() ).isEqualTo( ENTITY_COUNT - 1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isTrue();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, ENTITY_COUNT - 1 ) );

				// next() still works after a call to last()
				assertThat( scroll.next() ).isFalse();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				// Going to the last element would means going backwards: it's forbidden
				assertThatThrownBy( () -> scroll.last() )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining(
								"Cannot scroll backwards with Hibernate Search scrolls: they are forwards-only",
								"Ensure you always increment the scroll position, and never decrement it" );

				expectScrollClose();
			}
		} );
	}

	@Test
	void afterLast_fromLastOrBefore() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				assertThat( scroll.scroll( 10 ) ).isTrue();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( 9 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() )
						.isEqualTo( session.getReference( IndexedEntity.class, 9 ) );

				scroll.afterLast();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				expectScrollClose();
			}
		} );
	}

	@Test
	void afterLast_fromAfterLast() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( DEFAULT_FETCH_SIZE, 2 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 2 * DEFAULT_FETCH_SIZE, 3 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 3 * DEFAULT_FETCH_SIZE, 4 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 4 * DEFAULT_FETCH_SIZE, 5 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 5 * DEFAULT_FETCH_SIZE, 6 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 6 * DEFAULT_FETCH_SIZE, 7 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 7 * DEFAULT_FETCH_SIZE, 8 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 8 * DEFAULT_FETCH_SIZE, 9 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( 9 * DEFAULT_FETCH_SIZE, 10 * DEFAULT_FETCH_SIZE ) ) );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.afterLast() );
				assertThat( scroll.scroll( ENTITY_COUNT + 1 ) ).isFalse();
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				scroll.afterLast();

				// We're still on the same element
				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );
				assertThat( scroll.isFirst() ).isFalse();
				assertThat( scroll.isLast() ).isFalse();
				assertThat( scroll.get() ).isNull();

				expectScrollClose();
			}
		} );
	}

	@Test
	void close() {
		with( sessionFactory ).runInTransaction( session -> {
			expectScrollCreate();
			try ( ScrollableResults<?> scroll = createSimpleQuery( session ).scroll() ) {
				backendMock.verifyExpectationsMet();

				ScrollableResultsImplementor<?> implementor = (ScrollableResultsImplementor<?>) scroll;

				assertThat( implementor.isClosed() ).isFalse();

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT, references( 0, DEFAULT_FETCH_SIZE ) ) );
				scroll.next();

				assertThat( implementor.isClosed() ).isFalse();

				expectScrollClose();
				scroll.close();

				assertThat( implementor.isClosed() ).isTrue();

				// Mutating methods should no longer work after the results are closed
				Consumer<Throwable> exceptionExpectations = e -> assertThat( e )
						.asInstanceOf( InstanceOfAssertFactories.THROWABLE )
						.isInstanceOf( SearchException.class )
						.hasMessageContaining( "Cannot use this ScrollableResults instance: it is closed" );
				assertThatThrownBy( () -> scroll.next() ).satisfies( exceptionExpectations );
				assertThatThrownBy( () -> scroll.previous() ).satisfies( exceptionExpectations );
				assertThatThrownBy( () -> scroll.scroll( -1 ) ).satisfies( exceptionExpectations );
				assertThatThrownBy( () -> scroll.scroll( 0 ) ).satisfies( exceptionExpectations );
				assertThatThrownBy( () -> scroll.scroll( 1 ) ).satisfies( exceptionExpectations );
				assertThatThrownBy( () -> scroll.beforeFirst() ).satisfies( exceptionExpectations );
				assertThatThrownBy( () -> scroll.first() ).satisfies( exceptionExpectations );
				assertThatThrownBy( () -> scroll.last() ).satisfies( exceptionExpectations );
				assertThatThrownBy( () -> scroll.afterLast() ).satisfies( exceptionExpectations );
				assertThatThrownBy( () -> scroll.first() ).satisfies( exceptionExpectations );
			}
		} );
	}

	@Test
	void timeout() {
		with( sessionFactory ).runInTransaction( session -> {
			backendMock.expectScrollObjects( Collections.singletonList( IndexedEntity.NAME ),
					DEFAULT_FETCH_SIZE, b -> b.failAfter( 200, TimeUnit.MILLISECONDS ) );
			try ( ScrollableResults<?> scroll = createSimpleQuery( session )
					.setHint( "jakarta.persistence.query.timeout", 200 )
					.scroll() ) {
				backendMock.verifyExpectationsMet();

				assertThat( scroll.getRowNumber() ).isEqualTo( -1 );

				SearchTimeoutException timeoutException = new SearchTimeoutException( "Timed out" );

				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.failing( () -> timeoutException ) );
				assertThatThrownBy( scroll::next )
						.isInstanceOf( QueryTimeoutException.class )
						.hasCause( timeoutException );

				expectScrollClose();
			}
		} );
	}

	private void expectScrollCreate() {
		backendMock.expectScrollObjects( Collections.singletonList( IndexedEntity.NAME ),
				DEFAULT_FETCH_SIZE, b -> {} );
	}

	private void expectScrollNextIfNecessary(int i, int fetchSize, int totalHitCount) {
		Integer scrollNextFirstId = null;
		if ( i == 0 ) {
			// When moving to the first element, expect a call to the underlying scroll.next()
			// to get the first chunk.
			scrollNextFirstId = 0;
		}
		else if ( ( i + 1 ) % fetchSize == 0 ) {
			// After moving to the last element of each chunk, expect a call to the underlying scroll.next()
			// to get the next chunk (so that isLast() can be implemented)
			scrollNextFirstId = i + 1;
		}
		if ( scrollNextFirstId != null ) {
			if ( scrollNextFirstId < totalHitCount ) {
				int lastIdExclusive = Math.min( scrollNextFirstId + fetchSize, totalHitCount );
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.of( ENTITY_COUNT,
								references( scrollNextFirstId, lastIdExclusive ) ) );
			}
			else {
				backendMock.expectNextScroll( Collections.singletonList( IndexedEntity.NAME ),
						StubNextScrollWorkBehavior.afterLast() );
			}
		}
	}

	private void expectScrollClose() {
		backendMock.expectCloseScroll( Collections.singletonList( IndexedEntity.NAME ) );
	}

	private Query<IndexedEntity> createSimpleQuery(Session session) {
		return Search.toOrmQuery( Search.session( session )
				.search( IndexedEntity.class )
				.where( f -> f.matchAll() )
				.toQuery() );
	}

	private List<DocumentReference> references(int startInclusive, int endExclusive) {
		List<DocumentReference> result = new ArrayList<>();
		for ( int i = startInclusive; i < endExclusive; i++ ) {
			result.add( StubBackendUtils.reference( IndexedEntity.NAME, String.valueOf( i ) ) );
		}
		return result;
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.NAME)
	public static class IndexedEntity {

		public static final String NAME = "indexed";

		@Id
		@GenericField
		private Integer id;

		@FullTextField
		private String text;

		@Override
		public String toString() {
			return "IndexedEntity[id=" + id + "]";
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

}
