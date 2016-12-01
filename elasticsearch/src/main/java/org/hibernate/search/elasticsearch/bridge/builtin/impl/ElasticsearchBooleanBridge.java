/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.builtin.impl;

import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.bridge.spi.EncodingBridge;
import org.hibernate.search.bridge.spi.IgnoreAnalyzerBridge;
import org.hibernate.search.bridge.spi.NullMarker;
import org.hibernate.search.bridge.util.impl.ToStringNullMarker;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Bridge a boolean field to a {@link String}.
 *
 * <p>Behaves the same as Lucene's BooleanBridge, except the format of indexNullAs
 * strings is checked, so one cannot use a random string as null token.
 *
 * @author Sylvain Vieujot
 * @author Yoann Rodiere
 */
public class ElasticsearchBooleanBridge implements EncodingBridge, TwoWayStringBridge, IgnoreAnalyzerBridge {

	private static final Log LOG = LoggerFactory.make( Log.class );

	public static final ElasticsearchBooleanBridge INSTANCE = new ElasticsearchBooleanBridge();

	private ElasticsearchBooleanBridge() {
		// private constructor
	}

	@Override
	public Boolean stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		return Boolean.valueOf( stringValue );
	}

	@Override
	public String objectToString(Object object) {
		return object == null ?
				null :
				object.toString();
	}

	@Override
	public NumericEncodingType getEncodingType() {
		return NumericEncodingType.UNKNOWN;
	}

	protected Boolean parseIndexNullAs(String indexNullAs) throws IllegalArgumentException {
		if ( Boolean.TRUE.toString().equals( indexNullAs ) ) {
			return Boolean.TRUE;
		}
		else if ( Boolean.FALSE.toString().equals( indexNullAs ) ) {
			return Boolean.FALSE;
		}
		else {
			throw LOG.invalidNullMarkerForBoolean();
		}
	}

	@Override
	public NullMarker createNullMarker(String indexNullAs) throws IllegalArgumentException {
		Boolean booleanValue = parseIndexNullAs( indexNullAs );
		return new ToStringNullMarker( booleanValue );
	}
}
