/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.namedpredicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class NamedPredicateIT {
	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( ItemStock.class );
	}

	@Test
	void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			ItemStock unit1 = new ItemStock();
			unit1.setSkuId( "SHOES.WI2012.4242" );
			unit1.setAmountInStock( 23 );
			entityManager.persist( unit1 );
			ItemStock unit2 = new ItemStock();
			unit2.setSkuId( "STREETWEAR.WI2012.798" );
			unit2.setAmountInStock( 1 );
			entityManager.persist( unit2 );
			ItemStock unit3 = new ItemStock();
			unit3.setSkuId( "STREETWEAR.SU2012.145" );
			unit3.setAmountInStock( 89 );
			entityManager.persist( unit3 );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::named-predicate[]
			List<ItemStock> hits = searchSession.search( ItemStock.class )
					.where( f -> f.named( "skuId.skuIdMatch" ) // <1>
							.param( "pattern", "*.WI2012" ) ) // <2>
					.fetchHits( 20 );
			// end::named-predicate[]
			assertThat( hits ).hasSize( 2 );

			hits = searchSession.search( ItemStock.class )
					.where( f -> f.named( "skuId.skuIdMatch" )
							.param( "pattern", "STREETWEAR.WI2012" ) )
					.fetchHits( 20 );
			assertThat( hits ).hasSize( 1 );

			hits = searchSession.search( ItemStock.class )
					.where( f -> f.named( "skuId.skuIdMatch" )
							.param( "pattern", "STREETWEAR" ) )
					.fetchHits( 20 );
			assertThat( hits ).hasSize( 2 );
		} );
	}

}
