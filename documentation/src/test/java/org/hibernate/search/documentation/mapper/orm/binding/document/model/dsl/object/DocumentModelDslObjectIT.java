/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.document.model.dsl.object;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DocumentModelDslObjectIT {
	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Invoice.class );
	}

	@Test
	public void smoke() {
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
									.atMost( new BigDecimal( "10.0" ) ),
							f.nested( "lineItems" )
									.add( f.range().field( "lineItems.amount" )
											.between( new BigDecimal( "7.0" ), new BigDecimal( "9.0" ) ) )
									.add( f.match().field( "lineItems.category" )
											.matching( "BOOK" ) )
					) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 );
		} );
	}

}
