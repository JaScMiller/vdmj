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

package workspace;

import java.io.File;
import java.io.FilenameFilter;

import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.modules.ASTModule;
import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;

import json.JSONArray;
import lsp.textdocument.SymbolKind;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import vdmj.LSPDefinitionFinder;
import workspace.plugins.ASTPlugin;
import workspace.plugins.ASTPluginSL;
import workspace.plugins.INPluginSL;
import workspace.plugins.TCPlugin;
import workspace.plugins.TCPluginSL;

public class LSPWorkspaceManagerSL extends LSPWorkspaceManager
{
	public LSPWorkspaceManagerSL()
	{
		registry.registerPlugin(new ASTPluginSL(this));
		registry.registerPlugin(new TCPluginSL(this));
		registry.registerPlugin(new INPluginSL(this));
	}
	
	@Override
	public RPCMessageList documentSymbols(RPCRequest request, File file)
	{
		TCPlugin tc = registry.getPlugin("TC");
		TCModuleList tcModuleList = tc.getTC();
		JSONArray results = new JSONArray();
		
		if (!tcModuleList.isEmpty())	// May be syntax errors
		{
			for (TCModule module: tcModuleList)
			{
				if (module.files.contains(file))
				{
					results.add(messages.symbolInformation(module.name.toString(),
							module.name.getLocation(), SymbolKind.Module, null));

					for (TCDefinition def: module.defs)
					{
						for (TCDefinition indef: def.getDefinitions())
						{
							if (indef.name != null && indef.location.file.equals(file) && !indef.name.isOld())
							{
								results.add(messages.symbolInformation(indef.name + ":" + indef.getType(),
										indef.location, SymbolKind.kindOf(indef), indef.location.module));
							}
						}
					}
				}
			}
		}
		else
		{
			ASTPlugin ast = registry.getPlugin("AST");
			ASTModuleList astModuleList = ast.getAST();

			if (!astModuleList.isEmpty())	// May be syntax errors
			{
				for (ASTModule module: astModuleList)
				{
					if (module.files.contains(file))
					{
						results.add(messages.symbolInformation(module.name, SymbolKind.Module, null));

						for (ASTDefinition def: module.defs)
						{
							if (def.name != null && def.location.file.equals(file) && !def.name.old)
							{
								results.add(messages.symbolInformation(def.name.toString(),
										def.name.location, SymbolKind.kindOf(def), def.location.module));
							}
						}
					}
				}
			}
		}
		
		return new RPCMessageList(request, results);
	}

	@Override
	protected TCDefinition findDefinition(File file, int zline, int zcol)
	{
		TCPlugin plugin = registry.getPlugin("TC");
		TCModuleList tcModuleList = plugin.getTC();
		
		if (tcModuleList != null && !tcModuleList.isEmpty())
		{
			LSPDefinitionFinder finder = new LSPDefinitionFinder();
			return finder.findDefinition(tcModuleList, file, zline + 1, zcol + 1);
		}
		else
		{
			return null;
		}
	}

	@Override
	protected TCDefinitionList lookupDefinition(String startsWith)
	{
		TCPlugin plugin = registry.getPlugin("TC");
		TCModuleList tcModuleList = plugin.getTC();
		TCDefinitionList results = new TCDefinitionList();
		
		for (TCModule module: tcModuleList)
		{
			for (TCDefinition def: module.defs)
			{
				if (def.name != null && def.name.getName().startsWith(startsWith))
				{
					results.add(def);
				}
			}
		}
		
		return results;
	}

	@Override
	protected FilenameFilter getFilenameFilter()
	{
		return Dialect.VDM_SL.getFilter();
	}
	
	@Override
	protected String[] getFilenameFilters()
	{
		return new String[] { "**/*.vdm", "**/*.vdmsl" }; 
	}
}
