/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone;

import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.common.assertion.MappingAssertionHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendSetupStrategy;

public class StandalonePojoAssertionHelper extends MappingAssertionHelper<SearchMapping> {
	public StandalonePojoAssertionHelper(BackendConfiguration backendConfiguration) {
		super( backendConfiguration );
	}

	public StandalonePojoAssertionHelper(BackendSetupStrategy backendSetupStrategy) {
		super( backendSetupStrategy );
	}

	@Override
	protected void doRefresh(SearchMapping entryPoint) {
		entryPoint.scope( Object.class ).workspace().refresh();
	}
}
