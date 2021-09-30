/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.math.BigDecimal;

import org.hibernate.search.engine.cfg.spi.ParseUtils;

public final class DefaultBigDecimalBridge extends AbstractPassThroughDefaultBridge<BigDecimal> {

	@Override
	protected String toString(BigDecimal value) {
		return value.toString();
	}

	@Override
	protected BigDecimal fromString(String value) {
		return ParseUtils.parseBigDecimal( value );
	}

}
