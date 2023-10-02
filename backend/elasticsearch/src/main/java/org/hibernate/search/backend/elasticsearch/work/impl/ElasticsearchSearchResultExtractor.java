/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.engine.common.timing.Deadline;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchResultExtractor<R> {

	R extract(JsonObject responseBody, Deadline deadline);

}
