// $Id$
package org.hibernate.search.test.analyzer;

import org.hibernate.search.analyzer.Discriminator;

/**
 * @author Hardy Ferentschik
 */
public class LanguageDiscriminator implements Discriminator {

	public String getAnalyzerDefinitionName(Object value, Object entity, String field) {
		if ( value == null || !( entity instanceof Article ) ) {
			return null;
		}
		return (String) value;
	}
}
