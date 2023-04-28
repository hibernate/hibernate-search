/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.identifierbridge.param.annotation;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

//tag::include[]
public class OffsetIdentifierBridge implements IdentifierBridge<Integer> { // <1>

	private final int offset;

	public OffsetIdentifierBridge(int offset) { // <2>
		this.offset = offset;
	}

	@Override
	public String toDocumentIdentifier(Integer propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
		return String.valueOf( propertyValue + offset );
	}

	@Override
	public Integer fromDocumentIdentifier(String documentIdentifier,
			IdentifierBridgeFromDocumentIdentifierContext context) {
		return Integer.parseInt( documentIdentifier ) - offset;
	}
}
//end::include[]
