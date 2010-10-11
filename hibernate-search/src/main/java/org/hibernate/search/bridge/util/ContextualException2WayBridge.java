package org.hibernate.search.bridge.util;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * Wrap the exception with an exception provide contextual feedback
 *
 * @author Emmanuel Bernard
 */
public class ContextualException2WayBridge extends ContextualExceptionBridge implements TwoWayFieldBridge {
	private TwoWayFieldBridge delegate;
	private StringBridge stringBridge;

	public ContextualException2WayBridge setFieldBridge(TwoWayFieldBridge delegate) {
		super.setFieldBridge(delegate);
		this.delegate = delegate;
		this.stringBridge = null;
		return this;
	}

	public ContextualException2WayBridge setClass(Class<?> clazz) {
		super.setClass(clazz);
		return this;
	}

	public ContextualException2WayBridge setFieldName(String fieldName) {
		super.setFieldName(fieldName);
		return this;
	}

	public Object get(String name, Document document) {
		try {
			return delegate.get(name, document);
		}
		catch (Exception e) {
			throw buildBridgeException(e, "get");
		}
	}

	public String objectToString(Object object) {
		try {
			if (delegate != null) {
				return delegate.objectToString(object);
			}
			else {
				return stringBridge.objectToString(object);
			}
		}
		catch (Exception e) {
			throw buildBridgeException(e, "objectToString");
		}
	}

	public ContextualException2WayBridge pushMethod(String name) {
		super.pushMethod(name);
		return this;
	}

	public ContextualException2WayBridge popMethod() {
		super.popMethod();
		return this;
	}

	//FIXME yuk, create a cleaner inheritance for a ContextualExceptionStringBridge
	public ContextualException2WayBridge setStringBridge(StringBridge bridge) {
		this.stringBridge = bridge;
		this.delegate = null;
		return this;
	}
}
