/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.GsonSerializable;

/**
 * A definition of an Elasticsearch normalizer, to be included in index settings.
 *
 */
@GsonSerializable
public class NormalizerDefinition extends AbstractCompositeAnalysisDefinition {

}
