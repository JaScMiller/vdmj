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

package com.fujitsu.vdmj.in.expressions.visitors;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionUpdatableFinder;
import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INFieldExpression;
import com.fujitsu.vdmj.in.expressions.INVariableExpression;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.patterns.visitors.INBindUpdatableFinder;
import com.fujitsu.vdmj.in.patterns.visitors.INBindVisitor;
import com.fujitsu.vdmj.in.patterns.visitors.INMultipleBindUpdatableFinder;
import com.fujitsu.vdmj.in.patterns.visitors.INMultipleBindVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.UpdatableValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

/**
 * Find updatable values used within an expression.
 */
public class INExpressionUpdatableFinder extends INLeafExpressionVisitor<Value, ValueList, Context>
{
	private class VisitorSet extends INVisitorSet<Value, ValueList, Context>
	{
		private final INExpressionVisitor<ValueList, Context> expVisitor;
		private final INDefinitionVisitor<ValueList, Context> defVisitor = new INDefinitionUpdatableFinder(this);
		private final INBindVisitor<ValueList, Context> bindVisitor = new INBindUpdatableFinder(this);
		private final INMultipleBindVisitor<ValueList, Context> mbindVisitor = new INMultipleBindUpdatableFinder(this);

		public VisitorSet(INExpressionUpdatableFinder parent)
		{
			expVisitor = parent;
		}
		
		@Override
		public INDefinitionVisitor<ValueList, Context> getDefinitionVisitor()
		{
			return defVisitor;
		}

		@Override
		public INExpressionVisitor<ValueList, Context> getExpressionVisitor()
		{
			return expVisitor;
		}
		
		@Override
		public INBindVisitor<ValueList, Context> getBindVisitor()
		{
			return bindVisitor;
		}
		
		@Override
		public INMultipleBindVisitor<ValueList, Context> getMultiBindVisitor()
		{
			return mbindVisitor;
		}
	}
	
	public INExpressionUpdatableFinder()
	{
		super(false);
		visitorSet = new VisitorSet(this);
	}

	@Override
	protected ValueList newCollection()
	{
		return new ValueList();
	}
	
	@Override
	public ValueList caseVariableExpression(INVariableExpression node, Context ctxt)
	{
		Value v = ctxt.check(node.name);

		if (v == null || !(v instanceof UpdatableValue))
		{
			return new ValueList();
		}
		else
		{
			return new ValueList(v);
		}
	}

	@Override
	public ValueList caseFieldExpression(INFieldExpression node, Context ctxt)
	{
		ValueList values = node.object.apply(this, ctxt);
		
		try
		{
			// This evaluation should not affect scheduling as we are trying to
			// discover the sync variables to listen to only.
			
			ctxt.threadState.setAtomic(true);
			Value r = node.evaluate(ctxt);

			if (r instanceof UpdatableValue)
			{
				values.add(r);
			}
		}
		catch (ContextException e)
		{
			if (e.number == 4034 || e.number == 4097 || e.number == 4105)
			{
				return values;	// Non existent variable or can't get value
			}
			else
			{
				throw e;
			}
		}
		catch (ValueException e)
		{
			if (e.number == 4097 || e.number == 4105)
			{
				return values;	// Cannot get record/object value of ... 
			}
			else
			{
				ExceptionHandler.handle(new ContextException(e, node.location));
			}
		}
		finally
		{
			ctxt.threadState.setAtomic(false);
		}
		
		return values;
	}
	
	@Override
	public ValueList caseExpression(INExpression node, Context arg)
	{
		return newCollection();
	}
}
