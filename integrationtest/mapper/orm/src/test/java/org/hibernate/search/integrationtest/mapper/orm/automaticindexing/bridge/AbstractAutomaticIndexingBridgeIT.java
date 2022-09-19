/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.bridge;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * An abstract base for tests dealing with automatic indexing based on Hibernate ORM entity events when
 * {@link TypeBridge}s or {@link PropertyBridge}s are involved.
 */
public abstract class AbstractAutomaticIndexingBridgeIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	public void directPersistUpdateDelete() {
		SessionFactory sessionFactory = setupWithTypeBridge();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setDirectField( "initialValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", entity1.getDirectField() )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", null )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setDirectField( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", entity1.getDirectField() )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", null )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			session.remove( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.delete( "1" );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_typeBridge() {
		SessionFactory sessionFactory = setupWithTypeBridge();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setAssociation1( containingEntity1 );
			containingEntity1.setAssociation1InverseSide( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setAssociation1( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setAssociation1InverseSide( containingEntity1 );

			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", null )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInTypeBridge( "initialValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", "initialValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInTypeBridge( "updatedValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedSingle().getContainingAsSingle().clear();
			containingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", "updatedValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the bridge scope)
		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 6 );
			containedEntity.setIncludedInTypeBridge( "outOfScopeValue" );

			ContainingEntity deeplyNestedContainingEntity1 = session.get( ContainingEntity.class, 3 );
			deeplyNestedContainingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		with( sessionFactory ).runInTransaction( session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedSingle().getContainingAsSingle().clear();
			containingEntity1.setContainedSingle( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", null )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_typeBridge() {
		SessionFactory sessionFactory = setupWithTypeBridge();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setAssociation1( containingEntity1 );
			containingEntity1.setAssociation1InverseSide( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setAssociation1( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setAssociation1InverseSide( containingEntity1 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 4 );
			containedEntity1.setIncludedInTypeBridge( "initialValue" );
			containingEntity1.setContainedSingle( containedEntity1 );
			containedEntity1.getContainingAsSingle().add( containingEntity1 );

			ContainedEntity containedEntity2 = new ContainedEntity();
			containedEntity2.setId( 5 );
			containedEntity2.setIncludedInTypeBridge( "initialOutOfScopeValue" );
			deeplyNestedContainingEntity.setContainedSingle( containedEntity2 );
			containedEntity2.getContainingAsSingle().add( deeplyNestedContainingEntity );

			session.persist( containedEntity1 );
			session.persist( containedEntity2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", "initialValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setIncludedInTypeBridge( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "typeBridge", b2 -> b2
									.field( "directField", null )
									.objectField( "child", b3 -> b3
											.field( "includedInTypeBridge", "updatedValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is not included in the bridge
		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setExcludedFromAll( "updatedExcludedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is too deeply nested to matter (it's out of the bridge scope)
		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 5 );
			containedEntity.setIncludedInTypeBridge( "updatedOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_singleValuedPropertyBridge() {
		SessionFactory sessionFactory = setupWithSingleValuedPropertyBridge();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setAssociation1( containingEntity1 );
			containingEntity1.setAssociation1InverseSide( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setAssociation1( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setAssociation1InverseSide( containingEntity1 );

			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "singleValuedPropertyBridge", b2 -> b2
									.field( "includedInSingleValuedPropertyBridge", null )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInSingleValuedPropertyBridge( "initialValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "singleValuedPropertyBridge", b2 -> b2
									.field( "includedInSingleValuedPropertyBridge", "initialValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInSingleValuedPropertyBridge( "updatedValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedSingle().getContainingAsSingle().clear();
			containingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "singleValuedPropertyBridge", b2 -> b2
									.field( "includedInSingleValuedPropertyBridge", "updatedValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the bridge scope)
		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 6 );
			containedEntity.setIncludedInSingleValuedPropertyBridge( "outOfScopeValue" );

			ContainingEntity deeplyNestedContainingEntity1 = session.get( ContainingEntity.class, 3 );
			deeplyNestedContainingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		with( sessionFactory ).runInTransaction( session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedSingle().getContainingAsSingle().clear();
			containingEntity1.setContainedSingle( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "singleValuedPropertyBridge", b2 -> b2
									.field( "includedInSingleValuedPropertyBridge", null )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2496")
	public void indirectValueUpdate_singleValuedPropertyBridge() {
		SessionFactory sessionFactory = setupWithSingleValuedPropertyBridge();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setAssociation1( containingEntity1 );
			containingEntity1.setAssociation1InverseSide( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setAssociation1( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setAssociation1InverseSide( containingEntity1 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 4 );
			containedEntity1.setIncludedInSingleValuedPropertyBridge( "initialValue" );
			containingEntity1.setContainedSingle( containedEntity1 );
			containedEntity1.getContainingAsSingle().add( containingEntity1 );

			ContainedEntity containedEntity2 = new ContainedEntity();
			containedEntity2.setId( 5 );
			containedEntity2.setIncludedInSingleValuedPropertyBridge( "initialOutOfScopeValue" );
			deeplyNestedContainingEntity.setContainedSingle( containedEntity2 );
			containedEntity2.getContainingAsSingle().add( deeplyNestedContainingEntity );

			session.persist( containedEntity1 );
			session.persist( containedEntity2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "singleValuedPropertyBridge", b2 -> b2
									.field( "includedInSingleValuedPropertyBridge", "initialValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setIncludedInSingleValuedPropertyBridge( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "singleValuedPropertyBridge", b2 -> b2
									.field( "includedInSingleValuedPropertyBridge", "updatedValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is not included in the bridge
		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setExcludedFromAll( "updatedExcludedValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is too deeply nested to matter (it's out of the bridge scope)
		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 5 );
			containedEntity.setIncludedInSingleValuedPropertyBridge( "updatedOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3297")
	public void directAssociationUpdate_multiValuedPropertyBridge() {
		SessionFactory sessionFactory = setupWithMultiValuedPropertyBridge();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "multiValuedPropertyBridge", b2 -> b2
									.field( "includedInMultiValuedPropertyBridge", null )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.getAssociation2().add( containingEntity1 );
			containingEntity1.setAssociation2InverseSide( entity1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );
			containedEntity.setIncludedInMultiValuedPropertyBridge( "value1" );
			containingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( containingEntity1 );

			session.persist( containingEntity1 );
			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "multiValuedPropertyBridge", b2 -> b2
									.field( "includedInMultiValuedPropertyBridge", "value1" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainingEntity containingEntity2 = new ContainingEntity();
			containingEntity2.setId( 4 );
			entity1.getAssociation2().add( containingEntity2 );
			containingEntity2.setAssociation2InverseSide( entity1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInMultiValuedPropertyBridge( "value2" );
			containingEntity2.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( containingEntity2 );

			session.persist( containingEntity2 );
			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "multiValuedPropertyBridge", b2 -> b2
									.field( "includedInMultiValuedPropertyBridge", "value1 value2" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainingEntity containingEntity3 = new ContainingEntity();
			containingEntity3.setId( 6 );
			for ( ContainingEntity containingEntity : entity1.getAssociation2() ) {
				containingEntity.setAssociation2InverseSide( null );
			}
			entity1.getAssociation2().clear();
			entity1.getAssociation2().add( containingEntity3 );
			containingEntity3.setAssociation2InverseSide( entity1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 7 );
			containedEntity.setIncludedInMultiValuedPropertyBridge( "updatedValue" );
			containingEntity3.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( containingEntity3 );

			session.persist( containingEntity3 );
			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "multiValuedPropertyBridge", b2 -> b2
									.field( "includedInMultiValuedPropertyBridge", "updatedValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			for ( ContainingEntity containingEntity : entity1.getAssociation2() ) {
				containingEntity.setAssociation2InverseSide( null );
			}
			entity1.getAssociation2().clear();

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "multiValuedPropertyBridge", b2 -> b2
									.field( "includedInMultiValuedPropertyBridge", null )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	protected abstract TypeBinder createContainingEntityTypeBinder();

	protected abstract PropertyBinder createContainingEntitySingleValuedPropertyBinder();

	protected abstract PropertyBinder createContainingEntityMultiValuedPropertyBinder();

	private SessionFactory setupWithTypeBridge() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "typeBridge", b2 -> b2
						.field( "directField", String.class )
						.objectField( "child", b3 -> b3
								.field( "includedInTypeBridge", String.class )
						)
				)
		);

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						new HibernateOrmSearchMappingConfigurer() {
							@Override
							public void configure(HibernateOrmMappingConfigurationContext context) {
								context.programmaticMapping().type( ContainingEntity.class )
										.binder( createContainingEntityTypeBinder() );
							}
						}
				)
				.setup(
						IndexedEntity.class,
						ContainedEntity.class
				);
		backendMock.verifyExpectationsMet();

		return sessionFactory;
	}

	private SessionFactory setupWithSingleValuedPropertyBridge() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "singleValuedPropertyBridge", b2 -> b2
						.field( "includedInSingleValuedPropertyBridge", String.class )
				)
		);

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						new HibernateOrmSearchMappingConfigurer() {
							@Override
							public void configure(HibernateOrmMappingConfigurationContext context) {
								context.programmaticMapping().type( ContainingEntity.class )
										.property( "association1" )
										.binder( createContainingEntitySingleValuedPropertyBinder() );
							}
						}
				)
				.setup(
						IndexedEntity.class,
						ContainedEntity.class
				);
		backendMock.verifyExpectationsMet();

		return sessionFactory;
	}

	private SessionFactory setupWithMultiValuedPropertyBridge() {
		PropertyBinder binder = createContainingEntityMultiValuedPropertyBinder();

		assumeTrue(
				"Multi-valued property bridges must be supported",
				binder != null
		);

		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "multiValuedPropertyBridge", b2 -> b2
						.field( "includedInMultiValuedPropertyBridge", String.class )
				)
		);

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						new HibernateOrmSearchMappingConfigurer() {
							@Override
							public void configure(HibernateOrmMappingConfigurationContext context) {
								context.programmaticMapping().type( ContainingEntity.class )
										.property( "association2" )
										.binder( binder );
							}
						}
				)
				.setup(
						IndexedEntity.class,
						ContainedEntity.class
				);
		backendMock.verifyExpectationsMet();

		return sessionFactory;
	}

	@Entity(name = "containing")
	public static class ContainingEntity {

		@Id
		private Integer id;

		@OneToOne
		private ContainingEntity association1InverseSide;

		@OneToOne(mappedBy = "association1InverseSide")
		private ContainingEntity association1;

		@ManyToOne
		private ContainingEntity association2InverseSide;

		@OneToMany(mappedBy = "association2InverseSide")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> association2;

		@ManyToOne
		private ContainedEntity containedSingle;

		@Basic
		private String directField;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainingEntity getAssociation1InverseSide() {
			return association1InverseSide;
		}

		public void setAssociation1InverseSide(ContainingEntity association1InverseSide) {
			this.association1InverseSide = association1InverseSide;
		}

		public ContainingEntity getAssociation1() {
			return association1;
		}

		public void setAssociation1(ContainingEntity association1) {
			this.association1 = association1;
		}

		public ContainingEntity getAssociation2InverseSide() {
			return association2InverseSide;
		}

		public void setAssociation2InverseSide(ContainingEntity association2InverseSide) {
			this.association2InverseSide = association2InverseSide;
		}

		public List<ContainingEntity> getAssociation2() {
			return association2;
		}

		public ContainedEntity getContainedSingle() {
			return containedSingle;
		}

		public void setContainedSingle(ContainedEntity containedSingle) {
			this.containedSingle = containedSingle;
		}

		public String getDirectField() {
			return directField;
		}

		public void setDirectField(String directField) {
			this.directField = directField;
		}

	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity extends ContainingEntity {
		static final String INDEX = "IndexedEntity";
	}

	@Entity(name = "contained")
	public static class ContainedEntity {

		@Id
		private Integer id;

		@OneToMany(mappedBy = "containedSingle")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsSingle = new ArrayList<>();

		@Basic
		private String includedInTypeBridge;

		@Basic
		@Column(name = "IISingleVPB")
		private String includedInSingleValuedPropertyBridge;

		@Basic
		@Column(name = "IIMultiVPB")
		private String includedInMultiValuedPropertyBridge;

		@Basic
		private String excludedFromAll;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<ContainingEntity> getContainingAsSingle() {
			return containingAsSingle;
		}

		public String getIncludedInTypeBridge() {
			return includedInTypeBridge;
		}

		public void setIncludedInTypeBridge(String includedInTypeBridge) {
			this.includedInTypeBridge = includedInTypeBridge;
		}

		public String getIncludedInSingleValuedPropertyBridge() {
			return includedInSingleValuedPropertyBridge;
		}

		public void setIncludedInSingleValuedPropertyBridge(String includedInSingleValuedPropertyBridge) {
			this.includedInSingleValuedPropertyBridge = includedInSingleValuedPropertyBridge;
		}

		public String getIncludedInMultiValuedPropertyBridge() {
			return includedInMultiValuedPropertyBridge;
		}

		public void setIncludedInMultiValuedPropertyBridge(String includedInMultiValuedPropertyBridge) {
			this.includedInMultiValuedPropertyBridge = includedInMultiValuedPropertyBridge;
		}

		public String getExcludedFromAll() {
			return excludedFromAll;
		}

		public void setExcludedFromAll(String excludedFromAll) {
			this.excludedFromAll = excludedFromAll;
		}
	}
}
