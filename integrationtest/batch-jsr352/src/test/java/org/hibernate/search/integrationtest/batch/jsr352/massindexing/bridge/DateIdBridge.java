/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.massindexing.bridge;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.search.integrationtest.batch.jsr352.massindexing.id.EmbeddableDateId;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

public class DateIdBridge implements IdentifierBridge<EmbeddableDateId> {

	private static Pattern DATE_PATTERN = Pattern.compile( "^\\d{4}-\\d{2}-\\d{2}$" );

	@Override
	public String toDocumentIdentifier(EmbeddableDateId propertyValue,
			IdentifierBridgeToDocumentIdentifierContext context) {

		return String.format( Locale.ROOT, "%04d-%02d-%02d",
				propertyValue.getYear(), propertyValue.getMonth(), propertyValue.getDay()
		);
	}

	@Override
	public EmbeddableDateId fromDocumentIdentifier(String documentIdentifier,
			IdentifierBridgeFromDocumentIdentifierContext context) {

		Matcher matcher = DATE_PATTERN.matcher( documentIdentifier );
		if ( !matcher.find() ) {
			throw new RuntimeException( "Date does not match the pattern d{4}-d{2}-d{2}: " + documentIdentifier );
		}

		EmbeddableDateId result = new EmbeddableDateId();
		result.setYear( Integer.parseInt( matcher.group( 1 ) ) );
		result.setMonth( Integer.parseInt( matcher.group( 2 ) ) );
		result.setDay( Integer.parseInt( matcher.group( 3 ) ) );
		return result;
	}
}
