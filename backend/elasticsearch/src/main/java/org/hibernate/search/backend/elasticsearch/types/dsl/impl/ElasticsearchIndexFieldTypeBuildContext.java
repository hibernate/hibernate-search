package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.types.format.impl.ElasticsearchDefaultFieldFormatProvider;
import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;
import org.hibernate.search.util.common.reporting.EventContext;

public interface ElasticsearchIndexFieldTypeBuildContext {

	EventContext getEventContext();

	JsonCodecService getUserFacingCodec();

	ElasticsearchDefaultFieldFormatProvider getDefaultFieldFormatProvider();

	BackendMappingHints hints();

}
