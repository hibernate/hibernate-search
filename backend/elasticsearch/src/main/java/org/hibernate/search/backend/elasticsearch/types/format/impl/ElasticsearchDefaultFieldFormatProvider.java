package org.hibernate.search.backend.elasticsearch.types.format.impl;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;

public interface ElasticsearchDefaultFieldFormatProvider {

	DateTimeFormatter getDefaultDateTimeFormatter(Class<? extends TemporalAccessor> fieldType);

	List<String> getDefaultMappingFormat(Class<? extends TemporalAccessor> fieldType);

}
