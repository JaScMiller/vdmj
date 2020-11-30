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

package com.fujitsu.vdmj.po.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.ProofObligationList;

public class POElseIfExpression extends POExpression
{
	private static final long serialVersionUID = 1L;
	public final POExpression elseIfExp;
	public final POExpression thenExp;

	public POElseIfExpression(LexLocation location,	POExpression elseIfExp, POExpression thenExp)
	{
		super(location);
		this.elseIfExp = elseIfExp;
		this.thenExp = thenExp;
	}

	@Override
	public String toString()
	{
		return "elseif " + elseIfExp + "\nthen " + thenExp;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ctxt.push(new POImpliesContext(elseIfExp));
		ProofObligationList obligations = thenExp.getProofObligations(ctxt);
		ctxt.pop();

		return obligations;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseElseIfExpression(this, arg);
	}
}
