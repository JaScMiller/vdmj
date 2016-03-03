/*******************************************************************************
 *
 *	Copyright (C) 2008 Fujitsu Services Ltd.
 *
 *	Author: Nick Battle
 *
 *	This file is part of VDMJ.
 *	
 *	VDMJ is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	VDMJ is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package org.overturetool.vdmj.typechecker;

import java.util.List;
import java.util.Vector;

import org.overturetool.vdmj.lex.LexLocation;

/**
 * A type checking exception.
 */

@SuppressWarnings("serial")
public class TypeCheckException extends RuntimeException
{
	public final LexLocation location;
	public List<TypeCheckException> extras = null;

	public TypeCheckException(String msg, LexLocation location)
	{
		super(msg);
		this.location = location;
	}
	
	public void addExtra(TypeCheckException e)
	{
		if (extras == null)
		{
			extras = new Vector<TypeCheckException>();
		}
		
		extras.add(e);
	}
	
	public void addExtras(List<TypeCheckException> es)
	{
		if (extras == null)
		{
			extras = new Vector<TypeCheckException>();
		}
		
		extras.addAll(es);
	}
}
