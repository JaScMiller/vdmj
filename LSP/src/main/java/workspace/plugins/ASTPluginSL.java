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

package workspace.plugins;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.syntax.ModuleReader;
import workspace.LSPWorkspaceManager;
import workspace.Log;

public class ASTPluginSL extends ASTPlugin
{
	private ASTModuleList astModuleList = null;
	
	public ASTPluginSL(LSPWorkspaceManager manager)
	{
		super(manager);
	}
	
	@Override
	public void preCheck()
	{
		super.preCheck();
		astModuleList = new ASTModuleList();
	}
	
	@Override
	public boolean checkLoadedFiles()
	{
		dirty = false;
		Map<File, StringBuilder> projectFiles = lspManager.getProjectFiles();
		
		for (Entry<File, StringBuilder> entry: projectFiles.entrySet())
		{
			LexTokenReader ltr = new LexTokenReader(entry.getValue().toString(),
					Dialect.VDM_SL, entry.getKey(), Charset.defaultCharset().displayName());
			ModuleReader mr = new ModuleReader(ltr);
			astModuleList.addAll(mr.readModules());
			
			if (mr.getErrorCount() > 0)
			{
				errs.addAll(mr.getErrors());
			}
			
			if (mr.getWarningCount() > 0)
			{
				warns.addAll(mr.getWarnings());
			}
		}
		
		return errs.isEmpty();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAST()
	{
		return (T)astModuleList;
	}
	
	@Override
	protected List<VDMMessage> parseFile(File file)
	{
		dirty = true;	// Until saved.

		List<VDMMessage> errs = new Vector<VDMMessage>();
		Map<File, StringBuilder> projectFiles = lspManager.getProjectFiles();
		StringBuilder buffer = projectFiles.get(file);
		
		LexTokenReader ltr = new LexTokenReader(buffer.toString(),
				Settings.dialect, file, Charset.defaultCharset().displayName());
		ModuleReader mr = new ModuleReader(ltr);
		mr.readModules();
		
		if (mr.getErrorCount() > 0)
		{
			errs.addAll(mr.getErrors());
		}
		
		if (mr.getWarningCount() > 0)
		{
			errs.addAll(mr.getWarnings());
		}

		Log.dump(errs);
		return errs;
	}
}
