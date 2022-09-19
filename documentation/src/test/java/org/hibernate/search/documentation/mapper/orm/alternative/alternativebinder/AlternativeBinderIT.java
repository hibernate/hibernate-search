/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.alternative.alternativebinder;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.AlternativeBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AlternativeBinderIT {
	@Parameterized.Parameters(name = "{0}")
	public static List<?> params() {
		return DocumentationSetupHelper.testParamsForBothAnnotationsAndProgrammatic(
				BackendConfigurations.simple(),
				mapping -> {
					//tag::programmatic[]
					TypeMappingStep blogEntryMapping = mapping.type( BlogEntry.class );
					blogEntryMapping.indexed();
					blogEntryMapping.property( "language" )
							.marker( AlternativeBinder.alternativeDiscriminator() );
					LanguageAlternativeBinderDelegate delegate = new LanguageAlternativeBinderDelegate( null );
					blogEntryMapping.binder( AlternativeBinder.create( Language.class,
							"text", String.class, BeanReference.ofInstance( delegate ) ) );
					//end::programmatic[]
				} );
	}

	@Parameterized.Parameter
	@Rule
	public DocumentationSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( BlogEntry.class );
	}

	@Test
	public void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			BlogEntry entry1 = new BlogEntry();
			entry1.setId( 1 );
			entry1.setLanguage( Language.GERMAN );
			entry1.setText( "Auf Französisch ist „Wiedervereinigung“ „réunification“" );
			BlogEntry entry2 = new BlogEntry();
			entry2.setId( 2 );
			entry2.setLanguage( Language.FRENCH );
			entry2.setText( "En Allemand, « réunification » se dit « Wierdervereinigung »" );

			entityManager.persist( entry1 );
			entityManager.persist( entry2 );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// Only matches the german text, because the word is decomposed
			assertThat( searchSession.search( BlogEntry.class )
					.where( f -> f.match().fields( "text_en", "text_de", "text_fr" )
							.matching( "vereinigung" ) )
					.fetchHits( 20 ) )
					.extracting( BlogEntry::getId ).containsExactly( 1 );
			// Better score for the german text, because the word is decomposed
			assertThat( searchSession.search( BlogEntry.class )
					.where( f -> f.match().fields( "text_en", "text_de", "text_fr" )
							.matching( "Wierdervereinigung" ) )
					.fetchHits( 20 ) )
					.extracting( BlogEntry::getId ).containsExactly( 1, 2 );
		} );
	}

}
