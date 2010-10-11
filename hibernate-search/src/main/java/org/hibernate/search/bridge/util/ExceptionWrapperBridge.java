package org.hibernate.search.bridge.util;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.BridgeException;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * Wrap the exception with an exception provide contextual feedback
 *
 * @author Emmanuel Bernard
 */
public class ExceptionWrapperBridge implements FieldBridge {
	private FieldBridge delegate;
	protected Class<?> clazz;
	protected String path;
	protected String fieldName;

	public ExceptionWrapperBridge setFieldBridge(FieldBridge delegate) {
		this.delegate = delegate;
		return this;
	}

	public ExceptionWrapperBridge setClassAndMethod(Class<?> clazz, String path) {
		this.clazz = clazz;
		this.path = path;
		return this;
	}

	public ExceptionWrapperBridge setFieldName(String fieldName) {
		this.fieldName = fieldName;
		return this;
	}

	protected BridgeException buildBridgeException(Exception e, String method) {
		StringBuilder error = new StringBuilder("Exception while calling bridge#");
		error.append(method);
		if ( clazz != null ) {
			error.append("\n\tclass: ").append(clazz);
		}
		if ( path != null ) {
			error.append("\n\tpath: ").append(path);
		}
		if ( fieldName != null ) {
			error.append("\n\tfield bridge: ").append(fieldName);
		}
		throw new BridgeException(error.toString(), e);
	}

	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		try {
			delegate.set(name, value, document, luceneOptions);
		}
		catch (Exception e) {
			throw buildBridgeException(e, "set");
		}
	}
}
