/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import org.hibernate.search.engine.backend.common.DocumentReference;

/**
 * The initial and final step in a "document reference" projection definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface DocumentReferenceProjectionOptionsStep<S extends DocumentReferenceProjectionOptionsStep<?>>
		extends ProjectionFinalStep<DocumentReference> {

}
