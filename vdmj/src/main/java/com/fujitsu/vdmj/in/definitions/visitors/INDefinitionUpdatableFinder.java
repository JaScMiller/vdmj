/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package com.fujitsu.vdmj.in.definitions.visitors;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.visitors.INLeafDefinitionVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INDefinitionUpdatableFinder extends INLeafDefinitionVisitor<Value, ValueList, Context>
{
	public INDefinitionUpdatableFinder(INVisitorSet<Value, ValueList, Context> visitors)
	{
		visitorSet = visitors;
	}

	@Override
	protected ValueList newCollection()
	{
		return new ValueList();
	}

	@Override
	public ValueList caseDefinition(INDefinition node, Context arg)
	{
		return newCollection();
	}
}
