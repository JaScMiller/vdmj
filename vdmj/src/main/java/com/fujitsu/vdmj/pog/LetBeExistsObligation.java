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

package com.fujitsu.vdmj.pog;

import com.fujitsu.vdmj.po.expressions.POLetBeStExpression;
import com.fujitsu.vdmj.po.statements.POLetBeStStatement;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligation;

public class LetBeExistsObligation extends ProofObligation
{
	public LetBeExistsObligation(POLetBeStExpression exp, POContextStack ctxt)
	{
		super(exp.bind.location, POType.LET_BE_EXISTS, ctxt);
		StringBuilder sb = new StringBuilder();

		sb.append("exists ");
		sb.append(exp.bind);

		if (exp.suchThat != null)
		{
			sb.append(" & ");
			sb.append(exp.suchThat);
		}

		value = ctxt.getObligation(sb.toString());
	}

	public LetBeExistsObligation(POLetBeStStatement stmt, POContextStack ctxt)
	{
		super(stmt.bind.location, POType.LET_BE_EXISTS, ctxt);
		StringBuilder sb = new StringBuilder();

		sb.append("exists ");
		sb.append(stmt.bind);

		if (stmt.suchThat != null)
		{
			sb.append(" & ");
			sb.append(stmt.suchThat);
		}

		value = ctxt.getObligation(sb.toString());
	}
}
