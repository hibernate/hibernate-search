/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
