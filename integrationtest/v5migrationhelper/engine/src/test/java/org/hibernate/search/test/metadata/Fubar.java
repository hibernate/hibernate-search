/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.metadata;

import org.hibernate.search.annotations.Boost;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.DynamicBoost;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.BoostStrategy;

/**
 * @author Hardy Ferentschik
 */
@Indexed
@Boost(42.0f)
@DynamicBoost(impl = Fubar.DoublingBoost.class)
public class Fubar {
	@DocumentId
	private long id;

	public static class DoublingBoost implements BoostStrategy {
		@Override
		public float defineBoost(Object value) {
			return 2.0f;
		}
	}
}


