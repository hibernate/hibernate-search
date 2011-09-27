/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.search.bridge.builtin.impl;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * It manages arrays annotated with {@link org.hibernate.search.annotations.NumericField}
 *
 * @author Davide D'Alto
 */
public class BuiltinNumericArrayBridge extends BuiltinArrayBridge {

	public BuiltinNumericArrayBridge(FieldBridge fieldBridge) {
		super( fieldBridge );
	}

	public BuiltinNumericArrayBridge() {
		super( new FieldBridge() {

			@Override
			public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
				if ( value == null ) {
					manageNull( name, document, luceneOptions );
				}
				else {
					luceneOptions.addNumericFieldToDocument( name, value, document );
				}
			}

			private void manageNull(String name, Document document, LuceneOptions luceneOptions) {
				if ( luceneOptions.indexNullAs() != null )
					luceneOptions.addFieldToDocument( name, luceneOptions.indexNullAs(), document );
			}

		} );
	}

}
