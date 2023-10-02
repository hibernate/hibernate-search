/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.JsonAdapter;

/**
 * A definition of an Elasticsearch char filter, to be included in index settings.
 *
 */
/*
 * CAUTION: JSON serialization is controlled by a specific adapter, which must be
 * updated whenever fields of this class are added, renamed or removed.
 */
@JsonAdapter(AnalysisDefinitionJsonAdapterFactory.class)
public class CharFilterDefinition extends AnalysisDefinition {

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson( this );
	}

}
