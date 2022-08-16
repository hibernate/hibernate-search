/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration.isLucene;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import org.junit.Rule;
import org.junit.Test;

public class AnalysisIT {

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	@Test
	public void simple() {
		EntityManagerFactory entityManagerFactory = setupHelper.start()
				.withProperties(
						isLucene()
								? "/analysis/lucene-simple.properties"
								: "/analysis/elasticsearch-simple.properties"
				)
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> context.programmaticMapping()
								.type( IndexedEntity.class )
										.property( "text" )
												.fullTextField( "english" ).analyzer( "english" )
												.fullTextField( "french" ).analyzer( "french" )
												.keywordField( "lowercase" ).normalizer( "lowercase" )
				)
				.setup( IndexedEntity.class );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			IndexedEntity entity = new IndexedEntity();
			// Mix French and English to test multiple analyzers with different stemmers
			entity.setText( "THE <strong>châtié</strong> wording" );
			entityManager.persist( entity );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			assertThat(
					searchSession.search( IndexedEntity.class )
							.where( f -> f.match()
									.field( "english" )
									.matching( "worded" )
							)
							.fetchHits( 20 )
			)
					.hasSize( 1 );

			assertThat(
					searchSession.search( IndexedEntity.class )
							.where( f -> f.match()
									.field( "french" )
									.matching( "châtier" )
							)
							.fetchHits( 20 )
			)
					.hasSize( 1 );

			assertThat(
					searchSession.search( IndexedEntity.class )
							.where( f -> f.match()
									.field( "lowercase" )
									.matching( "the <strong>châtié</strong> WORDING" )
							)
							.fetchHits( 20 )
			)
					.hasSize( 1 );
		} );
	}

	@Test
	public void default_override() {
		EntityManagerFactory entityManagerFactory = setupHelper.start()
				.withProperties(
						isLucene()
								? "/analysis/lucene-default-override.properties"
								: "/analysis/elasticsearch-default-override.properties"
				)
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> context.programmaticMapping()
								.type( IndexedEntity.class )
								.property( "text" )
								.fullTextField()
				)
				.setup( IndexedEntity.class );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setText( "un language châtié" );
			entityManager.persist( entity );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			assertThat(
					searchSession.search( IndexedEntity.class )
							.where( f -> f.match()
									.field( "text" )
									.matching( "châtier" )
							)
							.fetchHits( 20 )
			)
					.hasSize( 1 );
		} );
	}


	@Entity(name = IndexedEntity.NAME)
	@Indexed
	static class IndexedEntity {

		static final String NAME = "indexed";

		@Id
		@GeneratedValue
		private Integer id;

		private String text;

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
