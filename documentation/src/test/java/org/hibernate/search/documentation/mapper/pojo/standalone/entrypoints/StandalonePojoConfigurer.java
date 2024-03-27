/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.entrypoints;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;

public class StandalonePojoConfigurer implements StandalonePojoMappingConfigurer {
	@Override
	public void configure(StandalonePojoMappingConfigurationContext context) {
		context.programmaticMapping().type( Book.class ).searchEntity();
		context.programmaticMapping().type( Associate.class ).searchEntity();
		context.programmaticMapping().type( Manager.class ).searchEntity();

		context.defaultReindexOnUpdate( ReindexOnUpdate.SHALLOW );
	}
}
