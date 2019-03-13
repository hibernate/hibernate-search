/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.format.impl;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;

public interface ElasticsearchDefaultFieldFormatProvider {

	DateTimeFormatter getDefaultDateTimeFormatter(Class<? extends TemporalAccessor> fieldType);

	List<String> getDefaultMappingFormat(Class<? extends TemporalAccessor> fieldType);

}
