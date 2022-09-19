/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.association;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Transient;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * Test automatic indexing based on Hibernate ORM entity events when polymorphic associations using generics are involved.
 */
public class AutomaticIndexingGenericPolymorphicAssociationIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext, ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "child", b3 -> b3
						.objectField( "containedSingle", b2 -> b2
								.field( "includedInSingle", String.class )
						)
				)
		);

		setupContext.withAnnotatedTypes(
				IndexedEntity.class,
				ContainingEntity.class,
				MiddleContainingEntity.class,
				UnrelatedContainingEntity.class,
				ContainedEntity.class
		);

		dataClearConfig.clearOrder( IndexedEntity.class, ContainingEntity.class, MiddleContainingEntity.class,
				UnrelatedContainingEntity.class, ContainedEntity.class );
	}

	@Test
	public void inversePathHandlesGenericTypes() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity indexedEntity = new IndexedEntity();
			indexedEntity.setId( 1 );

			MiddleContainingEntity middleContainingEntity = new MiddleContainingEntity();
			middleContainingEntity.setId( 2 );
			indexedEntity.setChild( middleContainingEntity );
			middleContainingEntity.setParent( indexedEntity );

			/*
			 * The automatic reindexing process should detect that the containing entity
			 * is a MiddleContainingEntity and thus has a parent property that can
			 * be used to get back to the indexed entity.
			 */
			ContainedEntity<MiddleContainingEntity> containedEntity1 = new ContainedEntity<>();
			containedEntity1.setId( 3 );
			containedEntity1.setIncludedInSingle( "initialValue" );
			middleContainingEntity.setContainedSingle( containedEntity1 );
			containedEntity1.getContainingAsSingle().add( middleContainingEntity );

			session.persist( containedEntity1 );
			session.persist( middleContainingEntity );
			session.persist( indexedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedSingle", b3 -> b3
											.field( "includedInSingle", "initialValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		setupHolder.runInTransaction( session -> {
			@SuppressWarnings("unchecked")
			ContainedEntity<MiddleContainingEntity> containedEntity = session.get( ContainedEntity.class, 3 );
			containedEntity.setIncludedInSingle( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "child", b2 -> b2
									.objectField( "containedSingle", b3 -> b3
											.field( "includedInSingle", "updatedValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void inversePathIgnoresUnrelatedTypes() {
		setupHolder.runInTransaction( session -> {
			UnrelatedContainingEntity unrelatedContainingEntity = new UnrelatedContainingEntity();
			unrelatedContainingEntity.setId( 1 );

			/*
			 * The automatic reindexing process should detect that the containing entity
			 * is a UnrelatedContainingEntity and thus doesn't have any parent property that can
			 * be used to get back to the indexed entity.
			 */
			ContainedEntity<UnrelatedContainingEntity> containedEntity1 = new ContainedEntity<>();
			containedEntity1.setId( 2 );
			containedEntity1.setIncludedInSingle( "initialValue" );
			unrelatedContainingEntity.setContainedSingle( containedEntity1 );
			containedEntity1.getContainingAsSingle().add( unrelatedContainingEntity );

			session.persist( containedEntity1 );
			session.persist( unrelatedContainingEntity );

			// Do not expect any work
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		setupHolder.runInTransaction( session -> {
			@SuppressWarnings("unchecked")
			ContainedEntity<UnrelatedContainingEntity> containedEntity = session.get( ContainedEntity.class, 2 );
			containedEntity.setIncludedInSingle( "updatedValue" );

			// Do not expect any work
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
		private MiddleContainingEntity child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public MiddleContainingEntity getChild() {
			return child;
		}

		public void setChild(MiddleContainingEntity child) {
			this.child = child;
		}
	}

	@Entity(name = "containing")
	public static class ContainingEntity<S extends ContainingEntity<S>> {
		@Id
		private Integer id;

		@ManyToOne(targetEntity = ContainedEntity.class)
		@IndexedEmbedded
		private ContainedEntity<S> containedSingle;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainedEntity<S> getContainedSingle() {
			return containedSingle;
		}

		public void setContainedSingle(ContainedEntity<S> containedSingle) {
			this.containedSingle = containedSingle;
		}
	}

	@Entity(name = "middle")
	public static class MiddleContainingEntity extends ContainingEntity<MiddleContainingEntity> {
		@OneToOne
		private IndexedEntity parent;

		public IndexedEntity getParent() {
			return parent;
		}

		public void setParent(IndexedEntity parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "unrelated")
	public static class UnrelatedContainingEntity extends ContainingEntity<UnrelatedContainingEntity> {
		@Transient
		public IndexedEntity getParent() {
			fail( "This method should never have been called" );
			return null; // Dead code
		}
	}

	@Entity(name = "contained")
	public static class ContainedEntity<C extends ContainingEntity<C>> {

		@Id
		private Integer id;

		@OneToMany(mappedBy = "containedSingle", targetEntity = ContainingEntity.class)
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<C> containingAsSingle = new ArrayList<>();

		@Basic
		@GenericField
		private String includedInSingle;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<C> getContainingAsSingle() {
			return containingAsSingle;
		}

		public String getIncludedInSingle() {
			return includedInSingle;
		}

		public void setIncludedInSingle(String includedInSingle) {
			this.includedInSingle = includedInSingle;
		}
	}

}
