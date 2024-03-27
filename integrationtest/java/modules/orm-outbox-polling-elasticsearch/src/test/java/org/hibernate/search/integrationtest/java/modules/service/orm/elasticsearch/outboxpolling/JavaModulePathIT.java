/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.java.modules.service.orm.elasticsearch.outboxpolling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import org.hibernate.search.integrationtest.java.modules.orm.elasticsearch.outboxpolling.service.AuthorService;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.Test;

class JavaModulePathIT {

	/*
	 * Test that the service successfully uses Hibernate Search in the module path.
	 * We don't really care about the features themselves,
	 * but the easiest way to check this is to just use Hibernate Search features and see if it works.
	 */
	@Test
	void test() {
		checkIsInModulePath( Object.class );
		checkIsInModulePath( AuthorService.class );
		checkIsInModulePath( Search.class );

		try ( AuthorService service = new AuthorService() ) {
			service.add( "foo" );
			service.add( "bar" );
			service.add( "foo bar" );

			await().untilAsserted( () -> {
				assertThat( service.search( "foo" ) ).hasSize( 2 );
			} );

			assertThatThrownBy( service::triggerValidationFailure )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Hibernate Search encountered failures during Schema management",
							"attribute 'analyzer'",
							"Invalid value. Expected 'default', actual is 'myAnalyzer'"
					);
		}
	}

	private void checkIsInModulePath(Class<?> clazz) {
		assertThat( clazz.getModule().isNamed() )
				.as( clazz + " should be part of a named module - there is a problem in test setup" )
				.isTrue();
	}
}
