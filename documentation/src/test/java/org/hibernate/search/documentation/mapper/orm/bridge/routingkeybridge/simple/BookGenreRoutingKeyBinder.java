/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.routingkeybridge.simple;

import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;

//tag::binder[]
public class BookGenreRoutingKeyBinder implements RoutingKeyBinder { // <1>

	@Override
	public void bind(RoutingKeyBindingContext context) { // <2>
		context.dependencies() // <3>
				.use( "genre" );

		context.bridge( new Bridge() ); // <4>
	}

	// ... class continues below
	//end::binder[]
	//tag::bridge[]
	// ... class BookGenreRoutingKeyBinder (continued)

	public static class Bridge implements RoutingKeyBridge { // <1>

		@Override
		public String toRoutingKey(String tenantIdentifier, Object entityIdentifier, // <2>
				Object bridgedElement, RoutingKeyBridgeToRoutingKeyContext context) {
			Book book = (Book) bridgedElement; // <3>
			String routingKey = book.getGenre().name(); // <4>
			return routingKey; // <5>
		}

	}
}
//end::bridge[]
