/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test automatic indexing based on Hibernate ORM entity events
 * when associations that are polymorphic on the original (containing) side are involved.
 */
public class AutomaticIndexingPolymorphicOriginalSideAssociationIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "child", b3 -> b3
						.objectField( "containedSingle", b2 -> b2
								.field( "includedInSingle", String.class )
						)
				)
		);

		sessionFactory = ormSetupHelper.start()
				.setup(
						IndexedEntity.class,
						ContainingEntity.class,
						FirstMiddleContainingEntity.class,
						SecondMiddleContainingEntity.class,
						ContainedEntity.class
				);
		backendMock.verifyExpectationsMet();
	}

	/**
	 * Check that, when resolving entities to reindex,
	 * we go through every possible path on the inverse side of an association
	 * considering the possible types on the entities on the original side.
	 * This is necessary because an indexed entity may defines an abstract getter to another entity,
	 * but define the association differently in different subtypes, in which case there will be multiple inverse
	 * paths for a single getter.
	 */
	@Test
	public void inversePathDependsOnConcreteType() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity indexedEntity1 = new IndexedEntity();
			indexedEntity1.setId( 1 );

			/*
			 * The automatic indexing process should access the property "containedSingleFromFirst"
			 * when a change happens in the contained entity.
			 * If it doesn't, indexedEntity1 will not be reindexed as required.
			 */
			FirstMiddleContainingEntity middleContainingEntity1 = new FirstMiddleContainingEntity();
			middleContainingEntity1.setId( 2 );
			indexedEntity1.setChild( middleContainingEntity1 );
			middleContainingEntity1.setParent( indexedEntity1 );

			IndexedEntity indexedEntity2 = new IndexedEntity();
			indexedEntity2.setId( 3 );

			/*
			 * The automatic indexing process should access the property "containedSingleFromSecond"
			 * when a change happens in the contained entity.
			 * If it doesn't, indexedEntity2 will not be reindexed as required.
			 */
			SecondMiddleContainingEntity middleContainingEntity2 = new SecondMiddleContainingEntity();
			middleContainingEntity2.setId( 4 );
			indexedEntity2.setChild( middleContainingEntity2 );
			middleContainingEntity2.setParent( indexedEntity2 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 5 );
			containedEntity1.setIncludedInSingle( "initialValue" );
			middleContainingEntity1.setContainedSingle( containedEntity1 );
			middleContainingEntity2.setContainedSingle( containedEntity1 );
			containedEntity1.getContainingAsSingleFromFirst().add( middleContainingEntity1 );
			containedEntity1.getContainingAsSingleFromSecond().add( middleContainingEntity2 );

			session.persist( containedEntity1 );
			session.persist( middleContainingEntity1 );
			session.persist( middleContainingEntity2 );
			session.persist( indexedEntity1 );
			session.persist( indexedEntity2 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedSingle", b3 -> b3
											.field( "includedInSingle", "initialValue" )
									)
							)
					)
					.add( "3", b -> b
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
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 5 );
			containedEntity.setIncludedInSingle( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.update( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedSingle", b3 -> b3
											.field( "includedInSingle", "updatedValue" )
									)
							)
					)
					.update( "3", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedSingle", b3 -> b3
											.field( "includedInSingle", "updatedValue" )
									)
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
		private Integer id;

		@OneToOne(mappedBy = "parent")
		@IndexedEmbedded
		private ContainingEntity child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainingEntity getChild() {
			return child;
		}

		public void setChild(ContainingEntity child) {
			this.child = child;
		}
	}

	@Entity(name = "containing")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS) // Necessary because subtypes declare columns with identical names
	public abstract static class ContainingEntity {
		@Id
		private Integer id;

		@OneToOne
		private IndexedEntity parent;

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

		@IndexedEmbedded
		public abstract ContainedEntity getContainedSingle();

		public abstract void setContainedSingle(ContainedEntity containedSingle);
	}

	@Entity(name = "firstmiddle")
	public static class FirstMiddleContainingEntity extends ContainingEntity {
		@ManyToOne
		private ContainedEntity containedSingle;

		@Override
		public ContainedEntity getContainedSingle() {
			return containedSingle;
		}

		@Override
		public void setContainedSingle(ContainedEntity containedSingle) {
			this.containedSingle = containedSingle;
		}
	}

	@Entity(name = "secondmiddle")
	public static class SecondMiddleContainingEntity extends ContainingEntity {
		/*
		 * This association must be implemented in this type and in FirstMiddleContainingEntity, not in a parent type.
		 * We want to test that "abstract" associations (associations whose getters are declared in an abstract type,
		 * but whose Hibernate ORM metadata are only provided in subtypes) are supported.
		 */
		@ManyToOne
		private ContainedEntity containedSingle;

		@Override
		public ContainedEntity getContainedSingle() {
			return containedSingle;
		}

		@Override
		public void setContainedSingle(ContainedEntity containedSingle) {
			this.containedSingle = containedSingle;
		}
	}

	@Entity(name = "contained")
	public static class ContainedEntity {

		@Id
		private Integer id;

		@OneToMany(mappedBy = "containedSingle")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<FirstMiddleContainingEntity> containingAsSingleFromFirst = new ArrayList<>();

		@OneToMany(mappedBy = "containedSingle")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<SecondMiddleContainingEntity> containingAsSingleFromSecond = new ArrayList<>();

		@Basic
		@GenericField
		private String includedInSingle;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<FirstMiddleContainingEntity> getContainingAsSingleFromFirst() {
			return containingAsSingleFromFirst;
		}

		public List<SecondMiddleContainingEntity> getContainingAsSingleFromSecond() {
			return containingAsSingleFromSecond;
		}

		public String getIncludedInSingle() {
			return includedInSingle;
		}

		public void setIncludedInSingle(String includedInSingle) {
			this.includedInSingle = includedInSingle;
		}
	}

}
