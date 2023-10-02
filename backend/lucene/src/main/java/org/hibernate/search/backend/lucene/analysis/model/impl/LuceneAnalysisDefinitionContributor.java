/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.model.impl;

import java.util.Optional;

import org.apache.lucene.search.similarities.Similarity;

public interface LuceneAnalysisDefinitionContributor {

	void contribute(LuceneAnalysisDefinitionCollector collector);

	Optional<Similarity> getSimilarity();

}
