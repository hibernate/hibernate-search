/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.reference;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface RootReferenceScope<SR, T> {
	Class<SR> rootReferenceType();
}
