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

package dap.handlers;

import java.io.IOException;

import dap.DAPHandler;
import dap.DAPMessageList;
import dap.DAPRequest;
import workspace.DAPWorkspaceManager;

public class InitializeHandler extends DAPHandler
{
	public InitializeHandler()
	{
		super();
	}

	@Override
	public DAPMessageList run(DAPRequest request) throws IOException
	{
		if ("initialize".equals(request.get("command")))
		{
			return DAPWorkspaceManager.getInstance().dapInitialize(request);
		}
		else if ("configurationDone".equals(request.get("command")))
		{
			return DAPWorkspaceManager.getInstance().configurationDone(request);
		}
		
		return new DAPMessageList(request, false, "Unexpected initialise message", null);
	}
}
