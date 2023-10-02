/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.session.impl;

import org.hibernate.search.mapper.pojo.standalone.work.impl.SearchIndexingPlanTypeContextProvider;
import org.hibernate.search.util.common.data.spi.KeyValueProvider;

public interface StandalonePojoSearchSessionTypeContextProvider
		extends SearchIndexingPlanTypeContextProvider {

	KeyValueProvider<String, ? extends StandalonePojoSessionIndexedTypeContext<?>> indexedByEntityName();

}
