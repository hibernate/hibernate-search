/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

public enum StubMappingSchemaManagementStrategy {

	NONE,
	DROP_AND_CREATE_AND_DROP,
	DROP_AND_CREATE_ON_STARTUP_ONLY,
	DROP_ON_SHUTDOWN_ONLY;

}
