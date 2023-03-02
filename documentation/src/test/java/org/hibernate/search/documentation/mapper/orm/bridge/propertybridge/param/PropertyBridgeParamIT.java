/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.propertybridge.param;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.math.BigDecimal;
import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class PropertyBridgeParamIT {
	@Parameterized.Parameters(name = "{0}")
	public static List<?> params() {
		return DocumentationSetupHelper.testParamsForBothAnnotationsAndProgrammatic(
				BackendConfigurations.simple(),
				mapping -> {
					//tag::programmatic[]
					TypeMappingStep invoiceMapping = mapping.type( Invoice.class );
					invoiceMapping.indexed();
					invoiceMapping.property( "lineItems" )
							.binder( new InvoiceLineItemsSummaryBinder().fieldName( "itemSummary" ) );
					//end::programmatic[]
				} );
	}

	@Parameterized.Parameter
	@Rule
	public DocumentationSetupHelper setupHelper;

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
							f.range().field( "itemSummary.total" )
									.atLeast( new BigDecimal( "20.0" ) ),
							f.range().field( "itemSummary.shipping" )
									.atMost( new BigDecimal( "10.0" ) )
					) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 );
		} );
	}

}
