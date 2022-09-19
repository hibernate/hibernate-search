/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * Test that Hibernate Search correctly unproxies entities before accessing entity fields to resolve entities to reindex,
 * so as to avoid fetching data from a private field on a proxy,
 * which would never work correctly as those private fields are always null on proxies.
 */
@TestForIssue(jiraKey = "HSEARCH-3643")
public class ReindexingResolverProxiedAssociatedEntityIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext, ReusableOrmSetupHolder.DataClearConfig dataClearConfig) {
		backendMock.expectAnySchema( IndexedEntity.NAME );

		setupContext.withAnnotatedTypes(
				IndexedEntity.class,
				ContainedLevel1Entity.class,
				ContainedLevel2Entity.class
		);

		dataClearConfig.clearOrder( IndexedEntity.class, ContainedLevel2Entity.class, ContainedLevel1Entity.class );
	}

	@Test
	public void toOne() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity indexed1 = new IndexedEntity( 1 );

			ContainedLevel1Entity contained1 = new ContainedLevel1Entity( 2 );
			indexed1.setContained( contained1 );
			contained1.setContaining( indexed1 );

			ContainedLevel2Entity contained2 = new ContainedLevel2Entity( 3, "initialValue" );
			contained1.setContainedSingle( contained2 );
			contained2.setContainingSingle( contained1 );

			session.persist( contained2 );
			session.persist( contained1 );
			session.persist( indexed1 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.objectField( "contained", b2 -> b2
									.objectField( "containedSingle", b3 -> b3
											.field( "text", "initialValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			ContainedLevel2Entity contained2 = session.getReference( ContainedLevel2Entity.class, 3 );

			// The contained entity should be a proxy, otherwise the test doesn't make sense
			assertThat( contained2.getContainingSingle() ).isInstanceOf( HibernateProxy.class );

			contained2.setText( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( "1", b -> b
							.objectField( "contained", b2 -> b2
									.objectField( "containedSingle", b3 -> b3
											.field( "text", "updatedValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void toMany() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity indexed1 = new IndexedEntity( 1 );

			ContainedLevel1Entity contained1 = new ContainedLevel1Entity( 2 );
			indexed1.setContained( contained1 );
			contained1.setContaining( indexed1 );

			ContainedLevel2Entity contained2 = new ContainedLevel2Entity( 3, "initialValue" );
			contained1.getContainedList().add( contained2 );
			contained2.getContainingList().add( contained1 );

			session.persist( contained2 );
			session.persist( contained1 );
			session.persist( indexed1 );

			session.persist( contained2 );
			session.persist( contained1 );
			session.persist( indexed1 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "1", b -> b
							.objectField( "contained", b2 -> b2
									.objectField( "containedList", b3 -> b3
											.field( "text", "initialValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();

		setupHolder.runInTransaction( session -> {
			// Create a proxy for contained1, so that the "containingList" list in contained2 is populated with that proxy.
			// The proxy will be initialized, but that's irrelevant to our test.
			@SuppressWarnings("unused") // Keep a reference to the proxy so that it's not garbage collected, which would prevent the above from happening.
			ContainedLevel1Entity contained1 = session.getReference( ContainedLevel1Entity.class, 2 );

			ContainedLevel2Entity contained2 = session.getReference( ContainedLevel2Entity.class, 3 );

			// The contained entity should be a proxy, otherwise the test doesn't make sense
			assertThat( contained2.getContainingList().get( 0 ) ).isInstanceOf( HibernateProxy.class );

			contained2.setText( "updatedValue" );

			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( "1", b -> b
							.objectField( "contained", b2 -> b2
									.objectField( "containedList", b3 -> b3
											.field( "text", "updatedValue" )
									)
							)
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.NAME)
	public static class IndexedEntity {
		public static final String NAME = "IndexedEntity";

		@Id
		private Integer id;

		@OneToOne
		@IndexedEmbedded
		private ContainedLevel1Entity contained;

		protected IndexedEntity() {
			// For ORM
		}

		public IndexedEntity(int id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainedLevel1Entity getContained() {
			return contained;
		}

		public void setContained(
				ContainedLevel1Entity contained) {
			this.contained = contained;
		}
	}

	@Entity(name = ContainedLevel1Entity.NAME)
	@Access(AccessType.FIELD) // This should be the default, but let's be safe: the test only makes sense with this access type
	public static class ContainedLevel1Entity {
		public static final String NAME = "ContainingEntity";

		@Id
		private Integer id;

		// This field is accessed directly (not through the getter), so it requires special handling if the entity is proxified
		@OneToOne(mappedBy = "contained", fetch = FetchType.LAZY)
		private IndexedEntity containing;

		@OneToOne(mappedBy = "containingSingle")
		@IndexedEmbedded
		private ContainedLevel2Entity containedSingle;

		@ManyToMany(mappedBy = "containingList")
		@IndexedEmbedded
		private List<ContainedLevel2Entity> containedList = new ArrayList<>();

		protected ContainedLevel1Entity() {
			// For ORM
		}

		public ContainedLevel1Entity(int id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public IndexedEntity getContaining() {
			return containing;
		}

		public void setContaining(IndexedEntity containing) {
			this.containing = containing;
		}

		public ContainedLevel2Entity getContainedSingle() {
			return containedSingle;
		}

		public void setContainedSingle(ContainedLevel2Entity containedSingle) {
			this.containedSingle = containedSingle;
		}

		@Transient
		public List<ContainedLevel2Entity> getContainedList() {
			return containedList;
		}

		public void setContainedList(List<ContainedLevel2Entity> containedList) {
			this.containedList = containedList;
		}
	}

	@Entity(name = ContainedLevel2Entity.NAME)
	@Access(AccessType.FIELD) // This should be the default, but let's be safe: the test only makes sense with this access type
	public static class ContainedLevel2Entity {
		public static final String NAME = "ContainedEntity";

		@Id
		private Integer id;

		// This field is only used to trigger reindexing, we don't really care about its value
		@GenericField
		private String text;

		@OneToOne(fetch = FetchType.LAZY)
		private ContainedLevel1Entity containingSingle;

		@ManyToMany(fetch = FetchType.LAZY)
		@JoinTable(name = "ContainedE_ContainingE")
		private List<ContainedLevel1Entity> containingList = new ArrayList<>();

		protected ContainedLevel2Entity() {
			// For ORM
		}

		public ContainedLevel2Entity(int id, String text) {
			this.id = id;
			this.text = text;
		}

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

		public ContainedLevel1Entity getContainingSingle() {
			return containingSingle;
		}

		public void setContainingSingle(ContainedLevel1Entity containingSingle) {
			this.containingSingle = containingSingle;
		}

		public List<ContainedLevel1Entity> getContainingList() {
			return containingList;
		}

		public void setContainingList(List<ContainedLevel1Entity> containingList) {
			this.containingList = containingList;
		}
	}
}
