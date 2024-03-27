/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.propertybridge.simple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class PropertyBridgeSimpleIT {
	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( Invoice.class );
	}

	@Test
	void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Invoice invoice = new Invoice();
			invoice.getLineItems()
					.add( new InvoiceLineItem( InvoiceLineItemCategory.BOOK, new BigDecimal( "5.99" ) ) );
			invoice.getLineItems()
					.add( new InvoiceLineItem( InvoiceLineItemCategory.BOOK, new BigDecimal( "8.99" ) ) );
			invoice.getLineItems()
					.add( new InvoiceLineItem( InvoiceLineItemCategory.BOOK, new BigDecimal( "15.99" ) ) );
			invoice.getLineItems()
					.add( new InvoiceLineItem( InvoiceLineItemCategory.SHIPPING, new BigDecimal( "7.99" ) ) );
			entityManager.persist( invoice );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Invoice> result = searchSession.search( Invoice.class )
					.where( f -> f.and(
							f.range().field( "summary.total" )
									.atLeast( new BigDecimal( "20.0" ) ),
							f.range().field( "summary.shipping" )
									.atMost( new BigDecimal( "10.0" ) )
					) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 );
		} );
	}

}
