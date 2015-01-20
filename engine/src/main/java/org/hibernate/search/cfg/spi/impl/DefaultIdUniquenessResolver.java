/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.cfg.spi.impl;

import org.hibernate.search.cfg.spi.IdUniquenessResolver;

/**
 * Default implementation that is conservative and always answers that ids may not be unique.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class DefaultIdUniquenessResolver implements IdUniquenessResolver {
	@Override
	public boolean areIdsUniqueForClasses(Class<?> entityInIndex, Class<?> otherEntityInIndex) {
		return false;
	}
}
