/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Check that we correctly populate the metamodel when ORM generates a {@link org.hibernate.mapping.Backref}
 * or {@link org.hibernate.mapping.IndexBackref} property,
 * which happens in particular when there is a one-to-many
 * whose referenced column is not the entity ID.
 */
@TestForIssue(jiraKey = "HSEARCH-4156")
public class BackRefPropertyIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	public void test() {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.objectField( "contained", b2 -> b2
						.field( "text", String.class, b3 -> { } ) ) );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.setup( IndexedEntity.class, ContainedEntity.class );
		backendMock.verifyExpectationsMet();

		// Hibernate Search started successfully.
		// Check that there actually is a backref:
		MetamodelImplementor metamodel = sessionFactory.unwrap( SessionFactoryImplementor.class ).getMetamodel();
		assertThat( metamodel.entityPersister( IndexedEntity.class ).getPropertyNames() )
				.contains( "_containing_fk_containingidBackref" )
				.contains( "_containingIndexBackref" );

		// If we get here the bug was solved, but let's at least check that indexing works

		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity containing1 = new IndexedEntity();
			containing1.setId( 0 );
			IndexedEntity containing2 = new IndexedEntity();
			containing2.setId( 1 );
			ContainedEntity contained1 = new ContainedEntity();
			contained1.setId( 2 );
			contained1.setText( "theText" );

			containing1.setContained( contained1 );
			contained1.getContaining().add( containing1 );
			containing2.setContained( contained1 );
			contained1.getContaining().add( containing2 );

			session.persist( contained1 );
			session.persist( containing1 );
			session.persist( containing2 );

			backendMock.expectWorks( IndexedEntity.NAME )
					.add( "0", b -> b
							.objectField( "contained", b2 -> b2
									.field( "text", "theText" ) ) )
					.add( "1", b -> b
							.objectField( "contained", b2 -> b2
									.field( "text", "theText" ) ) );
		} );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static final class IndexedEntity {
		static final String NAME = "indexed";

		@Id
		private Integer id;

		@ManyToOne
		@OrderColumn
		@IndexedEmbedded
		private ContainedEntity contained;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ContainedEntity getContained() {
			return contained;
		}

		public void setContained(ContainedEntity contained) {
			this.contained = contained;
		}
	}

	@Entity(name = ContainedEntity.NAME)
	public static final class ContainedEntity {
		static final String NAME = "contained";

		@Id
		private Integer id;

		@GenericField
		private String text;

		@OneToMany
		@OrderColumn
		@JoinColumn(name = "fk_containingid", referencedColumnName = "id", nullable = false)
		@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "contained")))
		private List<IndexedEntity> containing = new ArrayList<>();

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

		public List<IndexedEntity> getContaining() {
			return containing;
		}
	}
}
