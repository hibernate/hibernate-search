/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.bridge.impl;

import java.lang.reflect.AnnotatedElement;

import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.DateBridge;
import org.hibernate.search.bridge.builtin.impl.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Built-in {@link org.hibernate.search.bridge.spi.BridgeProvider} handling date bridging
 * when {@code @DateBridge} is involved.
 * As built-in provider, no Service Loader file is used: the {@code BridgeFactory} does access it
 * after the custom bridge providers found.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class DateBridgeProvider extends ExtendedBridgeProvider {
	private static final Log LOG = LoggerFactory.make();

	private static final FieldBridge DATE_YEAR = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_YEAR );
	private static final FieldBridge DATE_MONTH = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_MONTH );
	private static final FieldBridge DATE_DAY = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_DAY );
	private static final FieldBridge DATE_HOUR = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_HOUR );
	private static final FieldBridge DATE_MINUTE = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_MINUTE );
	private static final FieldBridge DATE_SECOND = new TwoWayString2FieldBridgeAdaptor( DateBridge.DATE_SECOND );
	public static final FieldBridge DATE_MILLISECOND = new TwoWayString2FieldBridgeAdaptor(DateBridge.DATE_MILLISECOND );

	@Override
	public FieldBridge provideFieldBridge(ExtendedBridgeProviderContext context) {
		AnnotatedElement annotatedElement = context.getAnnotatedElement();
		if ( annotatedElement.isAnnotationPresent( org.hibernate.search.annotations.DateBridge.class ) ) {
			Resolution resolution = annotatedElement.getAnnotation( org.hibernate.search.annotations.DateBridge.class )
					.resolution();
			return getDateField( resolution );
		}
		return null;
	}

	private FieldBridge getDateField(Resolution resolution) {
		switch ( resolution ) {
			case YEAR:
				return DATE_YEAR;
			case MONTH:
				return DATE_MONTH;
			case DAY:
				return DATE_DAY;
			case HOUR:
				return DATE_HOUR;
			case MINUTE:
				return DATE_MINUTE;
			case SECOND:
				return DATE_SECOND;
			case MILLISECOND:
				return DATE_MILLISECOND;
			default:
				throw LOG.unknownResolution( resolution.toString() );
		}
	}
}
