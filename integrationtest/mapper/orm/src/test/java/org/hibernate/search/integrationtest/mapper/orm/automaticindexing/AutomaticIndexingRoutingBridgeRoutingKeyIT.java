/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import java.util.Locale;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

public class AutomaticIndexingRoutingBridgeRoutingKeyIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.field( "text", String.class, b2 -> b2.analyzerName( AnalyzerNames.DEFAULT ) )
		);

		setupContext.withAnnotatedTypes( IndexedEntity.class );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4537")
	public void testLifecycle() {
		setupHolder.runInTransaction( entityManager -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setText( "Initial" );
			entity1.setCategory( Category.CAT_1 );
			entityManager.persist( entity1 );

			if ( !setupHolder.areEntitiesProcessedInSession() ) {
				// When processing out of session, we don't make a difference between add and addOrUpdate,
				// and thus we execute some potentially unnecessary deletes
				// to remove any older versions of the document.
				backendMock.expectWorks( IndexedEntity.NAME )
						.delete( b -> b.identifier( "1" ).routingKey( "cat-2" ) )
						.delete( b -> b.identifier( "1" ).routingKey( "cat-3" ) );
			}

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( b -> b.identifier( "1" ).routingKey( "cat-1" )
							.document( StubDocumentNode.document()
									.field( "text", "Initial" )
									.build() ) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = session.getReference( IndexedEntity.class, 1 );
			entity1.setText( "Updated" );

			backendMock.expectWorks( IndexedEntity.NAME )
					.delete( b -> b.identifier( "1" ).routingKey( "cat-2" ) )
					.delete( b -> b.identifier( "1" ).routingKey( "cat-3" ) )
					.addOrUpdate( b -> b.identifier( "1" ).routingKey( "cat-1" )
							.document( StubDocumentNode.document()
									.field( "text", "Updated" )
									.build() ) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = session.getReference( IndexedEntity.class, 1 );
			entity1.setCategory( Category.CAT_2 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.delete( b -> b.identifier( "1" ).routingKey( "cat-1" ) )
					.delete( b -> b.identifier( "1" ).routingKey( "cat-3" ) )
					.addOrUpdate( b -> b.identifier( "1" ).routingKey( "cat-2" )
							.document( StubDocumentNode.document()
									.field( "text", "Updated" )
									.build() ) );
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = session.getReference( IndexedEntity.class, 1 );
			entity1.setText( "Updated again" );

			backendMock.expectWorks( IndexedEntity.NAME )
					.delete( b -> b.identifier( "1" ).routingKey( "cat-1" ) )
					.delete( b -> b.identifier( "1" ).routingKey( "cat-3" ) )
					.addOrUpdate( b -> b.identifier( "1" ).routingKey( "cat-2" )
							.document( StubDocumentNode.document()
									.field( "text", "Updated again" )
									.build() ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(routingBinder = @RoutingBinderRef(type = RoutingKeyRoutingBinder.class))
	public static class IndexedEntity {

		public static final String NAME = "IndexedEntity";

		@Id
		private Integer id;

		@FullTextField
		private String text;

		@Basic(optional = false)
		private Category category;

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

		public Category getCategory() {
			return category;
		}

		public void setCategory(Category category) {
			this.category = category;
		}
	}

	public static class RoutingKeyRoutingBinder implements RoutingBinder {

		@Override
		public void bind(RoutingBindingContext context) {
			context.dependencies()
					.use( "category" );

			context.bridge( IndexedEntity.class, new Bridge() );
		}

		public static class Bridge implements RoutingBridge<IndexedEntity> {
			@Override
			public void route(DocumentRoutes routes, Object entityIdentifier, IndexedEntity indexedEntity,
					RoutingBridgeRouteContext context) {
				String routingKey = toRoutingKey( indexedEntity.getCategory() );
				routes.addRoute().routingKey( routingKey );
			}

			@Override
			public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, IndexedEntity indexedEntity,
					RoutingBridgeRouteContext context) {
				for ( Category possiblePreviousCategory : Category.values() ) {
					String routingKey = toRoutingKey( possiblePreviousCategory );
					routes.addRoute().routingKey( routingKey );
				}
			}

			private String toRoutingKey(Category category) {
				return category.name().toLowerCase( Locale.ROOT ).replace( '_', '-' );
			}
		}
	}

	public enum Category {

		CAT_1,
		CAT_2,
		CAT_3

	}
}
