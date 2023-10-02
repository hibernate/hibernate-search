/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;

public interface AggregationExtractContext {

	FromDocumentValueConvertContext fromDocumentValueConvertContext();

}
