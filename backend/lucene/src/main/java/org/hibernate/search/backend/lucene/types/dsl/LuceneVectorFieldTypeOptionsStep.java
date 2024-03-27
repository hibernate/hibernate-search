/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl;

import org.hibernate.search.engine.backend.types.dsl.VectorFieldTypeOptionsStep;

public interface LuceneVectorFieldTypeOptionsStep<S extends LuceneVectorFieldTypeOptionsStep<?, F>, F>
		extends VectorFieldTypeOptionsStep<S, F> {

}
