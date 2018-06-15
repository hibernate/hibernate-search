/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm;

import java.util.ArrayList;
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
import javax.persistence.MapKeyJoinColumn;
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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.service.ServiceRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test automatic indexing based on Hibernate ORM entity events.
 *
 * This test only checks updates involving a multi-valued, Map keys association.
 * Other tests in the same package check more basic, direct updates or updates involving different associations.
 */
public class OrmAutomaticIndexingMapKeysAssociationIT {

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
				.objectField( "containedMapKeys", b2 -> b2
						.field( "indexedField", String.class )
				)
				.objectField( "child", b3 -> b3
						.objectField( "containedMapKeys", b2 -> b2
								.field( "indexedField", String.class )
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
	public void directAssociationUpdate_indexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> { } )
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 2 );
			containedEntity.setIndexedField( "firstValue" );

			entity1.getContainedMapKeys().put( containedEntity, "first" );
			containedEntity.getContainingAsMapKeys().add( entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "containedMapKeys", b2 -> b2
									.field( "indexedField", "firstValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );
			containedEntity.setIndexedField( "secondValue" );

			entity1.getContainedMapKeys().put( containedEntity, "second" );
			containedEntity.getContainingAsMapKeys().add( entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "containedMapKeys", b2 -> b2
									.field( "indexedField", "firstValue" )
							)
							.objectField( "containedMapKeys", b2 -> b2
									.field( "indexedField", "secondValue" )
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
							.objectField( "containedMapKeys", b2 -> b2
									.field( "indexedField", "secondValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an IndexedEmbedded association in an indexed entity
	 * does trigger reindexing of the entity.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void directAssociationReplace_indexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 2 );
			containedEntity.setIndexedField( "firstValue" );
			entity1.getContainedMapKeys().put( containedEntity, "first" );
			containedEntity.getContainingAsMapKeys().add( entity1 );

			session.persist( containedEntity );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "containedMapKeys", b2 -> b2
									.field( "indexedField", "firstValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );
			containedEntity.setIndexedField( "secondValue" );

			Map<ContainedEntity, String> newAssociation = new LinkedHashMap<>(
					entity1.getContainedMapKeys()
			);
			newAssociation.put( containedEntity, "second" );
			entity1.setContainedMapKeys( newAssociation );
			containedEntity.getContainingAsMapKeys().add( entity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "containedMapKeys", b2 -> b2
									.field( "indexedField", "firstValue" )
							)
							.objectField( "containedMapKeys", b2 -> b2
									.field( "indexedField", "secondValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void directAssociationUpdate_nonIndexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> { } )
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 2 );
			containedEntity.setIndexedField( "firstValue" );

			entity1.getContainedNonIndexedEmbeddedMapKeys().put( containedEntity, "first" );
			containedEntity.getContainingAsNonIndexedEmbeddedMapKeys().add( entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a second value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );
			containedEntity.setIndexedField( "secondValue" );

			entity1.getContainedNonIndexedEmbeddedMapKeys().put( containedEntity, "second" );
			containedEntity.getContainingAsNonIndexedEmbeddedMapKeys().add( entity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainedEntity containedEntity = entity1.getContainedNonIndexedEmbeddedMapKeys().keySet().iterator().next();

			containedEntity.getContainingAsNonIndexedEmbeddedMapKeys().clear();
			entity1.getContainedNonIndexedEmbeddedMapKeys().keySet().remove( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing a non-IndexedEmbedded association in an entity
	 * whose other properties are indexed
	 * does not trigger reindexing of the entity.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3204")
	public void directAssociationReplace_nonIndexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 2 );
			containedEntity.setIndexedField( "firstValue" );
			entity1.getContainedNonIndexedEmbeddedMapKeys().put( containedEntity, "first" );
			containedEntity.getContainingAsNonIndexedEmbeddedMapKeys().add( entity1 );

			session.persist( containedEntity );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> { } )
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );
			containedEntity.setIndexedField( "secondValue" );

			Map<ContainedEntity, String> newAssociation = new LinkedHashMap<>(
					entity1.getContainedNonIndexedEmbeddedMapKeys()
			);
			newAssociation.put( containedEntity, "second" );
			entity1.setContainedNonIndexedEmbeddedMapKeys( newAssociation );
			containedEntity.getContainingAsNonIndexedEmbeddedMapKeys().add( entity1 );

			session.persist( containedEntity );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> { } )
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_indexedEmbedded() {
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
							.objectField( "child", b2 -> { } )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIndexedField( "firstValue" );

			containingEntity1.getContainedMapKeys().put( containedEntity, "first" );
			containedEntity.getContainingAsMapKeys().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedMapKeys", b3 -> b3
											.field( "indexedField", "firstValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIndexedField( "secondValue" );

			containingEntity1.getContainedMapKeys().put( containedEntity, "second" );
			containedEntity.getContainingAsMapKeys().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedMapKeys", b3 -> b3
											.field( "indexedField", "firstValue" )
									)
									.objectField( "containedMapKeys", b3 -> b3
											.field( "indexedField", "secondValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity deeplyNestedContainingEntity1 = session.get( ContainingEntity.class, 3 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 6 );
			containedEntity.setIndexedField( "outOfScopeValue" );

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
							.objectField( "child", b2 -> b2
									.objectField( "containedMapKeys", b3 -> b3
											.field( "indexedField", "secondValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing an IndexedEmbedded association in an entity
	 * that is IndexedEmbedded in an indexed entity
	 * does trigger reindexing of the indexed entity.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3199")
	public void indirectAssociationReplace_indexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );
			containedEntity.setIndexedField( "firstValue" );
			containingEntity1.getContainedMapKeys().put( containedEntity, "first" );
			containedEntity.getContainingAsMapKeys().add( containingEntity1 );

			session.persist( containedEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedMapKeys", b3 -> b3
											.field( "indexedField", "firstValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIndexedField( "secondValue" );

			Map<ContainedEntity, String> newAssociation = new LinkedHashMap<>(
					containingEntity1.getContainedMapKeys()
			);
			newAssociation.put( containedEntity, "second" );
			containingEntity1.setContainedMapKeys( newAssociation );
			containedEntity.getContainingAsMapKeys().add( containingEntity1 );

			session.persist( containedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedMapKeys", b3 -> b3
											.field( "indexedField", "firstValue" )
									)
									.objectField( "containedMapKeys", b3 -> b3
											.field( "indexedField", "secondValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectAssociationUpdate_nonIndexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test adding a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIndexedField( "firstValue" );

			containingEntity1.getContainedNonIndexedEmbeddedMapKeys().put( containedEntity, "first" );
			containedEntity.getContainingAsNonIndexedEmbeddedMapKeys().add( containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test adding another value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 5 );
			containedEntity.setIndexedField( "secondValue" );

			containingEntity1.getContainedNonIndexedEmbeddedMapKeys().put( containedEntity, "second" );
			containedEntity.getContainingAsNonIndexedEmbeddedMapKeys().add( containingEntity1 );

			session.persist( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test removing a value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			ContainedEntity containedEntity = containingEntity1.getContainedNonIndexedEmbeddedMapKeys()
					.keySet().iterator().next();

			containedEntity.getContainingAsNonIndexedEmbeddedMapKeys().clear();
			containingEntity1.getContainedNonIndexedEmbeddedMapKeys().remove( containedEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Test that replacing a non-IndexedEmbedded association in an entity
	 * whose properties are otherwise used in an IndexedEmbedded from an indexed entity
	 * does not trigger reindexing of the indexed entity.
	 * <p>
	 * We need dedicated tests for this because Hibernate ORM does not handle
	 * replaced collections the same way as it does updated collections.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3204")
	public void indirectAssociationReplace_nonIndexedEmbedded() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );

			ContainingEntity containingEntity1 = new ContainingEntity();
			containingEntity1.setId( 2 );
			entity1.setChild( containingEntity1 );
			containingEntity1.setParent( entity1 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 3 );
			containedEntity.setIndexedField( "firstValue" );
			containingEntity1.getContainedNonIndexedEmbeddedMapKeys().put( containedEntity, "first" );
			containedEntity.getContainingAsNonIndexedEmbeddedMapKeys().add( containingEntity1 );

			session.persist( containedEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> { } )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainingEntity containingEntity1 = session.get( ContainingEntity.class, 2 );

			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setId( 4 );
			containedEntity.setIndexedField( "secondValue" );

			Map<ContainedEntity, String> newAssociation = new LinkedHashMap<>(
					containingEntity1.getContainedNonIndexedEmbeddedMapKeys()
			);
			newAssociation.put( containedEntity, "second" );
			containingEntity1.setContainedNonIndexedEmbeddedMapKeys( newAssociation );
			containedEntity.getContainingAsNonIndexedEmbeddedMapKeys().add( containingEntity1 );

			session.persist( containedEntity );

			// TODO HSEARCH-3204: remove the statement below to not expect any work
			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "child", b2 -> { } )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indirectValueUpdate() {
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
			containedEntity1.setIndexedField( "initialValue" );
			containingEntity1.getContainedMapKeys().put( containedEntity1, "someValue" );
			containedEntity1.getContainingAsMapKeys().add( containingEntity1 );

			ContainedEntity containedEntity2 = new ContainedEntity();
			containedEntity2.setId( 5 );
			containedEntity2.setIndexedField( "initialOutOfScopeValue" );
			deeplyNestedContainingEntity.getContainedMapKeys().put( containedEntity2, "someValue" );
			containedEntity2.getContainingAsMapKeys().add( deeplyNestedContainingEntity );

			session.persist( containedEntity1 );
			session.persist( containedEntity2 );
			session.persist( deeplyNestedContainingEntity );
			session.persist( containingEntity1 );
			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedMapKeys", b3 -> b3
											.field( "indexedField", "initialValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 4 );
			containedEntity.setIndexedField( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedMapKeys", b3 -> b3
											.field( "indexedField", "updatedValue" )
									)
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating a value that is too deeply nested to matter (it's out of the IndexedEmbedded scope)
		OrmUtils.withinTransaction( sessionFactory, session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 5 );
			containedEntity.setIndexedField( "updatedOutOfScopeValue" );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "containing")
	public static class ContainingEntity {

		@Id
		private Integer id;

		@OneToOne
		private ContainingEntity parent;

		@OneToOne(mappedBy = "parent")
		@IndexedEmbedded(includePaths = {
				"containedMapKeys.indexedField"
		})
		private ContainingEntity child;

		@ElementCollection
		@JoinTable(
				name = "indexed_mapkeys",
				joinColumns = @JoinColumn(name = "mapHolder")
		)
		@MapKeyJoinColumn(name = "key")
		@Column(name = "value")
		@OrderBy("key asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		@IndexedEmbedded(
				includePaths = "indexedField",
				extractors = @ContainerValueExtractorBeanReference( type = MapKeyExtractor.class )
		)
		private Map<ContainedEntity, String> containedMapKeys = new LinkedHashMap<>();

		@ElementCollection
		@JoinTable(
				name = "indexed_nonIndexedEmbeddedMapkeys",
				joinColumns = @JoinColumn(name = "mapHolder")
		)
		@MapKeyJoinColumn(name = "key")
		@Column(name = "value")
		@OrderBy("key asc") // Forces Hibernate ORM to use a LinkedHashMap; we make sure to insert entries in the correct order
		private Map<ContainedEntity, String> containedNonIndexedEmbeddedMapKeys = new LinkedHashMap<>();

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

		public Map<ContainedEntity, String> getContainedMapKeys() {
			return containedMapKeys;
		}

		public void setContainedMapKeys(Map<ContainedEntity, String> containedMapKeys) {
			this.containedMapKeys = containedMapKeys;
		}

		public Map<ContainedEntity, String> getContainedNonIndexedEmbeddedMapKeys() {
			return containedNonIndexedEmbeddedMapKeys;
		}

		public void setContainedNonIndexedEmbeddedMapKeys(
				Map<ContainedEntity, String> containedNonIndexedEmbeddedMapKeys) {
			this.containedNonIndexedEmbeddedMapKeys = containedNonIndexedEmbeddedMapKeys;
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

		/*
		 * No mappedBy here. The inverse side of associations modeled by a Map key cannot use mappedBy.
		 * If they do, Hibernate assumes that map *values* are the opposite side of the association,
		 * and ends up adding all kind of wrong foreign keys.
		 */
		@ManyToMany
		@JoinTable(name = "contained_mapHolder")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inversePath = @PropertyValue(
						propertyName = "containedMapKeys",
						extractors = @ContainerValueExtractorBeanReference(type = MapKeyExtractor.class)
				)
		)
		private List<ContainingEntity> containingAsMapKeys = new ArrayList<>();

		/*
		 * No mappedBy here, same reasons as above.
		 */
		@ManyToMany
		@JoinTable(name = "contained_nonIndexedMapHolder")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<ContainingEntity> containingAsNonIndexedEmbeddedMapKeys = new ArrayList<>();

		@Basic
		@Field
		private String indexedField;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<ContainingEntity> getContainingAsMapKeys() {
			return containingAsMapKeys;
		}

		public List<ContainingEntity> getContainingAsNonIndexedEmbeddedMapKeys() {
			return containingAsNonIndexedEmbeddedMapKeys;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}
	}

}
