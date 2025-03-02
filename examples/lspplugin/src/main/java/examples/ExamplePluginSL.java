/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package examples;

import java.io.File;
import java.util.List;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;

import dap.DAPMessageList;
import json.JSONArray;
import rpc.RPCMessageList;
import workspace.events.DAPEvent;
import workspace.events.LSPEvent;
import workspace.lenses.TCCodeLens;
import workspace.plugins.TCPlugin;

public class ExamplePluginSL extends ExamplePlugin
{
	/**
	 * In this example, we just print out the names of the events received.
	 */
	@Override
	public RPCMessageList handleEvent(LSPEvent event) throws Exception
	{
		System.out.println("ExamplePluginSL got " + event);
		return null;
	}

	@Override
	public DAPMessageList handleEvent(DAPEvent event) throws Exception
	{
		System.out.println("ExamplePluginSL got " + event);
		return null;
	}
	
	/**
	 * To apply code lenses for SL, we get the TC plugin to obtain a ModuleList of
	 * type-checked modules, and then search through them for TCDefinitions that are
	 * within the File that is passed from the Client (ie. the file on screen).
	 * Note that the DEFAULT module can span multiple files!
	 * 
	 * This way of splitting lenses into getCodeLenses and applyCodeLenses is just
	 * a convention. The only requirement is that this method returns the lenses
	 * required.
	 */
	@Override
	public JSONArray getCodeLenses(File file)
	{
		TCPlugin tc = registry.getPlugin("TC");
		TCModuleList tcModuleList = tc.getTC();
		JSONArray results = new JSONArray();
		
		if (!tcModuleList.isEmpty())
		{
			List<TCCodeLens> lenses = getTCCodeLenses();
			
			for (TCModule module: tcModuleList)
			{
				for (TCDefinition def: module.defs)
				{
					if (def.location.file.equals(file))
					{
						for (TCCodeLens lens: lenses)
						{
							results.addAll(lens.getDefinitionLenses(def, module));
						}
					}
				}
			}
		}
		
		return results;
	}
}
