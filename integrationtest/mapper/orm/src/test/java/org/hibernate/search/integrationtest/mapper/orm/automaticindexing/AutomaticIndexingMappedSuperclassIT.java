/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

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
 * Test automatic indexing based on Hibernate ORM entity events when an association is defined in a MappedSuperclass.
 */
public class AutomaticIndexingMappedSuperclassIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.objectField( "containedSingle", b2 -> b2
						.field( "includedInSingle", String.class )
				)
		);

		setupContext.withAnnotatedTypes(
				IndexedEntityMappedSuperclass.class,
				IndexedEntity.class,
				ContainedEntity.class
		);
	}

	@Test
	public void inversePathHandlesMappedSuperclassDefinedAssociations() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity indexedEntity = new IndexedEntity();
			indexedEntity.setId( 1 );

			ContainedEntity containedEntity1 = new ContainedEntity();
			containedEntity1.setId( 3 );
			containedEntity1.setIncludedInSingle( "initialValue" );
			indexedEntity.setContainedSingle( containedEntity1 );
			containedEntity1.getContainingAsSingle().add( indexedEntity );

			session.persist( containedEntity1 );
			session.persist( indexedEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.objectField( "containedSingle", b3 -> b3
									.field( "includedInSingle", "initialValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value
		setupHolder.runInTransaction( session -> {
			ContainedEntity containedEntity = session.get( ContainedEntity.class, 3 );
			containedEntity.setIncludedInSingle( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.objectField( "containedSingle", b3 -> b3
									.field( "includedInSingle", "updatedValue" )
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@MappedSuperclass
	public static class IndexedEntityMappedSuperclass {
		@ManyToOne
		@IndexedEmbedded
		private ContainedEntity containedSingle;

		public ContainedEntity getContainedSingle() {
			return containedSingle;
		}

		public void setContainedSingle(ContainedEntity containedSingle) {
			this.containedSingle = containedSingle;
		}
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity extends IndexedEntityMappedSuperclass {
		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

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

		@OneToMany(mappedBy = "containedSingle")
		@OrderBy("id asc") // Make sure the iteration order is predictable
		private List<IndexedEntity> containingAsSingle = new ArrayList<>();

		@Basic
		@GenericField
		private String includedInSingle;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<IndexedEntity> getContainingAsSingle() {
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
