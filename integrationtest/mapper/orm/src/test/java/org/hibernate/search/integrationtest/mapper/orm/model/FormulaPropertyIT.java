/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.Formula;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class FormulaPropertyIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "string", String.class, b2 -> b2.analyzerName( AnalyzerNames.DEFAULT ) )
		).expectSchema( RootEntity.INDEX, b -> b
				.objectField( "entityShallow",
						b1 -> b1.field( "string", String.class, b2 -> b2.analyzerName( AnalyzerNames.DEFAULT ) ) )
				.objectField( "entityMapped",
						b1 -> b1.field( "string", String.class, b2 -> b2.analyzerName( AnalyzerNames.DEFAULT ) ) ) );

		sessionFactory = ormSetupHelper.start()
				.setup( IndexedEntity.class, RootEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void index() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.id = 1;
			entity1.string = "smth";
			entity1.amount1 = 10;
			entity1.amount2 = 20;

			RootEntity rootEntity = new RootEntity();
			rootEntity.id = 1;
			rootEntity.entityShallow = entity1;
			rootEntity.entityMapped = entity1;
			entity1.rootEntities.add( rootEntity );

			session.persist( entity1 );
			session.persist( rootEntity );

			backendMock.expectWorks( IndexedEntity.INDEX )
					.add( "1", b -> b
							.field( "string", "smth" )
					);
			backendMock.expectWorks( RootEntity.INDEX )
					.add( "1", b -> b
							.objectField( "entityShallow", b1 -> b1.field( "string", "smth" ) )
							.objectField( "entityMapped", b1 -> b1.field( "string", "smth" ) )
					);
		} );
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity entity1 = session.get( IndexedEntity.class, 1 );
			assertThat( entity1.string ).isEqualTo( "smth" );
			assertThat( entity1.amountDifference ).isEqualTo( 10 );
		} );
	}

	@Entity(name = "root_entity")
	@Indexed(index = RootEntity.INDEX)
	public static final class RootEntity {

		static final String INDEX = "RootEntity";

		@Id
		public Integer id;

		@IndexedEmbedded
		@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
		@ManyToOne
		public IndexedEntity entityShallow;

		@IndexedEmbedded
		@ManyToOne
		public IndexedEntity entityMapped;

	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static final class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
		public Integer id;

		@FullTextField
		public String string;

		public int amount1;
		public int amount2;

		@Formula("amount2 - amount1")
		public int amountDifference;

		@OneToMany(mappedBy = "entityMapped")
		Set<RootEntity> rootEntities = new HashSet<>();

	}
}
