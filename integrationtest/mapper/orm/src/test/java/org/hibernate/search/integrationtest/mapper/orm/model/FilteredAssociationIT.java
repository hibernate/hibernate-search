/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Rule;
import org.junit.Test;

/**
 * Check that we can use @IndexedEmbedded on an association "filtered" from another.
 */
public class FilteredAssociationIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	public void test() {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.objectField( "contained", b2 -> b2
						.multiValued( true )
						.field( "text", String.class, b3 -> {
						} ) ) );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.setup( IndexedEntity.class, ContainedEntity.class );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity containing1 = new IndexedEntity();
			containing1.setId( 0 );
			ContainedEntity contained1 = new ContainedEntity();
			contained1.setId( 1 );
			contained1.setText( "theText1" );
			contained1.setStatus( Status.ACTIVE );
			ContainedEntity contained2 = new ContainedEntity();
			contained2.setId( 2 );
			contained2.setText( "theText2" );
			contained2.setStatus( Status.ACTIVE );

			containing1.getContained().add( contained1 );
			contained1.setContaining( containing1 );
			containing1.getContained().add( contained2 );
			contained2.setContaining( containing1 );

			session.persist( containing1 );
			session.persist( contained1 );
			session.persist( contained2 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "0", b -> b
							.objectField( "contained", b2 -> b2
									.field( "text", "theText1" ) )
							.objectField( "contained", b2 -> b2
									.field( "text", "theText2" ) ) );
		} );
		backendMock.verifyExpectationsMet();

		// Verify that changing the contained text leads to reindexing
		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity contained1 = session.find( ContainedEntity.class, 1 );
			contained1.setText( "theText1_updated" );

			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( "0", b -> b
							.objectField( "contained", b2 -> b2
									.field( "text", "theText1_updated" ) )
							.objectField( "contained", b2 -> b2
									.field( "text", "theText2" ) ) );
		} );
		backendMock.verifyExpectationsMet();

		// Verify that changing the contained status leads to reindexing
		with( sessionFactory ).runInTransaction( session -> {
			ContainedEntity contained2 = session.find( ContainedEntity.class, 2 );
			contained2.setStatus( Status.DELETED );

			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( "0", b -> b
							.objectField( "contained", b2 -> b2
									.field( "text", "theText1_updated" ) ) );
		} );
		backendMock.verifyExpectationsMet();

		// Verify that adding a contained entity leads to reindexing
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity containing1 = session.find( IndexedEntity.class, 0 );
			ContainedEntity contained3 = new ContainedEntity();
			contained3.setId( 3 );
			contained3.setText( "theText3" );
			contained3.setStatus( Status.ACTIVE );

			containing1.getContained().add( contained3 );
			contained3.setContaining( containing1 );

			session.persist( contained3 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.addOrUpdate( "0", b -> b
							.objectField( "contained", b2 -> b2
									.field( "text", "theText1_updated" ) )
							.objectField( "contained", b2 -> b2
									.field( "text", "theText3" ) ) );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static final class IndexedEntity {
		static final String NAME = "indexed";

		@Id
		private Integer id;

		@OneToMany(mappedBy = "containing")
		@OrderColumn
		private List<ContainedEntity> contained = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<ContainedEntity> getContained() {
			return contained;
		}

		@IndexedEmbedded(name = "contained")
		@IndexingDependency(derivedFrom = {
				@ObjectPath({@PropertyValue(propertyName = "contained"), @PropertyValue(propertyName = "status")})
		})
		@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "containing")))
		public List<ContainedEntity> getContainedNotDeleted() {
			return contained.stream().filter( c -> c.getStatus() != Status.DELETED )
					.collect( Collectors.toList() );
		}

	}

	@Entity(name = ContainedEntity.NAME)
	public static final class ContainedEntity {
		static final String NAME = "contained";

		@Id
		private Integer id;

		@GenericField
		private String text;

		private Status status;

		@ManyToOne
		private IndexedEntity containing;

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

		public Status getStatus() {
			return status;
		}

		public void setStatus(Status status) {
			this.status = status;
		}

		public IndexedEntity getContaining() {
			return containing;
		}

		public void setContaining(
				IndexedEntity containing) {
			this.containing = containing;
		}
	}

	private enum Status {
		ACTIVE,
		DELETED;
	}
}
