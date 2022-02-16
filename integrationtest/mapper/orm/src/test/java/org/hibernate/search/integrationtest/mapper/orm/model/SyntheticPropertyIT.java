/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Check that we correctly populate the metamodel when ORM generates a {@link org.hibernate.mapping.SyntheticProperty},
 * which happens in particular when there is a many-to-one whose referenced column is not the entity ID.
 */
@TestForIssue(jiraKey = "HSEARCH-4156")
public class SyntheticPropertyIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	public void test() {
		backendMock.expectSchema( IndexedEntity.NAME, b -> b
				.objectField( "contained", b2 -> b2
						.field( "ref", String.class, b3 -> { } ) ) );

		SessionFactory sessionFactory = ormSetupHelper.start()
				.setup( IndexedEntity.class, ContainedEntity.class );
		backendMock.verifyExpectationsMet();

		// Hibernate Search started successfully.
		// Check that there actually is a synthetic property:
		MetamodelImplementor metamodel = sessionFactory.unwrap( SessionFactoryImplementor.class ).getMetamodel();
		assertThat( metamodel.entityPersister( ContainedEntity.class ).getPropertyNames() )
				.contains( "_" + IndexedEntity.class.getName().replace( '.', '_' ) + "_contained" );

		// If we get here the bug was solved, but let's at least check that indexing works

		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity containing1 = new IndexedEntity();
			containing1.setId( 0 );
			IndexedEntity containing2 = new IndexedEntity();
			containing2.setId( 1 );
			ContainedEntity contained1 = new ContainedEntity();
			contained1.setId( 2 );
			contained1.setRef( "theRef" );
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
									.field( "ref", "theRef" ) ) )
					.add( "1", b -> b
							.objectField( "contained", b2 -> b2
									.field( "ref", "theRef" ) ) );
		} );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed
	public static final class IndexedEntity {
		static final String NAME = "indexed";

		@Id
		private Integer id;

		@ManyToOne
		@IndexedEmbedded
		@JoinColumn(name = "ref", referencedColumnName = "ref")
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
	public static final class ContainedEntity
			implements Serializable { // The entity must implement Serializable in this case, for some reason.
		static final String NAME = "contained";

		@Id
		private Integer id;

		@GenericField
		private String ref;

		@OneToMany(mappedBy = "contained")
		private List<IndexedEntity> containing = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getRef() {
			return ref;
		}

		public void setRef(String ref) {
			this.ref = ref;
		}

		public List<IndexedEntity> getContaining() {
			return containing;
		}
	}

}
