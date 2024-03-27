/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.work.result.impl;

import com.google.gson.JsonObject;

public interface ExplainResult {

	JsonObject getJsonObject();

}
