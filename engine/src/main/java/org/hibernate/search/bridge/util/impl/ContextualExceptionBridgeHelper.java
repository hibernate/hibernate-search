/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.util.impl;

import java.util.ArrayList;

import org.apache.lucene.document.Document;

import org.hibernate.search.bridge.BridgeException;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.util.StringHelper;

/**
 * Wrap the exception with an exception provide contextual feedback.
 * This class is designed to be reused, but is not threadsafe.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public final class ContextualExceptionBridgeHelper implements ConversionContext {

	private static final String IDENTIFIER = "identifier";

	// Mutable state:
	private IndexedTypeIdentifier type;
	private StringBridge stringBridge;
	private FieldBridge oneWayBridge;
	private TwoWayFieldBridge twoWayBridge;

	//Reused helpers:
	private final ArrayList<String> propertyPath = new ArrayList<String>( 5 ); //half of usual increment size as I don't expect much
	private final OneWayConversionContextImpl oneWayAdapter = new OneWayConversionContextImpl();
	private final TwoWayConversionContextImpl twoWayAdapter = new TwoWayConversionContextImpl();
	private final StringConversionContextImpl stringAdapter = new StringConversionContextImpl();

	@Override
	public ConversionContext setConvertedTypeId(IndexedTypeIdentifier type) {
		this.type = type;
		return this;
	}

	@Override
	public ConversionContext pushProperty(String propertyName) {
		propertyPath.add( propertyName );
		return this;
	}

	@Override
	public ConversionContext popProperty() {
		if ( propertyPath.size() == 0 ) {
			throw new IllegalStateException( "Trying to pop a property from an empty conversion context" );
		}
		propertyPath.remove( propertyPath.size() - 1 );
		return this;
	}

	@Override
	public ConversionContext pushIdentifierProperty() {
		pushProperty( IDENTIFIER );
		return this;
	}

	protected BridgeException buildBridgeException(Exception e, String method, String fieldName, Object bridge) {
		StringBuilder errorMessage = new StringBuilder( "Exception while calling bridge#" ).append( method );
		if ( type != null ) {
			errorMessage.append( "\n\tentity class: " ).append( type.getName() );
		}
		if ( propertyPath.size() > 0 ) {
			errorMessage.append( "\n\tentity property path: " );
			for ( String pathNode : propertyPath ) {
				errorMessage.append( pathNode ).append( "." );
			}
			errorMessage.deleteCharAt( errorMessage.length() - 1 );
		}
		if ( StringHelper.isNotEmpty( fieldName ) ) {
			errorMessage.append( "\n\tdocument field name: " ).append( fieldName );
		}

		Throwable exceptionWhenBuildingBridgeString = null;
		if ( bridge != null ) {
			String bridgeAsString = null;
			try {
				bridgeAsString = bridge.toString();
			}
			catch (Exception e2) {
				// Make sure we support failing toString() implementations (field bridges may be custom)
				exceptionWhenBuildingBridgeString = e;
				bridgeAsString = "<toString() on the fieldbridge thrown " + e.getClass() + ">";
			}
			errorMessage.append( "\n\tfield bridge: " ).append( bridgeAsString );
		}

		BridgeException exception = new BridgeException( errorMessage.toString(), e );
		if ( exceptionWhenBuildingBridgeString != null ) {
			exception.addSuppressed( exceptionWhenBuildingBridgeString );
		}

		throw exception;
	}

	@Override
	public FieldBridge oneWayConversionContext(FieldBridge delegate) {
		this.oneWayBridge = delegate;
		return oneWayAdapter;
	}

	@Override
	public TwoWayFieldBridge twoWayConversionContext(TwoWayFieldBridge delegate) {
		this.twoWayBridge = delegate;
		return twoWayAdapter;
	}

	@Override
	public StringBridge stringConversionContext(StringBridge delegate) {
		this.stringBridge = delegate;
		return stringAdapter;
	}

	private final class OneWayConversionContextImpl implements FieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			try {
				oneWayBridge.set( name, value, document, luceneOptions );
			}
			catch (RuntimeException e) {
				throw buildBridgeException( e, "set", name, oneWayBridge );
			}
		}
	}

	private final class TwoWayConversionContextImpl implements TwoWayFieldBridge {

		@Override
		public Object get(String name, Document document) {
			try {
				return twoWayBridge.get( name, document );
			}
			catch (RuntimeException e) {
				throw buildBridgeException( e, "get", name, twoWayBridge );
			}
		}

		@Override
		public String objectToString(Object object) {
			try {
				return twoWayBridge.objectToString( object );
			}
			catch (RuntimeException e) {
				throw buildBridgeException( e, "objectToString", null, twoWayBridge );
			}
		}

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			try {
				twoWayBridge.set( name, value, document, luceneOptions );
			}
			catch (RuntimeException e) {
				throw buildBridgeException( e, "set", name, twoWayBridge );
			}
		}
	}

	private final class StringConversionContextImpl implements StringBridge {

		@Override
		public String objectToString(Object object) {
			try {
				return stringBridge.objectToString( object );
			}
			catch (RuntimeException e) {
				throw buildBridgeException( e, "objectToString", null, stringBridge );
			}
		}
	}

}
