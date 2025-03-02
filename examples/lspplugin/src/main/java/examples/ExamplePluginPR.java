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

import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;

import dap.DAPMessageList;
import json.JSONArray;
import rpc.RPCMessageList;
import workspace.events.DAPEvent;
import workspace.events.LSPEvent;
import workspace.lenses.TCCodeLens;
import workspace.plugins.TCPlugin;

public class ExamplePluginPR extends ExamplePlugin
{
	@Override
	public RPCMessageList handleEvent(LSPEvent event) throws Exception
	{
		System.out.println("ExamplePluginPR got " + event);
		return null;
	}

	@Override
	public DAPMessageList handleEvent(DAPEvent event) throws Exception
	{
		System.out.println("ExamplePluginPR got " + event);
		return null;
	}
	
	/**
	 * To apply code lenses for PP/RT, we get the TC plugin to obtain a ClassList of
	 * type-checked classes, and then search through them for TCDefinitions that are
	 * within the File that is passed from the Client (ie. the file on screen).
	 * 
	 * This way of splitting lenses into getCodeLenses and applyCodeLenses is just
	 * a convention. The only requirement is that this method returns the lenses
	 * required.
	 */
	@Override
	public JSONArray getCodeLenses(File file)
	{
		TCPlugin tc = registry.getPlugin("TC");
		TCClassList tcClassList = tc.getTC();
		JSONArray results = new JSONArray();
		
		if (!tcClassList.isEmpty())	// May be syntax errors
		{
			List<TCCodeLens> lenses = getTCCodeLenses();
			
			for (TCClassDefinition clazz: tcClassList)
			{
				if (clazz.name.getLocation().file.equals(file))
				{
					for (TCDefinition def: clazz.definitions)	// ie. within this class/file
					{
						for (TCCodeLens lens: lenses)
						{
							results.addAll(lens.getDefinitionLenses(def, clazz));
						}
					}
				}
			}
		}
		
		return results;
	}
}
