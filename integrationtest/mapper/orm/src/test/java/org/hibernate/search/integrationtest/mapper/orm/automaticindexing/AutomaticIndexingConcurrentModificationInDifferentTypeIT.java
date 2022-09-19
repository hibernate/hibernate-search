/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

public class AutomaticIndexingConcurrentModificationInDifferentTypeIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectSchema( ParentEntity.NAME, b -> b
				.field( "name", String.class )
				.objectField( "child", b2 -> b2
						.field( "name", String.class )
				)
		);
		backendMock.expectSchema( ChildEntity.NAME, b -> b
				.field( "name", String.class )
		);
		backendMock.expectSchema( OtherEntity.NAME, b -> b
				.field( "name", String.class )
		);

		setupContext.withAnnotatedTypes( ParentEntity.class, ChildEntity.class, OtherEntity.class );
	}

	@Before
	public void initData() {
		setupHolder.runInTransaction( session -> {
			ChildEntity entity1 = new ChildEntity();
			entity1.setId( 1 );
			entity1.setName( "edouard" );

			ParentEntity entity2 = new ParentEntity();
			entity2.setId( 2 );
			entity2.setName( "yann" );

			entity2.setChild( entity1 );
			entity1.setParent( entity2 );

			OtherEntity entity3 = new OtherEntity();
			entity3.setId( 3 );
			entity3.setName( "king" );

			session.persist( entity1 );
			session.persist( entity2 );
			session.persist( entity3 );

			backendMock.expectWorks( ParentEntity.NAME )
					.add( String.valueOf( 2 ), b -> b
							.field( "name", "yann" )
							.objectField( "child", b2 -> b2
									.field( "name", "edouard" )
							) );
			backendMock.expectWorks( ChildEntity.NAME )
					.add( String.valueOf( 1 ), b -> b
							.field( "name", "edouard" ) );
			backendMock.expectWorks( OtherEntity.NAME )
					.add( String.valueOf( 3 ), b -> b
							.field( "name", "king" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3857")
	public void updateTriggeringReindexingOfPreviouslyUnknownEntityType() {
		setupHolder.runInTransaction( session -> {
			ChildEntity entity1 = session.getReference( ChildEntity.class, 1 );
			entity1.setName( "updated" );
			// Add another type to the indexing plan so that we're not done iterating over all types
			// when ParentEntity is added to the indexing plan due to the change in the child.
			OtherEntity entity3 = session.getReference( OtherEntity.class, 3 );
			entity3.setName( "updated" );

			backendMock.expectWorks( ParentEntity.NAME )
					.addOrUpdate( String.valueOf( 2 ), b -> b
							.field( "name", "yann" )
							.objectField( "child", b2 -> b2
									.field( "name", "updated" )
							) );
			backendMock.expectWorks( ChildEntity.NAME )
					.addOrUpdate( String.valueOf( 1 ), b -> b
							.field( "name", "updated" ) );
			backendMock.expectWorks( OtherEntity.NAME )
					.addOrUpdate( String.valueOf( 3 ), b -> b
							.field( "name", "updated" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = ParentEntity.NAME)
	@Indexed
	public static class ParentEntity {
		static final String NAME = "Parent";

		@Id
		private Integer id;

		@GenericField
		@Basic
		private String name;

		@IndexedEmbedded
		@OneToOne(mappedBy = "parent")
		private ChildEntity child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public ChildEntity getChild() {
			return child;
		}

		public void setChild(ChildEntity child) {
			this.child = child;
		}
	}

	@Entity(name = ChildEntity.NAME)
	@Indexed
	public static class ChildEntity {
		static final String NAME = "Child";

		@Id
		private Integer id;

		@GenericField
		@Basic
		private String name;

		@OneToOne
		private ParentEntity parent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public ParentEntity getParent() {
			return parent;
		}

		public void setParent(ParentEntity parent) {
			this.parent = parent;
		}
	}

	@Entity(name = OtherEntity.NAME)
	@Indexed
	public static class OtherEntity {
		static final String NAME = "Other";

		@Id
		private Integer id;

		@GenericField
		@Basic
		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
