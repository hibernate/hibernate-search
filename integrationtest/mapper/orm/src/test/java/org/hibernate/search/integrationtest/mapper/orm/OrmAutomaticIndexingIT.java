/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.mapper.orm.cfg.SearchOrmSettings;
import org.hibernate.search.mapper.pojo.extractor.builtin.MapKeyExtractor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ContainerValueExtractorBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.service.ServiceRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test automatic indexing based on Hibernate ORM entity events.
 */
public class OrmAutomaticIndexingIT {

	private static final String PREFIX = SearchOrmSettings.PREFIX;

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
				.applySetting( PREFIX + "backend.stubBackend.type", StubBackendFactory.class.getName() )
				.applySetting( PREFIX + "index.default.backend", "stubBackend" );

		ServiceRegistry serviceRegistry = registryBuilder.build();

		MetadataSources ms = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( IndexedEntity.class )
				.addAnnotatedClass( ContainedEntity.class );

		Metadata metadata = ms.buildMetadata();

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();

		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "directField", String.class )
				.field( "directElementCollectionField", String.class )
				.objectField( "containedSingle", b2 -> b2
						.field( "includedInSingle", String.class )
						.field( "elementCollectionIncludedInSingle", String.class )
				)
				.objectField( "containedList", b2 -> b2
						.field( "includedInList", String.class )
				)
				.objectField( "containedMapValues", b2 -> b2
						.field( "includedInMapValues", String.class )
				)
				.objectField( "containedMapKeys", b2 -> b2
						.field( "includedInMapKeys", String.class )
				)
				.objectField( "child", b3 -> b3
						.objectField( "containedSingle", b2 -> b2
								.field( "includedInSingle", String.class )
								.field( "elementCollectionIncludedInSingle", String.class )
						)
						.objectField( "containedList", b2 -> b2
								.field( "includedInList", String.class )
						)
						.objectField( "containedMapValues", b2 -> b2
								.field( "includedInMapValues", String.class )
						)
						.objectField( "containedMapKeys", b2 -> b2
								.field( "includedInMapKeys", String.class )
						)
				)
		);

		sessionFactory = sfb.build();
		backendMock.verifyExpectationsMet();
	}

	@After
	public void cleanup() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Test
	public void directPersistUpdateDelete() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setDirectField( "initialValue" );
			entity1.getDirectElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", entity1.getDirectField() )
							.field(
									"directElementCollectionField",
									entity1.getDirectElementCollectionField().get( 0 )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setDirectField( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", entity1.getDirectField() )
							.field(
									"directElementCollectionField",
									entity1.getDirectElementCollectionField().get( 0 )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			session.delete( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.delete( "1" )
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void directValueUpdate_elementCollection() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.getDirectElementCollectionField().add( "firstValue" );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", null )
							.field(
									"directElementCollectionField",
									entity1.getDirectElementCollectionField().get( 0 )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a simple (single-valued) property
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setDirectField( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", entity1.getDirectField() )
							.field(
									"directElementCollectionField",
									entity1.getDirectElementCollectionField().get( 0 )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding an element to an ElementCollection (multi-valued) property
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getDirectElementCollectionField().add( "secondValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", entity1.getDirectField() )
							.field(
									"directElementCollectionField",
									entity1.getDirectElementCollectionField().get( 0 ),
									entity1.getDirectElementCollectionField().get( 1 )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding replacing an ElementCollection (multi-valued) property
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setDirectElementCollectionField( new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", entity1.getDirectField() )
							.field(
									"directElementCollectionField",
									entity1.getDirectElementCollectionField().get( 0 ),
									entity1.getDirectElementCollectionField().get( 1 )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test removing an element from an ElementCollection (multi-valued) property
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getDirectElementCollectionField().remove( 1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", entity1.getDirectField() )
							.field(
									"directElementCollectionField",
									entity1.getDirectElementCollectionField().get( 0 )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void directAssociationUpdate_singleAssociation() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", null )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 2 );
			containedEntity.setIncludedInSingle( "initialValue" );

			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "containedSingle", b2 -> b2
									.field( "includedInSingle", "initialValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );
			containedEntity.setIncludedInSingle( "updatedValue" );

			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getContainedSingle().getContainingAsSingle().clear();
			entity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "containedSingle", b2 -> b2
									.field( "includedInSingle", "updatedValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getContainedSingle().getContainingAsSingle().clear();
			entity1.setContainedSingle( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void directAssociationUpdate_listAssociation() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", null )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 2 );
			containedEntity.setIncludedInList( "firstValue" );

			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getContainedList().add( containedEntity );
			containedEntity.getContainingAsList().add( entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "containedList", b2 -> b2
									.field( "includedInList", "firstValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );
			containedEntity.setIncludedInList( "secondValue" );

			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getContainedList().add( containedEntity );
			containedEntity.getContainingAsList().add( entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "containedList", b2 -> b2
									.field( "includedInList", "firstValue" )
							)
							.objectField( "containedList", b2 -> b2
									.field( "includedInList", "secondValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			ContainedEntity containedEntity = entity1.getContainedList().get( 0 );
			containedEntity.getContainingAsList().clear();
			entity1.getContainedList().remove( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "containedList", b2 -> b2
									.field( "includedInList", "secondValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void directAssociationUpdate_mapValuesAssociation() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", null )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 2 );
			containedEntity.setIncludedInMapValues( "firstValue" );

			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getContainedMapValues().put( "first", containedEntity );
			containedEntity.getContainingAsMapValues().add( entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "containedMapValues", b2 -> b2
									.field( "includedInMapValues", "firstValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );
			containedEntity.setIncludedInMapValues( "secondValue" );

			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getContainedMapValues().put( "second", containedEntity );
			containedEntity.getContainingAsMapValues().add( entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "containedMapValues", b2 -> b2
									.field( "includedInMapValues", "firstValue" )
							)
							.objectField( "containedMapValues", b2 -> b2
									.field( "includedInMapValues", "secondValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			ContainedEntity containedEntity = entity1.getContainedMapValues().get( "first" );
			containedEntity.getContainingAsMapValues().clear();
			entity1.getContainedMapValues().values().remove( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "containedMapValues", b2 -> b2
									.field( "includedInMapValues", "secondValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void directAssociationUpdate_mapKeysAssociation() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", null )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 2 );
			containedEntity.setIncludedInMapKeys( "firstValue" );

			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getContainedMapKeys().put( containedEntity, "first" );
			containedEntity.getContainingAsMapKeys().add( entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "containedMapKeys", b2 -> b2
									.field( "includedInMapKeys", "firstValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );
			containedEntity.setIncludedInMapKeys( "secondValue" );

			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			entity1.getContainedMapKeys().put( containedEntity, "second" );
			containedEntity.getContainingAsMapKeys().add( entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "containedMapKeys", b2 -> b2
									.field( "includedInMapKeys", "firstValue" )
							)
							.objectField( "containedMapKeys", b2 -> b2
									.field( "includedInMapKeys", "secondValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			ContainedEntity containedEntity = entity1.getContainedMapKeys().keySet().iterator().next();
			containedEntity.getContainingAsMapKeys().clear();
			entity1.getContainedMapKeys().keySet().remove( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "containedMapKeys", b2 -> b2
									.field( "includedInMapKeys", "secondValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_singleAssociation() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setChild( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setParent( containingEntity1 );

			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> { } )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInSingle( "initialValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedSingle", b3 -> b3
											.field( "includedInSingle", "initialValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInSingle( "updatedValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedSingle().getContainingAsSingle().clear();
			containingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedSingle", b3 -> b3
											.field( "includedInSingle", "updatedValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 6 );
			containedEntity.setIncludedInSingle( "outOfScopeValue" );

			ContainingEntity deeplyNestedContainingEntity1 = session.get( ContainingEntity.class, 3 );
			deeplyNestedContainingEntity1.setContainedSingle( containedEntity );
			containedEntity.getContainingAsSingle().add( deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedSingle().getContainingAsSingle().clear();
			containingEntity1.setContainedSingle( null );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> { } )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_singleAssociation_singleValue() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setChild( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setParent( containingEntity1 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 4 );
			containedEntity1.setIncludedInSingle( "initialValue" );
			containingEntity1.setContainedSingle( containedEntity1 );
			containedEntity1.getContainingAsSingle().add( containingEntity1 );

			ContainedEntity containedEntity2 = new ContainedEntity();
			containedEntity2.setId( 5 );
			containedEntity2.setIncludedInSingle( "initialOutOfScopeValue" );
			deeplyNestedContainingEntity.setContainedSingle( containedEntity2 );
			containedEntity2.getContainingAsSingle().add( deeplyNestedContainingEntity );

			session.persist( containedEntity1 );
			session.persist( containedEntity2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedSingle", b3 -> b3
											.field( "includedInSingle", "initialValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setIncludedInSingle( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedSingle", b3 -> b3
											.field( "includedInSingle", "updatedValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 5 );
			containedEntity.setIncludedInSingle( "updatedOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_singleAssociation_elementCollectionValue() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setChild( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setParent( containingEntity1 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 4 );
			containedEntity1.getElementCollectionIncludedInSingle().add( "firstValue" );
			containingEntity1.setContainedSingle( containedEntity1 );
			containedEntity1.getContainingAsSingle().add( containingEntity1 );

			ContainedEntity containedEntity2 = new ContainedEntity();
			containedEntity2.setId( 5 );
			containedEntity2.getElementCollectionIncludedInSingle().add( "firstOutOfScopeValue" );
			deeplyNestedContainingEntity.setContainedSingle( containedEntity2 );
			containedEntity2.getContainingAsSingle().add( deeplyNestedContainingEntity );

			session.persist( containedEntity1 );
			session.persist( containedEntity2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedSingle", b3 -> b3
											.field( "includedInSingle", null )
											.field(
													"elementCollectionIncludedInSingle",
													"firstValue"
											)
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.getElementCollectionIncludedInSingle().add( "secondValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedSingle", b3 -> b3
											.field( "includedInSingle", null )
											.field(
													"elementCollectionIncludedInSingle",
													"firstValue", "secondValue"
											)
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing the values
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setElementCollectionIncludedInSingle( new ArrayList<>( Arrays.asList(
					"newFirstValue", "newSecondValue"
			) ) );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedSingle", b3 -> b3
											.field( "includedInSingle", null )
											.field(
													"elementCollectionIncludedInSingle",
													"newFirstValue", "newSecondValue"
											)
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.getElementCollectionIncludedInSingle().remove( 0 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedSingle", b3 -> b3
											.field( "includedInSingle", null )
											.field(
													"elementCollectionIncludedInSingle",
													"newSecondValue"
											)
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 5 );
			containedEntity.getElementCollectionIncludedInSingle().add( "secondOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test replacing a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 5 );
			containedEntity.setElementCollectionIncludedInSingle( new ArrayList<>( Arrays.asList(
					"newFirstOutOfScopeValue", "newSecondOutOfScopeValue"
			) ) );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_listAssociation() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setChild( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setParent( containingEntity1 );

			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> { } )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInList( "firstValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedList().add( containedEntity );
			containedEntity.getContainingAsList().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedList", b3 -> b3
											.field( "includedInList", "firstValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInList( "secondValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedList().add( containedEntity );
			containedEntity.getContainingAsList().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedList", b3 -> b3
											.field( "includedInList", "firstValue" )
									)
									.objectField( "containedList", b3 -> b3
											.field( "includedInList", "secondValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 6 );
			containedEntity.setIncludedInList( "outOfScopeValue" );

			ContainingEntity deeplyNestedContainingEntity1 = session.get( ContainingEntity.class, 3 );
			deeplyNestedContainingEntity1.getContainedList().add( containedEntity );
			containedEntity.getContainingAsList().add( deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			ContainedEntity containedEntity = containingEntity1.getContainedList().get( 0 );
			containedEntity.getContainingAsList().clear();
			containingEntity1.getContainedList().remove( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedList", b3 -> b3
											.field( "includedInList", "secondValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_listAssociation() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setChild( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setParent( containingEntity1 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 4 );
			containedEntity1.setIncludedInList( "initialValue" );
			containingEntity1.getContainedList().add( containedEntity1 );
			containedEntity1.getContainingAsList().add( containingEntity1 );

			ContainedEntity containedEntity2 = new ContainedEntity();
			containedEntity2.setId( 5 );
			containedEntity2.setIncludedInList( "initialOutOfScopeValue" );
			deeplyNestedContainingEntity.getContainedList().add( containedEntity2 );
			containedEntity2.getContainingAsList().add( deeplyNestedContainingEntity );

			session.persist( containedEntity1 );
			session.persist( containedEntity2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedList", b3 -> b3
											.field( "includedInList", "initialValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setIncludedInList( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedList", b3 -> b3
											.field( "includedInList", "updatedValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 5 );
			containedEntity.setIncludedInList( "updatedOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_mapValuesAssociation() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setChild( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setParent( containingEntity1 );

			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> { } )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInMapValues( "firstValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedMapValues().put( "first", containedEntity );
			containedEntity.getContainingAsMapValues().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedMapValues", b3 -> b3
											.field( "includedInMapValues", "firstValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInMapValues( "secondValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedMapValues().put( "second", containedEntity );
			containedEntity.getContainingAsMapValues().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedMapValues", b3 -> b3
											.field( "includedInMapValues", "firstValue" )
									)
									.objectField( "containedMapValues", b3 -> b3
											.field( "includedInMapValues", "secondValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 6 );
			containedEntity.setIncludedInMapValues( "outOfScopeValue" );

			ContainingEntity deeplyNestedContainingEntity1 = session.get( ContainingEntity.class, 3 );
			deeplyNestedContainingEntity1.getContainedMapValues().put( "outOfScopeKey", containedEntity );
			containedEntity.getContainingAsMapValues().add( deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			ContainedEntity containedEntity = containingEntity1.getContainedMapValues().get( "first" );
			containedEntity.getContainingAsMapValues().clear();
			containingEntity1.getContainedMapValues().values().remove( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedMapValues", b3 -> b3
											.field( "includedInMapValues", "secondValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_mapValuesAssociation() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setChild( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setParent( containingEntity1 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 4 );
			containedEntity1.setIncludedInMapValues( "initialValue" );
			containingEntity1.getContainedMapValues().put( "someKey", containedEntity1 );
			containedEntity1.getContainingAsMapValues().add( containingEntity1 );

			ContainedEntity containedEntity2 = new ContainedEntity();
			containedEntity2.setId( 5 );
			containedEntity2.setIncludedInMapValues( "initialOutOfScopeValue" );
			deeplyNestedContainingEntity.getContainedMapValues().put( "someKey", containedEntity2 );
			containedEntity2.getContainingAsMapValues().add( deeplyNestedContainingEntity );

			session.persist( containedEntity1 );
			session.persist( containedEntity2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedMapValues", b3 -> b3
											.field( "includedInMapValues", "initialValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setIncludedInMapValues( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedMapValues", b3 -> b3
											.field( "includedInMapValues", "updatedValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 5 );
			containedEntity.setIncludedInMapValues( "updatedOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_mapKeysAssociation() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setChild( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setParent( containingEntity1 );

			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> { } )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIncludedInMapKeys( "firstValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedMapKeys().put( containedEntity, "first" );
			containedEntity.getContainingAsMapKeys().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedMapKeys", b3 -> b3
											.field( "includedInMapKeys", "firstValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIncludedInMapKeys( "secondValue" );

			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			containingEntity1.getContainedMapKeys().put( containedEntity, "second" );
			containedEntity.getContainingAsMapKeys().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedMapKeys", b3 -> b3
											.field( "includedInMapKeys", "firstValue" )
									)
									.objectField( "containedMapKeys", b3 -> b3
											.field( "includedInMapKeys", "secondValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 6 );
			containedEntity.setIncludedInMapKeys( "outOfScopeValue" );

			ContainingEntity deeplyNestedContainingEntity1 = session.get( ContainingEntity.class, 3 );
			deeplyNestedContainingEntity1.getContainedMapKeys().put( containedEntity, "outOfScopeValue" );
			containedEntity.getContainingAsMapKeys().add( deeplyNestedContainingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );
			ContainedEntity containedEntity = containingEntity1.getContainedMapKeys().keySet().iterator().next();
			containedEntity.getContainingAsMapKeys().clear();
			containingEntity1.getContainedMapKeys().remove( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedMapKeys", b3 -> b3
											.field( "includedInMapKeys", "secondValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate_mapKeysAssociation() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainingEntity deeplyNestedContainingEntity = new ContainingEntity();
			deeplyNestedContainingEntity.setId( 3 );
			containingEntity1.setChild( deeplyNestedContainingEntity );
			deeplyNestedContainingEntity.setParent( containingEntity1 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 4 );
			containedEntity1.setIncludedInMapKeys( "initialValue" );
			containingEntity1.getContainedMapKeys().put( containedEntity1, "someValue" );
			containedEntity1.getContainingAsMapKeys().add( containingEntity1 );

			ContainedEntity containedEntity2 = new ContainedEntity();
			containedEntity2.setId( 5 );
			containedEntity2.setIncludedInMapKeys( "initialOutOfScopeValue" );
			deeplyNestedContainingEntity.getContainedMapKeys().put( containedEntity2, "someValue" );
			containedEntity2.getContainingAsMapKeys().add( deeplyNestedContainingEntity );

			session.persist( containedEntity1 );
			session.persist( containedEntity2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedMapKeys", b3 -> b3
											.field( "includedInMapKeys", "initialValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setIncludedInMapKeys( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.field( "directField", null )
							.objectField( "child", b2 -> b2
									.objectField( "containedMapKeys", b3 -> b3
											.field( "includedInMapKeys", "updatedValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 5 );
			containedEntity.setIncludedInMapKeys( "updatedOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "containing")
	public static class ContainingEntity {

		@Id
		@DocumentId
		private Integer id;

		@OneToOne
		private ContainingEntity parent;

		@OneToOne(mappedBy = "parent")
		@IndexedEmbedded(includePaths = {
				"containedSingle.includedInSingle",
				"containedSingle.elementCollectionIncludedInSingle",
				"containedList.includedInList",
				"containedMapValues.includedInMapValues",
				"containedMapKeys.includedInMapKeys",
		})
		private ContainingEntity child;

		@ManyToOne
		@IndexedEmbedded(includePaths = { "includedInSingle", "elementCollectionIncludedInSingle" })
		private ContainedEntity containedSingle;

		@ManyToMany
		@JoinTable(name = "indexed_list")
		@IndexedEmbedded(includePaths = "includedInList")
		private List<ContainedEntity> containedList = new ArrayList<>();

		@ManyToMany
		@JoinTable(
				name = "indexed_mapvals",
				joinColumns = @JoinColumn(name = "mapHolder"),
				inverseJoinColumns = @JoinColumn(name = "value")
		)
		@MapKeyColumn(name = "key")
		@IndexedEmbedded(includePaths = "includedInMapValues")
		@OrderBy("id asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		private Map<String, ContainedEntity> containedMapValues = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "indexed_mapkeys",
				joinColumns = @JoinColumn(name = "mapHolder")
		)
		@MapKeyJoinColumn(name = "key")
		@Column(name = "value")
		@OrderBy("key asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		@IndexedEmbedded(
				includePaths = "includedInMapKeys",
				extractors = @ContainerValueExtractorBeanReference( type = MapKeyExtractor.class )
		)
		private Map<ContainedEntity, String> containedMapKeys = new LinkedHashMap<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainingEntity getParent() {
			return parent;
		}

		public void setParent(ContainingEntity parent) {
			this.parent = parent;
		}

		public ContainingEntity getChild() {
			return child;
		}

		public void setChild(ContainingEntity child) {
			this.child = child;
		}

		public ContainedEntity getContainedSingle() {
			return containedSingle;
		}

		public void setContainedSingle(ContainedEntity containedSingle) {
			this.containedSingle = containedSingle;
		}

		public List<ContainedEntity> getContainedList() {
			return containedList;
		}

		public Map<String, ContainedEntity> getContainedMapValues() {
			return containedMapValues;
		}

		public Map<ContainedEntity, String> getContainedMapKeys() {
			return containedMapKeys;
		}
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity extends ContainingEntity {

		static final String INDEX = "IndexedEntity";

		@Basic
		@Field
		private String directField;

		@ElementCollection
		@Field
		private List<String> directElementCollectionField = new ArrayList<>();

		public String getDirectField() {
			return directField;
		}

		public void setDirectField(String directField) {
			this.directField = directField;
		}

		public List<String> getDirectElementCollectionField() {
			return directElementCollectionField;
		}

		public void setDirectElementCollectionField(List<String> directElementCollectionField) {
			this.directElementCollectionField = directElementCollectionField;
		}
	}

	@Entity(name = "contained")
	public static class ContainedEntity {

		@Id
		private Integer id;

		@OneToMany(mappedBy = "containedSingle")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsSingle = new ArrayList<>();

		@ManyToMany(mappedBy = "containedList")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsList = new ArrayList<>();

		@ManyToMany(mappedBy = "containedMapValues")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsMapValues = new ArrayList<>();

		/*
		 * No mappedBy here. The inverse side of associations modeled by a Map key cannot use mappedBy.
		 * If they do, Hibernate assumes that map *values* are the opposite side of the association,
		 * and ends up adding all kind of wrong foreign keys.
		 */
		@ManyToMany
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @PropertyValue(
						propertyName = "containedMapKeys",
						extractors = @ContainerValueExtractorBeanReference(type = MapKeyExtractor.class)
				)
		)
		private List<ContainingEntity> containingAsMapKeys = new ArrayList<>();

		@Basic
		@Field
		private String includedInSingle;

		@Basic
		@Field
		private String includedInList;

		@Basic
		@Field
		private String includedInMapValues;

		@Basic
		@Field
		private String includedInMapKeys;

		@ElementCollection
		@Field
		private List<String> elementCollectionIncludedInSingle = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<ContainingEntity> getContainingAsSingle() {
			return containingAsSingle;
		}

		public List<ContainingEntity> getContainingAsList() {
			return containingAsList;
		}

		public List<ContainingEntity> getContainingAsMapValues() {
			return containingAsMapValues;
		}

		public List<ContainingEntity> getContainingAsMapKeys() {
			return containingAsMapKeys;
		}

		public String getIncludedInSingle() {
			return includedInSingle;
		}

		public void setIncludedInSingle(String includedInSingle) {
			this.includedInSingle = includedInSingle;
		}

		public String getIncludedInList() {
			return includedInList;
		}

		public void setIncludedInList(String includedInList) {
			this.includedInList = includedInList;
		}

		public String getIncludedInMapValues() {
			return includedInMapValues;
		}

		public void setIncludedInMapValues(String includedInMapValues) {
			this.includedInMapValues = includedInMapValues;
		}

		public String getIncludedInMapKeys() {
			return includedInMapKeys;
		}

		public void setIncludedInMapKeys(String includedInMapKeys) {
			this.includedInMapKeys = includedInMapKeys;
		}

		public List<String> getElementCollectionIncludedInSingle() {
			return elementCollectionIncludedInSingle;
		}

		public void setElementCollectionIncludedInSingle(List<String> elementCollectionIncludedInSingle) {
			this.elementCollectionIncludedInSingle = elementCollectionIncludedInSingle;
		}
	}

}
