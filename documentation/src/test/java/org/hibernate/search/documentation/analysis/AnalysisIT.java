/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.ElasticsearchBackendConfiguration;
import org.hibernate.search.documentation.testsupport.LuceneBackendConfiguration;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AnalysisIT {

	private static final String BACKEND_NAME = "myBackend"; // Don't change, the same name is used in property files

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper;

	private final BackendConfiguration backendConfiguration;

	public AnalysisIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( BACKEND_NAME, backendConfiguration );
		this.backendConfiguration = backendConfiguration;
	}

	@Test
	public void simple() {
		EntityManagerFactory entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyName.SYNC
				)
				.withProperties(
						backendConfiguration instanceof LuceneBackendConfiguration
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

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			IndexedEntity entity = new IndexedEntity();
			// Mix French and English to test multiple analyzers with different stemmers
			entity.setText( "THE <strong>châtié</strong> wording" );
			entityManager.persist( entity );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
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
	public void lucene_advanced() {
		Assume.assumeTrue( backendConfiguration instanceof LuceneBackendConfiguration );

		EntityManagerFactory entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyName.SYNC
				)
				.withBackendProperty(
						BACKEND_NAME, LuceneBackendSettings.ANALYSIS_CONFIGURER,
						new AdvancedLuceneAnalysisConfigurer()
				)
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> context.programmaticMapping()
								.type( IndexedEntity.class )
										.property( "text" )
												.fullTextField( "standard" ).analyzer( "standard" )
				)
				.setup( IndexedEntity.class );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setText( "the Wording" );
			entityManager.persist( entity );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			assertThat(
					searchSession.search( IndexedEntity.class )
							.where( f -> f.match()
									.field( "standard" )
									.matching( "wording" )
							)
							.fetchHits( 20 )
			)
					.hasSize( 1 );
		} );
	}

	@Test
	public void elasticsearch_advanced() {
		Assume.assumeTrue( backendConfiguration instanceof ElasticsearchBackendConfiguration );

		EntityManagerFactory entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyName.SYNC
				)
				.withBackendProperty(
						BACKEND_NAME, ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						new AdvancedElasticsearchAnalysisConfigurer()
				)
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> context.programmaticMapping()
								.type( IndexedEntity.class )
										.property( "text" )
												.fullTextField( "standard" ).analyzer( "standard" )
				)
				.setup( IndexedEntity.class );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			IndexedEntity entity = new IndexedEntity();
			entity.setText( "the Wording" );
			entityManager.persist( entity );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			assertThat(
					searchSession.search( IndexedEntity.class )
							.where( f -> f.match()
									.field( "standard" )
									.matching( "wording" )
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
