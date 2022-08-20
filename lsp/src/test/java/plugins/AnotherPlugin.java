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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package plugins;

import rpc.RPCMessageList;
import workspace.EventHub;
import workspace.EventListener;
import workspace.events.Event;
import workspace.events.UnknownMethodEvent;
import workspace.plugins.AnalysisPlugin;

public class AnotherPlugin extends AnalysisPlugin implements EventListener
{
	public AnotherPlugin()
	{
		super();
	}
	
	@Override
	public String getName()
	{
		return "Another";
	}

	@Override
	public void init()
	{
		EventHub.getInstance().register(this, "unknownMethodEvent/slsp/another", this);
	}

	@Override
	public RPCMessageList handleEvent(Event event) throws Exception
	{
		if (event instanceof UnknownMethodEvent &&
			event.type.equals("unknownMethodEvent/slsp/another"))
		{
			return new RPCMessageList(event.request, "Handled method");
		}
		else
		{
			return null;
		}
	}
}