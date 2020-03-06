/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCQualifiedDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCIsExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public TCType basictype;
	public final TCNameToken typename;
	public final TCExpression test;

	private TCDefinition typedef = null;

	public TCIsExpression(LexLocation location, TCNameToken typename, TCType type, TCExpression test)
	{
		super(location);
		this.basictype = type;
		this.typename = typename;
		this.test = test;
	}

	@Override
	public String toString()
	{
		return "is_(" + test + ", " + (typename == null ? basictype : typename) + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		test.typeCheck(env, null, scope, null);

		if (basictype != null)
		{
			basictype = basictype.typeResolve(env, null);
			TypeComparator.checkComposeTypes(basictype, env, false);
		}

		if (typename != null)
		{
			typedef = env.findType(typename, location.module);

			if (typedef == null)
			{
				report(3113, "Unknown type name '" + typename + "'");
			}
		}

		return checkConstraint(constraint, new TCBooleanType(location));
	}

	@Override
	public TCDefinitionList getQualifiedDefs(Environment env)
	{
		TCDefinitionList result = new TCDefinitionList();
		
		if (test instanceof TCVariableExpression)
		{
			TCVariableExpression exp = (TCVariableExpression)test;
			TCDefinition existing = env.findName(exp.name, NameScope.NAMESANDSTATE);
			
			if (existing != null && existing.nameScope.matches(NameScope.NAMES))
			{
        		if (basictype != null)
        		{
       				result.add(new TCQualifiedDefinition(existing, basictype));
        		}
        		else if (typename != null)
        		{
        			if (typedef == null)
        			{
        				typedef = env.findType(typename, location.module);
        			}

        			if (typedef != null)
        			{
        				result.add(new TCQualifiedDefinition(existing, typedef.getType()));
        			}
        		}
			}
		}
		
		return result;
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIsExpression(this, arg);
	}
}
