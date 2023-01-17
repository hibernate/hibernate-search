/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
