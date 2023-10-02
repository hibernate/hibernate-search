/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.engine.spi;

/**
 * @deprecated This class will be removed without replacement. Use actual API instead.
 */
@Deprecated
public interface TupleTransformer {
	Object transform(Object[] tuple, String[] projectedFields);
}
