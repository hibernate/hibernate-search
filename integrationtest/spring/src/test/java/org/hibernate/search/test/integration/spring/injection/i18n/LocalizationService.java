/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.spring.injection.i18n;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

/**
 * @author Yoann Rodiere
 */
@Service
public class LocalizationService {

	private final Map<Pair<InternationalizedValue, Language>, String> localizationMap = new HashMap<>();

	public LocalizationService() {
		register( InternationalizedValue.HELLO, Language.ENGLISH, "hello" );
		register( InternationalizedValue.GOODBYE, Language.ENGLISH, "goodbye" );
		register( InternationalizedValue.HELLO, Language.GERMAN, "hallo" );
		register( InternationalizedValue.GOODBYE, Language.GERMAN, "auf wiedersehen" );
		register( InternationalizedValue.HELLO, Language.FRENCH, "bonjour" );
		register( InternationalizedValue.GOODBYE, Language.FRENCH, "au revoir" );
	}

	private void register(InternationalizedValue value, Language language, String translation) {
		localizationMap.put( Pair.of( value, language ), translation );
	}

	public String localize(InternationalizedValue value, Language language) {
		return localizationMap.get( Pair.of( value, language ) );
	}
}