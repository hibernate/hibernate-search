/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library;

import static org.hibernate.search.integrationtest.showcase.library.TestActiveProfilesResolver.configuredBackend;

import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.SearchBackendContainer;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
abstract class AbstractLibraryShowcaseSearchIT {
	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		DatabaseContainer.configuration().addAsSpring( (key, value) -> registry.add( key, () -> value ) );

		if ( "elasticsearch".equals( configuredBackend() ) ) {
			registry.add( "spring.jpa.properties.hibernate.search.backend.uris", SearchBackendContainer::connectionUrl );
		}
	}
}
