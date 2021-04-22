/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.valuebridge.parameter.context;

import org.hibernate.search.documentation.mapper.orm.bridge.valuebridge.parameter.BooleanAsStringBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;

//tag::include[]
public class BooleanAsStringBinder implements ValueBinder {

	@Override
	@SuppressWarnings("unchecked")
	public void bind(ValueBindingContext<?> context) {
		String trueAsString = (String) context.parameter( "trueAsString" ); // <1>
		String falseAsString = (String) context.parameter( "falseAsString" );

		context.bridge( Boolean.class, // <2>
				new BooleanAsStringBridge( trueAsString, falseAsString ) );
	}
}
//end::include[]