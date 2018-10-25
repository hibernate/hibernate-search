/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;

public final class DefaultIntegerIdentifierBridge implements IdentifierBridge<Integer> {

	@Override
	public String toDocumentIdentifier(Integer propertyValue) {
		return propertyValue.toString();
	}

	@Override
	public Integer fromDocumentIdentifier(String documentIdentifier, IdentifierBridgeFromDocumentIdentifierContext context) {
		return Integer.parseInt( documentIdentifier );
	}

}