/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.backend.lucene.type.asnative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.document.FeatureField;

public class LuceneNativeTypeIT {
	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( WebPage.class );
	}

	@Test
	public void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			WebPage webPage1 = new WebPage();
			webPage1.setId( 1 );
			webPage1.setPageRank( 2.0f );
			WebPage webPage2 = new WebPage();
			webPage2.setId( 2 );
			webPage2.setPageRank( 5.0f );
			WebPage webPage3 = new WebPage();
			webPage3.setId( 3 );
			webPage3.setPageRank( 1.0f );
			entityManager.persist( webPage1 );
			entityManager.persist( webPage2 );
			entityManager.persist( webPage3 );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Result> result = searchSession.search( WebPage.class )
					.extension( LuceneExtension.get() )
					.select( f -> f.composite()
							.from( f.entity(), f.field( "pageRank", Float.class ) )
							.as( Result::new ) )
					.where( f -> f.fromLuceneQuery(
							// This affects the document score based on the pageRank
							FeatureField.newSaturationQuery( "pageRank", "pageRank" )
					) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 3 )
					.extracting( r -> r.webPage )
					.extracting( WebPage::getId )
					.containsExactly( 2, 1, 3 );
			assertThat( result )
					.extracting( r -> r.projectedPageRank )
					.containsExactly( 5.0f, 2.0f, 1.0f );
		} );
	}

	private static class Result {
		private final WebPage webPage;
		private final Float projectedPageRank;

		private Result(WebPage webPage, Float projectedPageRank) {
			this.webPage = webPage;
			this.projectedPageRank = projectedPageRank;
		}
	}

}
