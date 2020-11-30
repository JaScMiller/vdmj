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

package com.fujitsu.vdmj.commands;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INNamedTraceDefinition;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.runtime.ClassInterpreter;

/**
 * A class to read and perform class related commands from standard input.
 */
public class ClassCommandReader extends CommandReader
{
	/** A ClassInterpreter version of the interpreter field. */
	private final ClassInterpreter cinterpreter;

	public ClassCommandReader(ClassInterpreter interpreter, String prompt)
	{
		super(interpreter, prompt);
		cinterpreter = interpreter;
	}

	@Override
	protected boolean doDefault(String line) throws Exception
	{
		String parts[] = line.split("\\s+");

		if (parts.length != 2)
		{
			throw new Exception("Usage: default <default class name>");
		}

		cinterpreter.setDefaultName(parts[1]);
		println("Default class set to " + cinterpreter.getDefaultName());
		return true;
	}

	@Override
	protected boolean doClasses(String line)
	{
		String def = cinterpreter.getDefaultName();
		INClassList classes = cinterpreter.getClasses();

		for (INClassDefinition c: classes)
		{
			if (c.name.getName().equals(def))
			{
				println(c.name.getName() + " (default)");
			}
			else
			{
				println(c.name.getName());
			}
		}

		return true;
	}

	@Override
	protected boolean doCreate(String line) throws Exception
	{
		Pattern p = Pattern.compile("^create (\\w+)\\s*?:=\\s*(.+)$");
		Matcher m = p.matcher(line);

		if (m.matches())
		{
			String var = m.group(1);
			String exp = m.group(2);

			cinterpreter.create(var, exp);
		}
		else
		{
			throw new Exception("Usage: create <id> := <value>");
		}

		return true;
	}

	@Override
	protected boolean doLog(String line)
	{
		if (Settings.dialect != Dialect.VDM_RT)
		{
			return super.doLog(line);
		}

		if (line.equals("log"))
		{
			if (RTLogger.getLogSize() > 0)
			{
				println("Flushing " + RTLogger.getLogSize() + " RT events");
			}

			RTLogger.setLogfile(null);
			println("RT events now logged to the console");
			return true;
		}

		String[] parts = line.split("\\s+");

		if (parts.length != 2 || !parts[0].equals("log"))
		{
			println("Usage: log [<file> | off]");
		}
		else if (parts[1].equals("off"))
		{
			RTLogger.enable(false);
			println("RT event logging disabled");
		}
		else
		{
			try
			{
				PrintWriter p = new PrintWriter(new FileOutputStream(parts[1], false));
				RTLogger.setLogfile(p);
				println("RT events now logged to " + parts[1]);
			}
			catch (FileNotFoundException e)
			{
				println("Cannot create RT event log: " + e.getMessage());
			}
		}

		return true;
	}
	
	@Override
	protected boolean doAllTraces(String line)
	{
		int index = line.indexOf(' ') + 1;
		String pattern = (index == 0) ? ".*" : line.substring(index);

		for (INClassDefinition cdef: cinterpreter.getClasses())
		{
			if (cdef.name.getName().matches(pattern))
			{
    			for (INDefinition d: cdef.definitions)
    			{
    				if (d instanceof INNamedTraceDefinition)
    				{
    					String cmd = "runtrace " + d.name.getExplicit(true);
    					println("-------------------------------------");
    					println(cmd);
    					doRuntrace(cmd, false);
    				}
    			}
			}
		}
		
		return true;
	}

	@Override
	protected void doHelp(String line)
	{
		println("classes - list the loaded class names");
		println("default <class> - set the default class name");
		println("create <id> := <exp> - create a named variable");

		if (Settings.dialect == Dialect.VDM_RT)
		{
			println("log [<file> | off] - log RT events to file");
		}

		super.doHelp(line);
	}
}
