/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.GsonSerializable;

/**
 * A definition of an Elasticsearch tokenizer, to be included in index settings.
 *
 */
@GsonSerializable
public class TokenizerDefinition extends AnalysisDefinition {

}
