/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl;

import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;

public interface LuceneStandardIndexFieldTypeOptionsStep<S extends LuceneStandardIndexFieldTypeOptionsStep<?, F>, F>
		extends StandardIndexFieldTypeOptionsStep<S, F> {

}
