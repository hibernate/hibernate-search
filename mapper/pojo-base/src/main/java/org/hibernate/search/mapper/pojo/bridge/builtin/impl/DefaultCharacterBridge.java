/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultCharacterBridge extends AbstractStringBasedDefaultBridge<Character> {

	public static final DefaultCharacterBridge INSTANCE = new DefaultCharacterBridge();

	private DefaultCharacterBridge() {
	}

	@Override
	protected String toString(Character value) {
		// The character is turned into a one character String
		return value.toString();
	}

	@Override
	protected Character fromString(String value) {
		return ParseUtils.parseCharacter( value );
	}

}
