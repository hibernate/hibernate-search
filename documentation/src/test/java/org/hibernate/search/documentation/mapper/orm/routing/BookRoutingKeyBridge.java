/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.routing;

import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;

public class BookRoutingKeyBridge implements RoutingKeyBridge {

	@Override
	public String toRoutingKey(String tenantIdentifier, Object entityIdentifier, Object bridgedElement,
			RoutingKeyBridgeToRoutingKeyContext context) {
		return ( (Book) bridgedElement ).getGenre().name();
	}

	public static class Binder implements RoutingKeyBinder<BookRoutingKeyBinding> {
		@Override
		public void bind(RoutingKeyBindingContext context) {
			context.getDependencies().use( "genre" );
			context.setBridge( new BookRoutingKeyBridge() );
		}
	}
}
