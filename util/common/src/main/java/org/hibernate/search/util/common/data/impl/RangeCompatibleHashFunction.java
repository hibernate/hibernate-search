/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.data.impl;

/**
 * Tagging interface for hash functions that can reasonably be used
 * with {@link RangeHashTable}.
 * <p>
 * Such hash functions produce a hash that is evenly distributed across the integer space.
 * That's not the case of {@link SimpleHashFunction},
 * which requires a modulo operation before it can be used reliably in a hash table.
 */
public interface RangeCompatibleHashFunction extends HashFunction {

}
