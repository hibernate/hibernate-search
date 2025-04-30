/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public final class ExceptionUtils {

	private ExceptionUtils() {
	}

	public static void logError(Messager messager, Exception exception, String message) {
		logError( messager, exception, message, null );
	}

	public static void logError(Messager messager, Exception exception, String message, Element element) {
		try ( var sw = new StringWriter(); var pw = new PrintWriter( sw ) ) {
			exception.printStackTrace( pw );
			pw.flush();
			messager.printMessage( Diagnostic.Kind.ERROR, message + sw.toString(), element );
		}
		catch (IOException ex) {
			throw new RuntimeException( ex );
		}
	}
}
