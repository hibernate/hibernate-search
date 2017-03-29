/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.cdi.i18n;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Singleton;

/**
 * @author Yoann Rodiere
 */
@Singleton
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
		localizationMap.put( new Pair<>( value, language ), translation );
	}

	public String localize(InternationalizedValue value, Language language) {
		return localizationMap.get( new Pair<>( value, language ) );
	}

	private static class Pair<L, R> {

		private final L left;
		private final R right;

		public Pair(L left, R right) {
			super();
			this.left = left;
			this.right = right;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj != null && obj.getClass() == getClass() ) {
				Pair<?, ?> other = (Pair<?, ?>) obj;
				return Objects.equals( left, other.left )
						&& Objects.equals( right, other.right );
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash( left, right );
		}

	}
}