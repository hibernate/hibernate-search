/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.java.modules.pojo.standalone.elasticsearch.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JavaModulePathIT {

	@BeforeAll
	@AfterAll
	static void clear() {
		SimulatedDatastore.clear();
	}

	/*
	 * Test that the service successfully uses Hibernate Search in the module path.
	 * We don't really care about the features themselves,
	 * but the easiest way to check this is to just use Hibernate Search features and see if it works.
	 */
	@Test
	void test() {
		checkIsInModulePath( Object.class );
		checkIsInModulePath( AuthorService.class );
		checkIsInModulePath( SearchMapping.class );

		AuthorService service = new AuthorService();
		service.add( "foo" );
		service.add( "bar" );
		service.add( "foo bar" );
		assertThat( service.search( "foo" ) ).hasSize( 2 );
	}

	private void checkIsInModulePath(Class<?> clazz) {
		assertThat( clazz.getModule().isNamed() )
				.as( clazz + " should be part of a named module - there is a problem in test setup" )
				.isTrue();
	}
}
