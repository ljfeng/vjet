/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.mod.wst.jsdt.internal.core.interpret;

public class StringValue extends Value {

	String stringValue;

	public StringValue(String value) {
		super(Value.STRING);
		this.stringValue=value;
	}


	public boolean booleanValue() {
		return stringValue.length()!=0;
	}

	public int numberValue() {
		return Integer.valueOf(stringValue).intValue();
	}

	public String stringValue() {
		return stringValue;
	}
	
}
