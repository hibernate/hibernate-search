/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutomaticIndexingRoutingBridgeConditionalIndexingIT {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );
	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, b2 -> b2.analyzerName( AnalyzerNames.DEFAULT ) )
		);

		sessionFactory = ormSetupHelper.start().withAnnotatedTypes( IndexedEntity.class ).setup();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4537")
	void testLifecycle() {
		with( sessionFactory ).runInTransaction( entityManager -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setText( "Initial" );
			entity1.setStatus( Status.DRAFT );
			entityManager.persist( entity1 );

			// Do not expect any work:
			// * The routing bridge interprets the DRAFT status as "not indexed"
			// * Newly created entities don't require any delete work
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.getReference( IndexedEntity.class, 1 );
			entity1.setText( "Updated" );

			// Still not any work:
			// * The routing bridge interprets the DRAFT status as "not indexed"
			// * The routing bridge assumes the DRAFT status means the document has never been indexed,
			//   so we don't need to delete previous versions of the document
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.getReference( IndexedEntity.class, 1 );
			entity1.setStatus( Status.PUBLISHED );

			// The routing bridge interprets the PUBLISHED status as "indexed"
			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( "1", b -> b
							.field( "text", "Updated" ) );
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.getReference( IndexedEntity.class, 1 );
			entity1.setText( "Updated again" );

			// The routing bridge interprets the PUBLISHED status as "indexed"
			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( "1", b -> b
							.field( "text", "Updated again" ) );
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.getReference( IndexedEntity.class, 1 );
			entity1.setStatus( Status.ARCHIVED );

			// The routing bridge interprets the ARCHIVED status as "not indexed"
			backendMock.expectWorks( IndexedEntity.NAME )
					.delete( "1" );
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.getReference( IndexedEntity.class, 1 );
			entity1.setText( "Updated yet again" );

			// Delete the document again, even if it's superfluous:
			// * The routing bridge interprets the ARCHIVED status as "not indexed"
			// * The routing bridge assumes the ARCHIVED status means the document *may* have been indexed,
			//   so we need to delete previous versions of the document
			// Maybe this could be optimized, but that would require giving the bridge
			// access to the list of changed properties (so that it seems the status didn't change),
			// and that would require new APIs.
			backendMock.expectWorks( IndexedEntity.NAME )
					.delete( "1" );
		} );
		backendMock.verifyExpectationsMet();
	}

	public static class ConditionalIndexingRoutingBinder implements RoutingBinder {

		@Override
		public void bind(RoutingBindingContext context) {
			context.dependencies()
					.use( "status" );

			context.bridge( IndexedEntity.class, new Bridge() );
		}

		public static class Bridge implements RoutingBridge<IndexedEntity> {
			@Override
			public void route(DocumentRoutes routes, Object entityIdentifier, IndexedEntity indexedEntity,
					RoutingBridgeRouteContext context) {
				switch ( indexedEntity.getStatus() ) {
					case PUBLISHED:
						routes.addRoute();
						break;
					case DRAFT:
					case ARCHIVED:
						routes.notIndexed();
						break;
				}
			}

			@Override
			public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, IndexedEntity indexedEntity,
					RoutingBridgeRouteContext context) {
				switch ( indexedEntity.getStatus() ) {
					case DRAFT:
						// We know a draft has always been a draft and thus cannot have been indexed.
						routes.notIndexed();
						break;
					case PUBLISHED:
					case ARCHIVED:
						routes.addRoute();
						break;
				}
			}
		}
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(routingBinder = @RoutingBinderRef(type = ConditionalIndexingRoutingBinder.class))
	public static class IndexedEntity {

		public static final String NAME = "IndexedEntity";

		@Id
		private Integer id;

		@FullTextField
		private String text;

		@Basic(optional = false)
		private Status status;

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

		public Status getStatus() {
			return status;
		}

		public void setStatus(Status status) {
			this.status = status;
		}
	}

	public enum Status {

		DRAFT,
		PUBLISHED,
		ARCHIVED

	}
}
