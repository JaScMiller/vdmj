/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package com.fujitsu.vdmj.ast.annotations;

import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.modules.ASTModule;
import com.fujitsu.vdmj.ast.statements.ASTStatement;
import com.fujitsu.vdmj.syntax.SyntaxReader;

public abstract class ASTAnnotation
{
	public final LexIdentifierToken name;
	
	public final ASTExpressionList args;

	public ASTAnnotation(LexIdentifierToken name, ASTExpressionList args)
	{
		this.name = name;
		this.args = args;
	}

	@Override
	public String toString()
	{
		return "@" + name + (args.isEmpty() ? "" : "(" + args + ")");
	}

	public void before(SyntaxReader reader)
	{
		// Nothing by default
	}

	public void after(SyntaxReader reader, ASTDefinition def)
	{
		// Nothing by default
	}

	public void after(SyntaxReader reader, ASTStatement stmt)
	{
		// Nothing by default
	}

	public void after(SyntaxReader reader, ASTExpression exp)
	{
		// Nothing by default
	}

	public void after(SyntaxReader reader, ASTModule module)
	{
		// Nothing by default
	}
}
