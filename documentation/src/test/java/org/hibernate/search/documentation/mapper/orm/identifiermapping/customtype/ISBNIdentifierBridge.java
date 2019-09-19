/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.identifiermapping.customtype;

import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

public class ISBNIdentifierBridge implements IdentifierBridge<ISBN> {

	@Override
	public String toDocumentIdentifier(ISBN value, IdentifierBridgeToDocumentIdentifierContext context) {
		return value == null ? null : value.getStringValue();
	}

	@Override
	public ISBN fromDocumentIdentifier(String documentIdentifier,
			IdentifierBridgeFromDocumentIdentifierContext context) {
		return documentIdentifier == null ? null : ISBN.parse( documentIdentifier );
	}

}
