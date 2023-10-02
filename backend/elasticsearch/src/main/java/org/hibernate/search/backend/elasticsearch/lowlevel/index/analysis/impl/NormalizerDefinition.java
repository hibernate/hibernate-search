/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl;

import com.google.gson.annotations.JsonAdapter;

/**
 * A definition of an Elasticsearch normalizer, to be included in index settings.
 *
 */
@JsonAdapter(NormalizerDefinitionJsonAdapterFactory.class)
public class NormalizerDefinition extends AbstractCompositeAnalysisDefinition {

}
