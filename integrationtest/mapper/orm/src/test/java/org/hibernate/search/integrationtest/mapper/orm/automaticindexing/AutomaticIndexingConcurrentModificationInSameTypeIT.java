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

public class AutomaticIndexingConcurrentModificationInSameTypeIT {

	@ClassRule
	public static BackendMock backendMock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder = ReusableOrmSetupHolder.withBackendMock( backendMock );

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "firstName", String.class )
				.field( "name", String.class )
				.objectField( "child", b2 -> b2
						.field( "firstName", String.class )
						.field( "name", String.class )
				)
		);

		setupContext.withAnnotatedTypes( IndexedEntity.class );
	}

	@Before
	public void initData() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setName( "edouard" );
			entity1.setFirstName( "zobocare" );

			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setName( "yann" );
			entity2.setFirstName( "bonduel" );

			IndexedEntity entity3 = new IndexedEntity();
			entity3.setId( 3 );
			entity3.setName( "antoine" );
			entity3.setFirstName( "owl" );

			entity2.setChild( entity1 );
			entity1.setParent( entity2 );

			session.persist( entity1 );
			session.persist( entity2 );
			session.persist( entity3 );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( String.valueOf( 1 ), b -> b
							.field( "name", "edouard" )
							.field( "firstName", "zobocare" ) )
					.add( String.valueOf( 2 ), b -> b
							.field( "name", "yann" )
							.field( "firstName", "bonduel" )
							.objectField( "child", b2 -> b2
									.field( "name", "edouard" )
									.field( "firstName", "zobocare" ) ) )
					.add( String.valueOf( 3 ), b -> b
							.field( "name", "antoine" )
							.field( "firstName", "owl" ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3857")
	public void updateTriggeringReindexingOfPreviouslyUnknownEntity() {
		setupHolder.runInTransaction( session -> {
			IndexedEntity entity1 = session.getReference( IndexedEntity.class, 1 );
			entity1.setName( "updated" );
			// Add another entity to the indexing plan so that we're not done iterating over all entities
			// when entity2 is added to the indexing plan due to the change in the child.
			IndexedEntity entity3 = session.getReference( IndexedEntity.class, 3 );
			entity3.setName( "updated" );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.addOrUpdate( String.valueOf( 1 ), b -> b
							.field( "name", "updated" )
							.field( "firstName", "zobocare" ) )
					.addOrUpdate( String.valueOf( 2 ), b -> b
							.field( "name", "yann" )
							.field( "firstName", "bonduel" )
							.objectField( "child", b2 -> b2
									.field( "name", "updated" )
									.field( "firstName", "zobocare" ) ) )
					.addOrUpdate( String.valueOf( 3 ), b -> b
							.field( "name", "updated" )
							.field( "firstName", "owl" ) );
		} );

		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {
		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@GenericField
		@Basic
		private String firstName;

		@GenericField
		@Basic
		private String name;

		@OneToOne
		private IndexedEntity parent;

		@IndexedEmbedded(includeDepth = 1)
		@OneToOne(mappedBy = "parent")
		private IndexedEntity child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public IndexedEntity getParent() {
			return parent;
		}

		public void setParent(IndexedEntity parent) {
			this.parent = parent;
		}

		public IndexedEntity getChild() {
			return child;
		}

		public void setChild(IndexedEntity child) {
			this.child = child;
		}
	}
}
