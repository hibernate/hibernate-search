/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.checkstyle.checks.regexp;

import java.util.HashSet;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * A simple CheckStyle checker to verify specific import statements are not being used.
 *
 * @author Sanne Grinovero
 */
public class IllegalImport extends Check {

	private final HashSet<String> notAllowedImports = new HashSet<String>();
	private String message = "";

	/**
	 * Set the list of illegal import statements.
	 *
	 * @param importStatements array of illegal packages
	 */
	public void setIllegalClassnames(String[] importStatements) {
		for ( String impo : importStatements ) {
			notAllowedImports.add( impo );
		}
	}

	public void setMessage(String message) {
		if ( message != null ) {
			this.message = message;
		}
	}

	@Override
	public int[] getDefaultTokens() {
		return new int[] { TokenTypes.IMPORT, TokenTypes.STATIC_IMPORT };
	}

	@Override
	public void visitToken(DetailAST aAST) {
		final FullIdent imp;
		if ( aAST.getType() == TokenTypes.IMPORT ) {
			imp = FullIdent.createFullIdentBelow( aAST );
		}
		else {
			// handle case of static imports of method names
			imp = FullIdent.createFullIdent( aAST.getFirstChild().getNextSibling() );
		}
		final String text = imp.getText();
		if ( isIllegalImport( text ) ) {
			final String message = buildError( text );
			log( aAST.getLineNo(), aAST.getColumnNo(), message, text );
		}
	}

	private String buildError(String importStatement) {
		return "Import statement violating a checkstyle rule: " + importStatement + ". " + message;
	}

	private boolean isIllegalImport(String importString) {
		return notAllowedImports.contains( importString );
	}

}
