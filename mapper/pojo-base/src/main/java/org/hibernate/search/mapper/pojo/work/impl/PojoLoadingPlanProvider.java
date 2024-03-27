/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.impl;

import org.hibernate.search.mapper.pojo.loading.impl.PojoLoadingPlan;

public interface PojoLoadingPlanProvider {

	PojoLoadingPlan<Object> loadingPlan();

}
