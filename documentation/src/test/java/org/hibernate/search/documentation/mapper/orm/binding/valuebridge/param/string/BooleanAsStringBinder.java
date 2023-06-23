/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.param.string;

import org.hibernate.search.documentation.mapper.orm.binding.valuebridge.param.annotation.BooleanAsStringBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;

//tag::include[]
public class BooleanAsStringBinder implements ValueBinder {

	@Override
	public void bind(ValueBindingContext<?> context) {
		String trueAsString = context.param( "trueAsString", String.class ); // <1>
		String falseAsString = context.param( "falseAsString", String.class );

		context.bridge( Boolean.class, // <2>
				new BooleanAsStringBridge( trueAsString, falseAsString ) );
	}
}
//end::include[]
