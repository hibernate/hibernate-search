/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.entrypoints;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;

// tag::configurer[]

public class StandalonePojoConfigurer implements StandalonePojoMappingConfigurer {
	@Override
	public void configure(StandalonePojoMappingConfigurationContext context) {
		context.addEntityTypes( Book.class, Associate.class, Manager.class );// <1>

		context.defaultReindexOnUpdate( ReindexOnUpdate.SHALLOW ); // <2>
	}
}
// end::configurer[]
