package org.hibernate.search.bridge.util;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * Wrap the exception with an exception provide contextual feedback
 *
 * @author Emmanuel Bernard
 */
public class ExceptionWrapper2WayBridge extends ExceptionWrapperBridge implements TwoWayFieldBridge {
	private TwoWayFieldBridge delegate;

	public ExceptionWrapper2WayBridge setFieldBridge(TwoWayFieldBridge delegate) {
		super.setFieldBridge(delegate);
		this.delegate = delegate;
		return this;
	}

	public ExceptionWrapper2WayBridge setClass(Class<?> clazz) {
		super.setClass(clazz);
		return this;
	}

	public ExceptionWrapper2WayBridge setFieldName(String fieldName) {
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
			return delegate.objectToString(object);
		}
		catch (Exception e) {
			throw buildBridgeException(e, "objectToString");
		}
	}

	public ExceptionWrapper2WayBridge pushMethod(String name) {
		super.pushMethod(name);
		return this;
	}

	public ExceptionWrapper2WayBridge popMethod() {
		super.popMethod();
		return this;
	}
}
