/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.session.impl;

import org.hibernate.search.mapper.orm.model.impl.HibernateOrmRuntimeIntrospectorTypeContextProvider;
import org.hibernate.search.mapper.orm.work.impl.SearchIndexingPlanTypeContextProvider;

public interface HibernateOrmSessionTypeContextProvider
		extends HibernateOrmRuntimeIntrospectorTypeContextProvider,
		SearchIndexingPlanTypeContextProvider {

}
