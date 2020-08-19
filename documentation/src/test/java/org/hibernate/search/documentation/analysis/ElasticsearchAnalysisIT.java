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

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchAnalysisIT {

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	@Test
	public void advanced() {
		EntityManagerFactory entityManagerFactory = setupHelper.start()
				.withBackendProperty(
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
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
