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

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.modules.INModuleList;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.tc.modules.TCModuleList;

public class INPluginSL extends INPlugin
{
	private INModuleList inModuleList = null;
	
	public INPluginSL()
	{
		super();
	}
	
	@Override
	public String getName()
	{
		return "IN";
	}

	@Override
	public void init()
	{
	}

	@Override
	public void preCheck()
	{
		inModuleList = new INModuleList();
	}
	
	@Override
	public <T> boolean checkLoadedFiles(T tcModuleList) throws Exception
	{
		inModuleList = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(tcModuleList);
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getIN()
	{
		return (T)inModuleList;
	}

	@Override
	public <T> Interpreter getInterpreter(T tcModuleList) throws Exception
	{
		return new ModuleInterpreter(inModuleList, (TCModuleList) tcModuleList);
	}
}
