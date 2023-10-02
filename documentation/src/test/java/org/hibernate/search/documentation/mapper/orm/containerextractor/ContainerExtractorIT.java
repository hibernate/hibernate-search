/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.containerextractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ContainerExtractorIT {

	public static List<? extends Arguments> params() {
		return DocumentationSetupHelper.testParamsForBothAnnotationsAndProgrammatic(
				mapping -> {
					TypeMappingStep bookMapping = mapping.type( Book.class );
					bookMapping.indexed();
				// @formatter:off
					//tag::programmatic-extractor[]
					bookMapping.property( "priceByFormat" )
							.genericField( "availableFormats" )
									.extractor( BuiltinContainerExtractors.MAP_KEY );
					//end::programmatic-extractor[]
					//tag::programmatic-noExtractors[]
					bookMapping.property( "authors" )
							.genericField( "authorCount" )
									.valueBridge( new MyCollectionSizeBridge() )
									.noExtractors();
					//end::programmatic-noExtractors[]
					// @formatter:on
				} );
	}

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );
	private EntityManagerFactory entityManagerFactory;

	public void init(DocumentationSetupHelper.SetupVariant variant) {
		entityManagerFactory = setupHelper.start( variant )
				.setup( Book.class, Author.class );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void smoke(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Author author = new Author();
			author.setId( 1 );
			author.setName( "Isaac Asimov" );

			Book book = new Book();
			book.setId( 1 );
			book.setTitle( "The Caves Of Steel" );
			book.getAuthors().add( author );
			author.getBooks().add( book );
			book.getPriceByFormat().put( BookFormat.AUDIOBOOK, new BigDecimal( "15.99" ) );
			book.getPriceByFormat().put( BookFormat.HARDCOVER, new BigDecimal( "25.99" ) );

			entityManager.persist( author );
			entityManager.persist( book );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Book> result = searchSession.search( Book.class )
					.where( f -> f.and(
							f.match().field( "availableFormats" ).matching( BookFormat.AUDIOBOOK ),
							f.match().field( "availableFormats" ).matching( BookFormat.HARDCOVER ),
							f.match().field( "authorCount" ).matching( 1, ValueConvert.NO )
					) )
					.fetchHits( 20 );
			assertThat( result ).hasSize( 1 );
		} );
	}

}
