/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.runInTransaction;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.mapper.orm.HibernateOrmExtension;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test an actual use case of explicit reindexing declaration,
 * where the indexed -> contained side of the association does not exist.
 * <p>
 * Functionally, this test makes more sense than AutomaticIndexingBridgeExplicitReindexingBaseIT,
 * tests the feature works correctly but in a setup that wouldn't require this feature.
 */
@TestForIssue(jiraKey = "HSEARCH-3297")
public class AutomaticIndexingBridgeExplicitReindexingFunctionalIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "typeBridge", b2 -> b2
						.field( "includedInTypeBridge", String.class )
				)
		);

		sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						new HibernateOrmSearchMappingConfigurer() {
							@Override
							public void configure(HibernateOrmMappingConfigurationContext context) {
								context.programmaticMapping().type( IndexedEntity.class )
										.binder( new QueryBasedTypeBridge.Binder() );
							}
						}
				)
				.setup(
						IndexedEntity.class,
						ContainedEntity.class
				);
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void test() {
		// Init
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			session.persist( entity1 );
			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "typeBridge", b2 -> {} )
					);
		} );
		backendMock.verifyExpectationsMet();

		// Add a contained entity
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.getReference( IndexedEntity.class, 1 );
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 2 );
			containedEntity.setParent( entity1 );
			containedEntity.setIncludedInTypeBridge( "value1" );
			session.persist( containedEntity );
			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "includedInTypeBridge", "value1" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		/*
		 * Add irrelevant contained entities.
		 * Unfortunately the indexed entity will still be reindexed,
		 * because Search doesn't know which contained entities are relevant.
		 */
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.getReference( IndexedEntity.class, 1 );
			for ( int i = 3; i < 100; ++i ) {
				ContainedEntity containedEntity = new ContainedEntity();
				containedEntity.setId( i );
				containedEntity.setParent( entity1 );
				session.persist( containedEntity );
			}
			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "includedInTypeBridge", "value1" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		/*
		 * Update one contained entity.
		 * The bridge will not load any entity and will just retrieve data from the database.
		 */
		with( sessionFactory ).runNoTransaction( session -> {
			runInTransaction( session, tx -> {
				ContainedEntity containedEntity = session.getReference( ContainedEntity.class, 10 );
				containedEntity.setIncludedInTypeBridge( "value2" );
				backendMock.expectWorks( IndexedEntity.INDEX )
						.addOrUpdate( "1", b -> b
								.objectField( "typeBridge", b2 -> b2
										.field( "includedInTypeBridge", "value1", "value2" )
								)
						);
			} );
			backendMock.verifyExpectationsMet();

			// Only the indexed and contained entity should have been loaded
			assertThat( session.getStatistics().getEntityCount() ).isEqualTo( 2 );
		} );

		// Remove one contained entity.
		with( sessionFactory ).runNoTransaction( session -> {
			ContainedEntity containedEntity = session.getReference( ContainedEntity.class, 10 );
			containedEntity.setParent( null );
			session.remove( containedEntity );

			// TODO HSEARCH-3567 this does not trigger any work because we do not handle asymmetric association updates
			//			backendMock.expectWorks( IndexedEntity.INDEX )
			//					.addOrUpdate( "1", b -> b
			//							.objectField( "typeBridge", b2 -> b2
			//									.field( "includedInTypeBridge", "value1" )
			//							)
			//					);
		} );
		backendMock.verifyExpectationsMet();
	}


	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {
		static final String INDEX = "indexedentity";

		@Id
		private Integer id;

		// We do NOT model the children *on purpose*. For example in a real setup there could be too many children.

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "contained")
	public static class ContainedEntity {
		@Id
		private Integer id;

		@ManyToOne
		private IndexedEntity parent;

		private String includedInTypeBridge;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public IndexedEntity getParent() {
			return parent;
		}

		public void setParent(IndexedEntity parent) {
			this.parent = parent;
		}

		public String getIncludedInTypeBridge() {
			return includedInTypeBridge;
		}

		public void setIncludedInTypeBridge(String includedInTypeBridge) {
			this.includedInTypeBridge = includedInTypeBridge;
		}
	}

	public static class QueryBasedTypeBridge implements TypeBridge<IndexedEntity> {

		private final IndexObjectFieldReference typeBridgeObjectFieldReference;
		private final IndexFieldReference<String> includedInTypeBridgeFieldReference;

		private QueryBasedTypeBridge(TypeBindingContext context) {
			context.dependencies()
					.fromOtherEntity( ContainedEntity.class, "parent" )
					.use( "includedInTypeBridge" );

			IndexSchemaObjectField typeBridgeObjectField = context.indexSchemaElement().objectField( "typeBridge" );
			typeBridgeObjectFieldReference = typeBridgeObjectField.toReference();
			includedInTypeBridgeFieldReference = typeBridgeObjectField.field(
					"includedInTypeBridge", f -> f.asString()
			)
					.toReference();
		}

		@Override
		public void write(DocumentElement target, IndexedEntity bridgedElement, TypeBridgeWriteContext context) {
			Session session = context.extension( HibernateOrmExtension.get() ).session();
			/*
			 * Note this approach is rather limited as it does not allow batching.
			 * HSEARCH-1937 should address this problem.
			 */
			Query<String> query = session.createQuery(
					"select c.includedInTypeBridge from contained c"
							+ " where c.parent = :parent and c.includedInTypeBridge is not null"
							+ " order by c.includedInTypeBridge",
					String.class
			);
			query.setParameter( "parent", bridgedElement );

			DocumentElement typeBridgeObjectField = target.addObject( typeBridgeObjectFieldReference );
			for ( String includedInTypeBridge : query.list() ) {
				typeBridgeObjectField.addValue( includedInTypeBridgeFieldReference, includedInTypeBridge );
			}
		}

		public static class Binder implements TypeBinder {
			@Override
			public void bind(TypeBindingContext context) {
				context.bridge( IndexedEntity.class, new QueryBasedTypeBridge( context ) );
			}
		}
	}
}
