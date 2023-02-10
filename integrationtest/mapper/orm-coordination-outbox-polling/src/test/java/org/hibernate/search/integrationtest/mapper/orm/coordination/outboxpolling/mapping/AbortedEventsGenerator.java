/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.mapping;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.concurrent.CompletableFuture;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

public class AbortedEventsGenerator {

	private final SessionFactory sessionFactory;
	private final BackendMock backendMock;
	private final String tenantId;

	public AbortedEventsGenerator(SessionFactory sessionFactory, BackendMock backendMock) {
		this.sessionFactory = sessionFactory;
		this.backendMock = backendMock;
		this.tenantId = null;
	}

	public AbortedEventsGenerator(SessionFactory sessionFactory, BackendMock backendMock, String tenantId) {
		this.sessionFactory = sessionFactory;
		this.backendMock = backendMock;
		this.tenantId = tenantId;
	}

	void generateThreeAbortedEvents() {
		if ( tenantId == null ) {
			with( sessionFactory ).runInTransaction( this::generateThreeAbortedEvents );
		}
		else {
			with( sessionFactory, tenantId ).runInTransaction( this::generateThreeAbortedEvents );
		}

		backendMock.verifyExpectationsMet();
	}

	private void generateThreeAbortedEvents(Session session) {
		IndexedEntity entity1 = new IndexedEntity();
		entity1.setId( 1 );
		entity1.setIndexedField( "initialValue" );
		session.persist( entity1 );

		IndexedEntity entity2 = new IndexedEntity();
		entity2.setId( 2 );
		entity2.setIndexedField( "initialValue" );
		session.persist( entity2 );

		IndexedEntity entity3 = new IndexedEntity();
		entity3.setId( 3 );
		entity3.setIndexedField( "initialValue" );
		session.persist( entity3 );

		CompletableFuture<?> failingFuture = new CompletableFuture<>();
		failingFuture.completeExceptionally( new SimulatedFailure( "Indexing work failed!" ) );

		backendMock.expectWorks( IndexedEntity.INDEX, tenantId )
				.createAndExecuteFollowingWorks( failingFuture )
				.add( "1", b -> b
						.field( "indexedField", "initialValue" )
				)
				.add( "2", b -> b
						.field( "indexedField", "initialValue" )
				)
				.add( "3", b -> b
						.field( "indexedField", "initialValue" )
				)
				// retry (fails too):
				.createAndExecuteFollowingWorks( failingFuture )
				.addOrUpdate( "1", b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( "2", b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( "3", b -> b
						.field( "indexedField", "initialValue" )
				)
				// retry (fails too):
				.createAndExecuteFollowingWorks( failingFuture )
				.addOrUpdate( "1", b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( "2", b -> b
						.field( "indexedField", "initialValue" )
				)
				.addOrUpdate( "3", b -> b
						.field( "indexedField", "initialValue" )
				);
		// no more retry
	}

	public static class SimulatedFailure extends RuntimeException {
		SimulatedFailure(String message) {
			super( message );
		}
	}
}
