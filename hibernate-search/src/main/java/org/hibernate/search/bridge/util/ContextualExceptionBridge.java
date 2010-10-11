package org.hibernate.search.bridge.util;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.BridgeException;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrap the exception with an exception provide contextual feedback
 *
 * @author Emmanuel Bernard
 */
public class ContextualExceptionBridge implements FieldBridge {
	private FieldBridge delegate;
	protected Class<?> clazz;
	protected List<String> path = new ArrayList<String>(5);
	protected String fieldName;

	public ContextualExceptionBridge setFieldBridge(FieldBridge delegate) {
		this.delegate = delegate;
		return this;
	}

	public ContextualExceptionBridge setClass(Class<?> clazz) {
		this.clazz = clazz;
		return this;
	}

	public ContextualExceptionBridge setFieldName(String fieldName) {
		this.fieldName = fieldName;
		return this;
	}

	protected BridgeException buildBridgeException(Exception e, String method) {
		StringBuilder error = new StringBuilder("Exception while calling bridge#");
		error.append(method);
		if ( clazz != null ) {
			error.append("\n\tclass: ").append( clazz.getName() );
		}
		if ( path.size() > 0 ) {
			error.append("\n\tpath: ");
			for(String pathNode : path) {
				error.append(pathNode).append(".");
			}
			error.deleteCharAt( error.length() - 1 );
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

	public ContextualExceptionBridge pushMethod(String name) {
		path.add(name);
		return this;
	}

	public ContextualExceptionBridge popMethod() {
		path.remove( path.size() - 1 );
		return this;
	}
}
