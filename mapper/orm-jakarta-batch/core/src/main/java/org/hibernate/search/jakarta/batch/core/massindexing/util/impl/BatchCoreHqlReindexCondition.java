/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.util.impl;

import java.util.Collections;
import java.util.Map;

import org.hibernate.search.mapper.orm.loading.batch.HibernateOrmBatchReindexCondition;

public class BatchCoreHqlReindexCondition implements HibernateOrmBatchReindexCondition {

	private final String reindexOnlyHql;
	private final Map<String, Object> params;

	public BatchCoreHqlReindexCondition(String reindexOnlyHql, Map<String, Object> params) {
		this.reindexOnlyHql = reindexOnlyHql;
		this.params = Collections.unmodifiableMap( params );
	}

	@Override
	public String conditionString() {
		return reindexOnlyHql;
	}

	@Override
	public Map<String, Object> params() {
		return params;
	}
}
