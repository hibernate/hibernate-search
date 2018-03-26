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
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.OneToMany;
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
				.objectField( "containedSingle", b2 -> b2
						.field( "includedInSingle", String.class )
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

			session.persist( entity1 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "directField", entity1.getDirectField() )
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
	public void directAssociationUpdate_single() {
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
	public void directAssociationUpdate_list() {
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
	public void directAssociationUpdate_mapValues() {
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
	public void directAssociationUpdate_mapKeys() {
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


	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
		@DocumentId
		private Integer id;

		@Basic
		@Field
		private String directField;

		@ManyToOne
		@IndexedEmbedded(includePaths = "includedInSingle")
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

		public String getDirectField() {
			return directField;
		}

		public void setDirectField(String directField) {
			this.directField = directField;
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

	@Entity(name = "contained")
	public static class ContainedEntity {

		@Id
		@DocumentId
		private Integer id;

		@OneToMany(mappedBy = "containedSingle")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<IndexedEntity> containingAsSingle = new ArrayList<>();

		@ManyToMany(mappedBy = "containedList")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<IndexedEntity> containingAsList = new ArrayList<>();

		@ManyToMany(mappedBy = "containedMapValues")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<IndexedEntity> containingAsMapValues = new ArrayList<>();

		/*
		 * No mappedBy here. The inverse side of associations modeled by a Map key cannot use mappedBy.
		 * If they do, Hibernate assumes that map *values* are the opposite side of the association,
		 * and ends up adding all kind of wrong foreign keys.
		 */
		@ManyToMany
		@OrderBy("id asc") // Make sure the iteration order is predictable
		@AssociationInverseSide(
				inverseProperty = "containedMapKeys",
				inverseExtractors = @ContainerValueExtractorBeanReference(type = MapKeyExtractor.class)
		)
		private List<IndexedEntity> containingAsMapKeys = new ArrayList<>();

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

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<IndexedEntity> getContainingAsSingle() {
			return containingAsSingle;
		}

		public List<IndexedEntity> getContainingAsList() {
			return containingAsList;
		}

		public List<IndexedEntity> getContainingAsMapValues() {
			return containingAsMapValues;
		}

		public List<IndexedEntity> getContainingAsMapKeys() {
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
	}

}
