/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.types.format.impl.ElasticsearchDefaultFieldFormatProvider;
import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.Gson;

public interface ElasticsearchIndexFieldTypeBuildContext {

	EventContext getEventContext();

	Gson getUserFacingGson();

	ElasticsearchDefaultFieldFormatProvider getDefaultFieldFormatProvider();

	BackendMappingHints hints();

}
