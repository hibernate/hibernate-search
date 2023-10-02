/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.dsl;

import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;

public interface ElasticsearchStandardIndexFieldTypeOptionsStep<
		S extends ElasticsearchStandardIndexFieldTypeOptionsStep<?, F>,
		F>
		extends StandardIndexFieldTypeOptionsStep<S, F> {

}
