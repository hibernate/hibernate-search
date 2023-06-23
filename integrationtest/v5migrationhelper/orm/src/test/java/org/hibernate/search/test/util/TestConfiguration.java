/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util;

import java.util.Map;
import java.util.Set;

/**
 * Contract to several testing setup utilities to define the
 * configuration of a SessionFactory for testing.
 *
 * @author Sanne Grinovero
 * @since 5.4
 */
public interface TestConfiguration {

	/**
	 * The implementation should set the configuration properties
	 * on the passed object. Usually this will be a String value,
	 * but sometimes it can be different type.
	 * @param settings
	 */
	void configure(Map<String, Object> settings);

	/**
	 * Use this to enable a multi-tenant capable SessionFactory,
	 * or return an empty Set.
	 * @return the tenant identifiers
	 */
	Set<String> multiTenantIds();

	/**
	 * List the entities which should be used by the SessionFactory
	 * building system as a metatada source.
	 * @return usually all entity types used by the test
	 */
	Class<?>[] getAnnotatedClasses();

}
