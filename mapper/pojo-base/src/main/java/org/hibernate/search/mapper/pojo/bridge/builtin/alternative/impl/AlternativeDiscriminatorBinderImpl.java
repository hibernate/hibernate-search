/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.alternative.impl;

import org.hibernate.search.mapper.pojo.bridge.binding.MarkerBindingContext;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.AlternativeDiscriminatorBinder;

public final class AlternativeDiscriminatorBinderImpl implements AlternativeDiscriminatorBinder {

	private String id;

	@Override
	public AlternativeDiscriminatorBinder id(String id) {
		this.id = id;
		return this;
	}

	@Override
	public void bind(MarkerBindingContext context) {
		context.marker( new Marker( id ) );
	}

	public static final class Marker {

		private final String id;

		private Marker(String id) {
			this.id = id;
		}

		public String id() {
			return id;
		}

	}
}
