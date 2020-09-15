/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.identifiertovalue.impl;

import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;

/**
 * An adapter from {@link IdentifierBinder} to {@link ValueBinder},
 * used to apply an identifier bridge to create an index field.
 */
public class IdentifierBinderToValueBinderAdapter implements ValueBinder {
	private final IdentifierBinder identifierBinder;

	public IdentifierBinderToValueBinderAdapter(IdentifierBinder identifierBinder) {
		this.identifierBinder = identifierBinder;
	}

	@Override
	public void bind(ValueBindingContext<?> context) {
		identifierBinder.bind( new ValueBindingContextToIdentifierBindingContextAdapter<>( context ) );
	}
}
