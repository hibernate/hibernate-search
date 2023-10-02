/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.mapping;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;

public class AbortedEventsGenerator {
	private static final AtomicInteger ID_GENERATOR = new AtomicInteger( 1 );

	private final SessionFactory sessionFactory;
	private final BackendMock backendMock;
	private final Object tenantId;

	private final String indexName;
	private final BiConsumer<Session, Integer> entityCreator;

	public AbortedEventsGenerator(SessionFactory sessionFactory, BackendMock backendMock) {
		this( sessionFactory, backendMock, null );
	}

	public AbortedEventsGenerator(SessionFactory sessionFactory, BackendMock backendMock, String tenantId) {
		this( sessionFactory, backendMock, tenantId, IndexedEntity.INDEX, (session, id) -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( id );
			entity1.setIndexedField( "initialValue" );
			session.persist( entity1 );
		} );
	}

	public AbortedEventsGenerator(SessionFactory sessionFactory, BackendMock backendMock, Object tenantId,
			String indexName, BiConsumer<Session, Integer> entityCreator) {
		this.sessionFactory = sessionFactory;
		this.backendMock = backendMock;
		this.tenantId = tenantId;
		this.indexName = indexName;
		this.entityCreator = entityCreator;
	}

	List<Integer> generateThreeAbortedEvents() {
		List<Integer> generatedIds;
		if ( tenantId == null ) {
			generatedIds = with( sessionFactory ).applyInTransaction( this::generateThreeAbortedEvents );
		}
		else {
			generatedIds = with( sessionFactory, tenantId ).applyInTransaction( this::generateThreeAbortedEvents );
		}
		backendMock.verifyExpectationsMet();
		return generatedIds;
	}

	private List<Integer> generateThreeAbortedEvents(Session session) {
		int id1 = ID_GENERATOR.getAndIncrement();
		int id2 = ID_GENERATOR.getAndIncrement();
		int id3 = ID_GENERATOR.getAndIncrement();
		entityCreator.accept( session, id1 );
		entityCreator.accept( session, id2 );
		entityCreator.accept( session, id3 );

		CompletableFuture<?> failingFuture = new CompletableFuture<>();
		failingFuture.completeExceptionally( new SimulatedFailure( "Indexing work failed!" ) );
		String id1String = Integer.toString( id1 );
		String id2String = Integer.toString( id2 );
		String id3String = Integer.toString( id3 );
		backendMock.expectWorks( indexName, tenantId )
				.createAndExecuteFollowingWorks( failingFuture )
				.add( id1String, b -> b
						.field( "indexedField", "initialValue" )
				)
				.add( id2String, b -> b
						.field( "indexedField", "initialValue" )
				)
				.add( id3String, b -> b
						.field( "indexedField", "initialValue" )
				)
				// retry (fails too):
				.createAndExecuteFollowingWorks( failingFuture )
				.addOrUpdate( id1String, b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( id2String, b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( id3String, b -> b
						.field( "indexedField", "initialValue" )
				)
				// retry (fails too):
				.createAndExecuteFollowingWorks( failingFuture )
				.addOrUpdate( id1String, b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( id2String, b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( id3String, b -> b
						.field( "indexedField", "initialValue" )
				);
		// no more retry
		return Arrays.asList( id1, id2, id3 );
	}

	public static class SimulatedFailure extends RuntimeException {
		SimulatedFailure(String message) {
			super( message );
		}
	}
}
