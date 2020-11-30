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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.VDMJ;
import com.fujitsu.vdmj.in.expressions.INBinaryExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.debug.ConsoleDebugReader;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.ConsolePrintWriter;
import com.fujitsu.vdmj.messages.ConsoleWriter;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.messages.VDMErrorsException;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.DebuggerException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.SourceFile;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.traces.TraceReductionType;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.Value;

/**
 * A class to read and perform commands from standard input.
 */
abstract public class CommandReader
{
	/** The interpreter to use for the execution of commands. */
	protected final Interpreter interpreter;

	/** The prompt for the user. */
	protected final String prompt;

	/** The degree of trace reduction. */
	private float reduction = 1.0F;

	/** The type of trace reduction. */
	private TraceReductionType reductionType = TraceReductionType.NONE;
	
	/** The output file for trace execution or null */
	private File traceoutput = null;

	/** The seed for the trace PRNG */
	private long traceseed = 0;
	
	/** The cache of loaded plugin instances */
	private Map<String, CommandPlugin> plugins = new HashMap<String, CommandPlugin>();

	/** The script command input file, if any */
	private BufferedReader scriptReader = null;
	
	/** The script command filename, if any */
	private File scriptFile = null;

	/**
	 * Create a command reader with the given interpreter and prompt.
	 *
	 * @param interpreter The interpreter instance to use.
	 * @param prompt The user prompt.
	 */
	public CommandReader(Interpreter interpreter, String prompt)
	{
		this.interpreter = interpreter;
		this.prompt = prompt;
	}

	/**
	 * Read and execute commands from standard input. The prompt passed
	 * to the constructor is used to prompt the user, and the interpreter
	 * passed is used to execute commands. Each command is directed through
	 * the corresponding "do" method in this class, the defaults for which
	 * print that the command is not available from the current context.
	 * Subclasses of CommandReader implement the "do" methods that apply
	 * to them. The only methods implemented at this level are the ones
	 * which are globally applicable.
	 * <p>
	 * The "do" methods return a boolean which indicates whether the command
	 * reader should loop and read/dispatch more commands, or exit.
	 */
	public ExitStatus run(List<File> filenames)
	{
		String line = "";
		String lastline = "";
		boolean carryOn = true;
		long timestamp = System.currentTimeMillis();

		while (carryOn)
		{
			for (File file: filenames)
			{
				if (file.lastModified() > timestamp)
				{
					println("File " + file + " has changed");
				}
			}

			try
			{
				prompt();
				line = readLine();

				if (line == null)
				{
					carryOn = doQuit("");
					continue;
				}
				
				line = line.trim();

				if (line.equals("."))
				{
					line = lastline;
					println(prompt + line);
				}

				if (line.equals("") || line.startsWith("--"))
				{
					continue;
				}

				if (scriptReader == null)	// Don't remember script lines!
				{
					lastline = line;
				}

				if (line.equals("quit") || line.equals("q"))
				{
					carryOn = doQuit(line);
				}
				else if (line.startsWith("reload"))
				{
					carryOn = doReLoad(line);

					if (!carryOn)
					{
						return ExitStatus.RELOAD;
					}
				}
				else if (line.startsWith("load"))
				{
					carryOn = doLoad(line, filenames);

					if (!carryOn)
					{
						return ExitStatus.RELOAD;
					}
				}
				else if (line.equals("files"))
				{
					carryOn = doFiles();
				}
				else if (line.startsWith("set"))
				{
					carryOn = doSet(line);
				}
				else if (line.equals("help") || line.equals("?"))
				{
					doHelp(line);
				}
				else if (line.startsWith("assert"))
				{
					carryOn = doAssert(line);
				}
				else if (line.startsWith("script"))
				{
					carryOn = doScript(line);
				}
				else if(line.startsWith("trace"))
				{
					carryOn = doTrace(line);
				}
				else if(line.startsWith("break"))
				{
					carryOn = doBreak(line);
				}
				else if(line.equals("list"))
				{
					carryOn = doList(line);
				}
				else if(line.equals("source"))
				{
					carryOn = doSource(line);
				}
				else if(line.startsWith("coverage"))
				{
					carryOn = doCoverage(line);
				}
				else if(line.startsWith("latexdoc"))
				{
					carryOn = doLatex(line, true);
				}
				else if(line.startsWith("latex"))
				{
					carryOn = doLatex(line, false);
				}
				else if(line.startsWith("word"))
				{
					carryOn = doWord(line);
				}
				else if(line.startsWith("remove"))
				{
					carryOn = doRemove(line);
				}
				else if(line.equals("init"))
				{
					carryOn = doInit(line);
				}
				else if(line.equals("env"))
				{
					carryOn = doEnv(line);
				}
				else if(line.equals("state"))
				{
					carryOn = doState(line);
				}
				else if(line.startsWith("default"))
				{
					carryOn = doDefault(line);
				}
				else if(line.equals("classes"))
				{
					carryOn = doClasses(line);
				}
				else if(line.startsWith("create"))
				{
					carryOn = doCreate(line);
				}
				else if(line.equals("modules"))
				{
					carryOn = doModules(line);
				}
				else if(line.startsWith("pog"))
				{
					carryOn = doPog(line);
				}
				else if(line.startsWith("log"))
				{
					carryOn = doLog(line);
				}
				else if (line.startsWith("print ") || line.startsWith("p "))
				{
					carryOn = doEvaluate(line);
				}
				else if (line.startsWith("runtrace ") || line.startsWith("rt "))
				{
					carryOn = doRuntrace(line, false);
				}
				else if (line.startsWith("debugtrace ") || line.startsWith("dt "))
				{
					carryOn = doRuntrace(line, true);
				}
				else if (line.startsWith("runalltraces"))
				{
					carryOn = doAllTraces(line);
				}
				else if (line.startsWith("savetrace"))
				{
					carryOn = doSavetrace(line);
				}
				else if (line.startsWith("seedtrace"))
				{
					carryOn = doSeedtrace(line);
				}
				else if (line.startsWith("save"))
				{
					carryOn = doSave(line);
				}
				else if (line.startsWith("filter"))
				{
					carryOn = doFilter(line);
				}
				else if (!usePlugin(line))		// Attempt to load plugin
				{
					println("Bad command. Try 'help'");
				}
			}
			catch (Exception e)
			{
				carryOn = doException(e);
			}
		}

		return ExitStatus.EXIT_OK;
	}
	
	private void prompt()
	{
		if (scriptFile == null)
		{
			print(prompt);
		}
		else
		{
			print(scriptFile.getName() + prompt);
		}
	}
	
	private String readLine() throws IOException
	{
		StringBuilder line = new StringBuilder();
		line.append("\\");
		
		do
		{
			line.deleteCharAt(line.length() - 1);	// Remove trailing backslash
			
			if (scriptReader != null)
			{
				String part = scriptReader.readLine();
				
				if (part != null)
				{
					line.append(part);
					println(part);
				}
				else
				{
					scriptReader.close();
					scriptReader = null;	// EOF
					scriptFile = null;
					println("END");
					break;
				}
			}
			else
			{
				line.append(Console.in.readLine());
			}
		}
		while (line.length() > 0 && line.charAt(line.length() - 1) == '\\');
		
		return line.toString();
	}

	private boolean usePlugin(String line) throws Exception
	{
		String[] argv = line.split("\\s+");
		CommandPlugin cmd = plugins.get(argv[0]);
		
		if (cmd != null)
		{
			return cmd.run(argv);
		}
		
		String plugin = Character.toUpperCase(argv[0].charAt(0)) + argv[0].substring(1).toLowerCase() + "Plugin";
		String[] packages = System.getProperty("vdmj.plugins", "plugins").split(";|:");
		
		for (String pack: packages)
		{
			try
			{
				Class<?> clazz = Class.forName(pack + "." + plugin);

				if (CommandPlugin.class.isAssignableFrom(clazz))
				{
					Constructor<?> ctor = clazz.getConstructor(Interpreter.class);
					cmd = (CommandPlugin)ctor.newInstance(interpreter);
					plugins.put(argv[0], cmd);
					return cmd.run(argv);
				}
			}
			catch (ClassNotFoundException e)
			{
				// Try next package
			}
			catch (InstantiationException e)
			{
				// Try next package
			}
			catch (IllegalAccessException e)
			{
				// Try next package
			}
		}

		return false;
	}

	protected boolean doException(Exception e)
	{
		while (e instanceof InvocationTargetException)
		{
			e = (Exception)e.getCause();
		}
		
		println("Exception: " + e.getMessage());
		return true;
	}

	protected boolean doEvaluate(String line)
	{
		line = line.substring(line.indexOf(' ') + 1);
		ConsoleDebugReader dbg = null;
		
		try
		{
			dbg = new ConsoleDebugReader();
			dbg.start();
			
   			long before = System.currentTimeMillis();
   			println("= " + interpreter.execute(line));
   			long after = System.currentTimeMillis();
			println("Executed in " + (double)(after-before)/1000 + " secs. ");

			if (RTLogger.getLogSize() > 0)
			{
				println("Dumped RT events");
				RTLogger.dump(false);
			}
		}
		catch (ParserException e)
		{
			println("Syntax: " + e.getMessage());
		}
		catch (DebuggerException e)
		{
			println("Debug: " + e.getMessage());
		}
		catch (RuntimeException e)
		{
			println("Runtime: " + e);
		}
		catch (VDMErrorsException e)
		{
			println(e.toString());
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			println("Error: " + e.getMessage());
		}
		finally
		{
			if (dbg != null)
			{
				dbg.interrupt();	// Stop the debugger reader.
			}
		}

		return true;
	}

	protected boolean doFilter(String line)
	{
		String[] parts = line.split("\\s+");

		if (parts.length != 2)
		{
			println("Usage: filter %age | RANDOM | SHAPES_NOVARS | SHAPES_VARNAMES | SHAPES_VARVALUES");
		}
		else
		{
			try
			{
				float val = Float.parseFloat(parts[1]) / 100.0F;

				if (val > 1 || val <= 0)
				{
					throw new NumberFormatException("Should be 1-100");
				}
				
				reduction = val;
				
				if (reductionType == TraceReductionType.NONE)
				{
					reductionType = TraceReductionType.RANDOM;
				}
			}
			catch (NumberFormatException e)
			{
				try
				{
					reductionType = TraceReductionType.valueOf(parts[1].toUpperCase());
					
					if (reductionType == TraceReductionType.NONE)
					{
						reduction = (float)1.0;
					}
				}
				catch (Exception e1)
				{
					println("Usage: filter %age | RANDOM | SHAPES_NOVARS | SHAPES_VARNAMES | SHAPES_VARVALUES");
				}
			}
		}

		println("Trace filter currently " + reduction*100 + "% " + reductionType + " (seed " + traceseed + ")");
		return true;
	}

	protected boolean doRuntrace(String line, boolean debug)
	{
		String[] parts = line.split("\\s+");
		int startTest = 0;
		int endTest = 0;

		if (parts.length == 3)
		{
			try
			{
				startTest = Integer.parseInt(parts[2]);
				endTest = startTest;
			}
			catch (NumberFormatException e)
			{
				println(parts[0] + " <name> [start number]");
				return true;
			}
		}
		else if (parts.length == 4)
		{
			try
			{
				startTest = Integer.parseInt(parts[2]);
				endTest = parts[3].equalsIgnoreCase("end") ? 0 : Integer.parseInt(parts[3]);
			}
			catch (NumberFormatException e)
			{
				println(parts[0] + " <name> [start number [end number or \"end\"]]");
				return true;
			}
		}

		line = parts[1];

		try
		{
			ConsoleWriter out = null;
			
			if (!debug && traceoutput != null)
			{
				try
				{
					out = new ConsolePrintWriter(traceoutput);
					Interpreter.setTraceOutput(out);
					println("Trace output sent to " + traceoutput);
				}
				catch (Exception e)
				{
					println("Cannot create output file " + traceoutput);
					return true;
				}
			}
			
			ConsoleDebugReader dbg = null;
			
			try
			{
				if (debug)
				{
					dbg = new ConsoleDebugReader();
					dbg.start();
				}

				boolean passed = interpreter.runtrace(line, startTest, endTest, debug, reduction, reductionType, traceseed);
    			
    			if (!debug && traceoutput != null)
    			{
    				out.close();
    				Interpreter.setTraceOutput(null);
    			}
    
    			if (passed)
    			{
    				println("All tests passed");
    			}
    			else
    			{
    				println("Some tests failed or indeterminate");
    			}
			}
			finally
			{
				if (debug)
				{
					dbg.interrupt();
				}
			}

			if (RTLogger.getLogSize() > 0)
			{
				println("Dumped RT events");
				RTLogger.dump(false);
			}
		}
		catch (ParserException e)
		{
			println("Syntax: " + e.getMessage());
		}
		catch (DebuggerException e)
		{
			println("Debug: " + e.getMessage());
		}
		catch (RuntimeException e)
		{
			println("Runtime: " + e);
		}
		catch (VDMErrorsException e)
		{
			println(e.toString());
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			println("Error: " + e.getMessage());
		}

		return true;
	}
	
	protected boolean doSavetrace(String line)
	{
		String[] parts = line.split("\\s+");
		
		if (parts.length == 1)
		{
			if (traceoutput == null)
			{
				println("runtrace output is not redirected");
			}
			else
			{
				println("runtrace output is redirected to " + traceoutput);
			}
		}
		else if (parts.length != 2)
		{
			println("savetrace <file> | OFF");
		}
		else
		{
			if (parts[1].equalsIgnoreCase("off"))
			{
				traceoutput = null;
				println("runtrace output is not redirected");
			}
			else
			{
				traceoutput = new File(parts[1]);
				println("runtrace output redirected to " + traceoutput);
			}
		}

		return true;
	}

	protected boolean doSeedtrace(String line)
	{
		String[] parts = line.split("\\s+");
		
		if (parts.length != 2)
		{
			println("seedtrace <number>");
		}
		else
		{
			try
			{
				traceseed = Long.parseLong(parts[1]);
				println("Seed now set to " + traceseed);
			}
			catch (NumberFormatException e)
			{
				println("seedtrace <number>");
			}
		}
		
		return true;
	}

	protected boolean doAllTraces(String line)
	{
		return notAvailable(line);
	}

	protected boolean doQuit(@SuppressWarnings("unused") String line)
	{
		if (RTLogger.getLogSize() > 0)
		{
			println("Dumping RT events");
			RTLogger.dump(true);
		}

		return false;
	}

	protected boolean doModules(String line)
	{
		return notAvailable(line);
	}

	protected boolean doClasses(String line)
	{
		return notAvailable(line);
	}

	protected boolean doFiles()
	{
		Set<File> filenames = interpreter.getSourceFiles();

		for (File file: filenames)
		{
			println(file.getPath());
		}

		return true;
	}

	private void isEnabled(String name, boolean flag)
	{
		print(name);
		println(flag ? " are enabled" : " are disabled");
	}

	protected boolean doSet(String line)
	{
		if (line.equals("set"))
		{
			isEnabled("Preconditions", Settings.prechecks);
			isEnabled("Postconditions", Settings.postchecks);
			isEnabled("Invariants", Settings.invchecks);
			isEnabled("Dynamic type checks", Settings.dynamictypechecks);
			isEnabled("Pre/post/inv exceptions", Settings.exceptions);
			isEnabled("Measure checks", Settings.measureChecks);
			isEnabled("Annotations", Settings.annotations);
		}
		else
		{
			String[] parts = line.split("\\s+");

			if (parts.length == 3 &&
				(parts[2].equalsIgnoreCase("on") || parts[2].equalsIgnoreCase("off")))
			{
				boolean setting = parts[2].equalsIgnoreCase("on");

	    		if (parts[1].equals("pre"))
	    		{
	    			Settings.prechecks = setting;
	    		}
	    		else if (parts[1].equals("post"))
	    		{
	    			Settings.postchecks = setting;
	    		}
	    		else if (parts[1].equals("inv"))
	    		{
	    			Settings.invchecks = setting;
	    		}
	    		else if (parts[1].equals("dtc"))
	    		{
	    			// NB. Do both
	    			Settings.invchecks = setting;
	    			Settings.dynamictypechecks = setting;
	    		}
	    		else if (parts[1].equals("exceptions"))
	    		{
	    			Settings.exceptions = setting;
	    		}
	    		else if (parts[1].equals("measures"))
	    		{
	    			Settings.measureChecks = setting;
	    		}
	    		else if (parts[1].equals("annotations"))
	    		{
	    			if (setting != Settings.annotations)
	    			{
		    			println("Specification must now be re-parsed (reload)");
	    			}

	    			Settings.annotations = setting;
	    		}
				else
				{
					println("Usage: set [<pre|post|inv|dtc|exceptions|measures|annotations> <on|off>]");
				}
			}
			else
			{
				println("Usage: set [<pre|post|inv|dtc|exceptions|measures|annotations> <on|off>]");
			}
		}

		return true;
	}

	protected boolean doPog(String line) throws Exception
	{
		ProofObligationList all = interpreter.getProofObligations();
		ProofObligationList list = null;

		if (line.equals("pog"))
		{
			list = all;
		}
		else
		{
    		Pattern p1 = Pattern.compile("^pog (\\w+)$");
    		Matcher m = p1.matcher(line);

    		if (m.matches())
    		{
    			list = new ProofObligationList();
    			String name = m.group(1) + (Settings.dialect == Dialect.VDM_SL ? "" : "(");

    			for (ProofObligation po: all)
    			{
    				if (po.name.indexOf(name) >= 0)
    				{
    					list.add(po);
    				}
    			}
    		}
    		else
    		{
    			println("Usage: pog [<fn/op name>]");
    			return true;
    		}
		}

		if (list.isEmpty())
		{
			println("No proof obligations generated");
		}
		else
		{
			println("Generated " +
				plural(list.size(), "proof obligation", "s") + ":\n");
			print(list.toString());
		}

		return true;
	}

	protected boolean doLog(String line)
	{
		return notAvailable(line);
	}

	/**
	 * @throws Exception
	 */
	protected boolean doCreate(String line) throws Exception
	{
		return notAvailable(line);
	}

	/**
	 * @throws Exception
	 */
	protected boolean doDefault(String line) throws Exception
	{
		return notAvailable(line);
	}

	protected boolean doInit(@SuppressWarnings("unused") String line)
	{
		LexLocation.clearLocations();
		println("Cleared all coverage information");
		ConsoleDebugReader dbg = null;

		try
		{
			dbg = new ConsoleDebugReader();
			dbg.start();
			interpreter.init();
		}
		catch (Exception e)
		{
			println("Initialization failed: " + e.getMessage());
		}
		finally
		{
			if (dbg != null)
			{
				dbg.interrupt();
			}
		}
		
		println("Global context initialized");
		return true;
	}

	protected boolean doEnv(@SuppressWarnings("unused") String line)
	{
		print(interpreter.getInitialContext().toString());
		return true;
	}

	protected boolean doState(String line)
	{
		return notAvailable(line);
	}

	protected boolean doRemove(String line)
	{
		String parts[] = line.split("\\s+");

		if (parts.length != 2)
		{
			println("Usage: remove <breakpoint#>");
			return true;
		}

		int bpno = Integer.parseInt(parts[1]);
		Breakpoint old = interpreter.clearBreakpoint(bpno);

		if (old != null)
		{
			println("Cleared " + old);
			println(interpreter.getSourceLine(old.location));
		}
		else
		{
			println("Breakpoint [" + bpno + "] not set");
		}

		return true;
	}

	protected boolean doList(@SuppressWarnings("unused") String line)
	{
		Map<Integer, Breakpoint> map = interpreter.getBreakpoints();

		for (Entry<Integer, Breakpoint> entry: map.entrySet())
		{
			Breakpoint bp = entry.getValue();
			println(bp.toString());
			println(interpreter.getSourceLine(bp.location));
		}

		return true;
	}

	protected boolean doSource(String line)
	{
		return notAvailable(line);
	}

	protected boolean doCoverage(String line)
	{
		try
		{
			if (line.equals("coverage"))
			{
				for (File file: interpreter.getSourceFiles())
				{
					doCoverage(file);
				}

				return true;
			}

			String[] parts = line.split("\\s+");

			if (parts.length == 2 && parts[1].equals("clear"))
			{
				LexLocation.clearLocations();
				println("Cleared all coverage information");
				return true;
			}

			if (parts.length == 3 && parts[1].equals("write"))
			{
				writeCoverage(new File(parts[2]));
				return true;
			}

			if (parts.length == 3 && parts[1].equals("merge"))
			{
				mergeCoverage(new File(parts[2]));
				return true;
			}

			for (int p = 1; p < parts.length; p++)
			{
				File farg = new File(parts[p]);
				boolean done = false;
				
				for (File file: interpreter.getSourceFiles())
    			{
					if (file.getCanonicalFile().equals(farg.getCanonicalFile()))
					{
						doCoverage(file);	// NB. don't use canonical files
						done = true;
						break;
					}
    			}

				if (!done)
    			{
    				println(farg + " is not loaded - try 'files'");
    			}
			}
		}
		catch (Exception e)
		{
			println("Usage: coverage clear|write <dir>|merge <dir>|<filenames>");
		}

		return true;
	}

	protected boolean doCoverage(File file)
	{
		try
		{
			SourceFile source = interpreter.getSourceFile(file);

			if (source == null)
			{
				println(file + ": file not found");
			}
			else
			{
				source.printCoverage(Console.out);
			}
		}
		catch (Exception e)
		{
			println("coverage: " + e.getMessage());
		}

		return true;
	}

	protected boolean doWord(String line)
	{
		try
		{
			if (line.equals("word"))
			{
				for (File file: interpreter.getSourceFiles())
				{
					doWord(file);
				}

				return true;
			}

			String[] parts = line.split("\\s+");

			for (int p = 1; p < parts.length; p++)
			{
				File farg = new File(parts[p]);
				boolean done = false;
				
				for (File file: interpreter.getSourceFiles())
    			{
					if (file.getCanonicalFile().equals(farg.getCanonicalFile()))
					{
						doWord(file);	// NB. don't use canonical files
						done = true;
						break;
					}
    			}

				if (!done)
    			{
    				println(farg + " is not loaded - try 'files'");
    			}
			}
		}
		catch (Exception e)
		{
			println("Usage: word [<filenames>]");
		}

		return true;
	}

	protected void doWord(File file)
	{
		try
		{
			SourceFile source = interpreter.getSourceFile(file);

			if (source == null)
			{
				println(file + ": file not found");
			}
			else
			{
				File html = new File(source.filename.getPath() + ".doc");
				PrintWriter pw = new PrintWriter(html, "UTF-8");
				source.printWordCoverage(pw);
				pw.close();
				println("Word HTML coverage written to " + html);
			}
		}
		catch (Exception e)
		{
			println("word: " + e.getMessage());
		}
	}

	protected boolean doSave(String line)
	{
		try
		{
			if (line.equals("save"))
			{
				for (File file: interpreter.getSourceFiles())
				{
					doSave(file);
				}

				return true;
			}

			String[] parts = line.split("\\s+");

			for (int p = 1; p < parts.length; p++)
			{
				File farg = new File(parts[p]);
				boolean done = false;
				
				for (File file: interpreter.getSourceFiles())
    			{
					if (file.getCanonicalFile().equals(farg.getCanonicalFile()))
					{
						doSave(file);	// NB. don't use canonical files
						done = true;
						break;
					}
    			}

				if (!done)
    			{
    				println(farg + " is not loaded - try 'files'");
    			}
			}
		}
		catch (Exception e)
		{
			println("Usage: save [<filenames>]");
		}

		return true;
	}

	protected void doSave(File file)
	{
		try
		{
			String name = file.getName().toLowerCase();

			if (name.endsWith(".doc") ||
				name.endsWith(".docx") ||
				name.endsWith(".odt"))
			{
    			SourceFile source = interpreter.getSourceFile(file);

    			if (source == null)
    			{
    				println(file + ": file not found");
    			}
    			else
    			{
    				File vdm = new File(source.filename.getPath() + "." +
    					Settings.dialect.getArgstring().substring(1));
    				PrintWriter spw = new PrintWriter(vdm, "UTF-8");
    				source.printSource(spw);
    				spw.close();
    				println("Extracted source written to " + vdm);
    			}
			}
			else
			{
				println("Not a Word or ODF file: " + file);
			}
		}
		catch (Exception e)
		{
			println("save: " + e.getMessage());
		}
	}

	protected boolean doLatex(String line, boolean headers)
	{
		try
		{
			if (line.equals("latex") || line.equals("latexdoc"))
			{
				for (File file: interpreter.getSourceFiles())
				{
					doLatex(file, headers);
				}

				return true;
			}

			String[] parts = line.split("\\s+");

			for (int p = 1; p < parts.length; p++)
			{
				File farg = new File(parts[p]);
				boolean done = false;
				
				for (File file: interpreter.getSourceFiles())
    			{
					if (file.getCanonicalFile().equals(farg.getCanonicalFile()))
					{
						doLatex(file, headers);	// NB. don't use canonical files
						done = true;
						break;
					}
    			}

				if (!done)
    			{
    				println(farg + " is not loaded - try 'files'");
    			}
			}
		}
		catch (Exception e)
		{
			println("Usage: latex|latexdoc <filenames>");
		}

		return true;
	}

	protected boolean doLatex(File file, boolean headers)
	{
		try
		{
			SourceFile source = interpreter.getSourceFile(file);

			if (source == null)
			{
				println(file + ": file not found");
			}
			else
			{
				File tex = new File(source.filename.getPath() + ".tex");
				PrintWriter pw = new PrintWriter(tex, "UTF-8");
				source.printLatexCoverage(pw, headers);
				pw.close();
				println("Latex coverage written to " + tex);
			}
		}
		catch (Exception e)
		{
			println("latex: " + e.getMessage());
		}

		return true;
	}

	protected boolean doBreak(String line) throws Exception
	{
		Pattern p1 = Pattern.compile("^break ([\\w._/\\\\]++)?:?(\\d+) ?(.+)?$");
		Matcher m = p1.matcher(line);

		if (m.matches())
		{
			String g1 = m.group(1);
			File file = g1 == null ? null : new File(g1);
			setBreakpoint(file, Integer.parseInt(m.group(2)), m.group(3));
		}
		else
		{
			Pattern p2 = Pattern.compile("^break ([\\w`$%']+) ?(.+)?$");
			m = p2.matcher(line);

			if (m.matches())
			{
				setBreakpoint(m.group(1), m.group(2));
			}
			else
			{
	    		println("Usage: break [<file>:]<lineno> [<condition>]");
	    		println("   or: break <function/operation> [<condition>]");
			}
		}

		return true;
	}

	protected boolean doTrace(String line) throws Exception
	{
		Pattern p1 = Pattern.compile("^trace ([\\w._/\\\\]++)?:?(\\d+) ?(.+)?$");
		Matcher m = p1.matcher(line);

		if (m.matches())
		{
			String g1 = m.group(1);
			File file = g1 == null ? null : new File(g1);
			setTracepoint(file, Integer.parseInt(m.group(2)), m.group(3));
		}
		else
		{
			Pattern p2 = Pattern.compile("^trace ([\\w`$%']+) ?(.+)?$");
			m = p2.matcher(line);

			if (m.matches())
			{
				setTracepoint(m.group(1), m.group(2));
			}
			else
			{
	    		println("Usage: trace [<file>:]<lineno> [<expression>]");
	    		println("   or: trace <function/operation> [<expression>]");
			}
		}

		return true;
	}

	protected boolean doAssert(String line)
	{
		File filename = null;

		try
		{
			String[] parts = line.split("\\s+");
			filename = new File(parts[1]);

			if (!filename.canRead())
			{
				println("File '" + filename + "' not accessible");
				return true;
			}
		}
		catch (Exception e)
		{
			println("Usage: assert <filename>");
			return true;
		}

		assertFile(filename);
		return true;
	}

	protected boolean doScript(String line)
	{
		if (scriptReader != null)
		{
			println("Cannot call a script within a script!");
			return true;
		}
		
		try
		{
			String[] parts = line.split("\\s+");
			
			if (parts.length != 2)
			{
				println("Usage: script <filename>");
				scriptReader = null;
				scriptFile = null;
			}
			else
			{
				scriptFile = new File(parts[1]);
				scriptReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(scriptFile), VDMJ.filecharset));
			}
		}
		catch (Exception e)
		{
			println("Cannot read file: " + e.getMessage());
			scriptReader = null;
			scriptFile = null;
		}

		return true;
	}

	protected boolean doReLoad(String line)
	{
		if (!line.equals("reload"))
		{
			println("Usage: reload");
			return true;
		}

		return false;
	}

	protected boolean doLoad(String line, List<File> filenames)
	{
		if (line.indexOf(' ') < 0)
		{
			println("Usage: load <files or dirs>");
			return true;
		}

		filenames.clear();

		for (String s: line.split("\\s+"))
		{
			File dir = new File(s);

			if (dir.isDirectory())
			{
				for (File file: dir.listFiles(Settings.dialect.getFilter()))
				{
					if (file.isFile())
					{
						filenames.add(file);
					}
				}
			}
			else
			{
				filenames.add(dir);
			}
		}

		filenames.remove(0);	// which is "load" :-)
		return false;
	}

	public boolean assertFile(File filename)
	{
		BufferedReader input = null;

		try
		{
			input = new BufferedReader(new InputStreamReader(new FileInputStream(filename), VDMJ.filecharset));
		}
		catch (FileNotFoundException e)
		{
			println("File '" + filename + "' not found");
			return false;
		}
		catch (UnsupportedEncodingException e)
		{
			println("File encoding exception: " + e);
			return false;
		}

		int assertErrors = 0;
		int assertPasses = 0;

		while (true)
		{
			String assertion = null;

			try
			{
				assertion = input.readLine();

				if (assertion == null)
				{
					break;	 // EOF
				}

				if (assertion.equals("") || assertion.startsWith("--"))
				{
					continue;
				}

	   			Value result = interpreter.execute(assertion);

	   			if (!(result instanceof BooleanValue) || !result.boolValue(null))
   				{
   					println("FAILED: " + assertion);
   					assertErrors++;
   				}
   				else
   				{
   					assertPasses++;
   				}
			}
			catch (ParserException e)
			{
				println("FAILED: " + assertion);
				println("Syntax: " + e);
				assertErrors++;
				break;
			}
			catch (ContextException e)
			{
				println("FAILED: " + assertion);
				println("Runtime: " + e.getMessage());
				e.ctxt.printStackTrace(Console.out, true);
				assertErrors++;
				break;
			}
			catch (RuntimeException e)
			{
				println("FAILED: " + assertion);
				println("Runtime: " + e.getMessage());
				e.printStackTrace();
				assertErrors++;
				break;
			}
			catch (Exception e)
			{
				println("FAILED: " + assertion);
				println("Exception: " + e);
				e.printStackTrace();
				assertErrors++;
				break;
			}
		}

		if (assertErrors == 0)
		{
			println("PASSED all " + assertPasses +
				" assertions from " + filename);
		}
		else
		{
			println("FAILED " + assertErrors +
				" and passed " + assertPasses + " assertions from " + filename);
		}

		try { input.close(); } catch (Exception e) {/* */}
		return assertErrors == 0;
	}

	protected void doHelp(@SuppressWarnings("unused") String line)
	{
		println("print <expression> - evaluate expression");
		println("runtrace <name> [start test [end test]] - run CT trace");
		println("debugtrace <name> [start test [end test]] - debug CT trace");
		println("savetrace [<file> | off] - save CT trace output");
		println("seedtrace <number> - seed CT trace random generator");
		println("runalltraces [<name>] - run all CT traces in class/module name");
		println("filter %age | <reduction type> - reduce CT trace(s)");
		println("assert <file> - run assertions from a file");
		println("script <file> - run commands from a file");
		println("init - re-initialize the global environment");
		println("env - list the global symbols in the default environment");
		println("pog [<function/operation>] - generate proof obligations");
		println("break [<file>:]<line#> [<condition>] - create a breakpoint");
		println("break <function/operation> [<condition>] - create a breakpoint");
		println("trace [<file>:]<line#> [<exp>] - create a tracepoint");
		println("trace <function/operation> [<exp>] - create a tracepoint");
		println("remove <breakpoint#> - remove a trace/breakpoint");
		println("list - list breakpoints");
		println("coverage clear|write <dir>|merge <dir>|<filenames> - handle line coverage");
		println("latex|latexdoc [<files>] - generate LaTeX line coverage files");
		println("word [<files>] - generate Word HTML line coverage files");
		println("files - list files in the current specification");
		println("set [<pre|post|inv|dtc|measures|annotations> <on|off>] - set runtime checks");
		println("reload - reload the current specification files");
		println("load <files or dirs> - replace current loaded specification files");
		println("save [<files>] - generate Word/ODF source extract files");
		println("quit - leave the interpreter");
	}

	/**
	 * Callable by "do" methods which want to make the command unavailable.
	 * The method just prints "Command not available in this context" and
	 * returns true.
	 */
	protected boolean notAvailable(@SuppressWarnings("unused") String line)
	{
		println("Command not available in this context");
		return true;
	}

	/**
	 * Set a breakpoint at the given file and line with a condition.
	 *
	 * @param file The file name
	 * @param line The line number
	 * @param condition Any condition for the breakpoint, or null
	 * @throws Exception Problems parsing condition.
	 */
	private void setBreakpoint(File file, int line, String condition)
		throws Exception
	{
		if (file == null)
		{
			file = interpreter.getDefaultFile();
		}
		
		if (file == null || file.getPath().equals("?"))
		{
			Set<File> files = interpreter.getSourceFiles();
			
			if (files.size() > 1)
			{
				println("Assuming file " + file.getPath());
			}
			else if (files.isEmpty())
			{
				println("No files defined");
				return;
			}

			file = files.iterator().next();
		}

		INStatement stmt = interpreter.findStatement(file, line);

		if (stmt == null)
		{
			INExpression exp = interpreter.findExpression(file, line);

			if (exp == null)
			{
				println("No breakable expressions or statements at " + file + ":" + line);
			}
			else
			{
				Breakpoint old = interpreter.clearBreakpoint(exp.breakpoint.number);
				
				if (old != null)
				{
					println("Overwriting [" + old.number + "] " + old.location);
				}
				
				Breakpoint bp = interpreter.setBreakpoint(exp, condition);
				println("Created " + bp);
				println(interpreter.getSourceLine(bp.location));
			}
		}
		else
		{
			Breakpoint old = interpreter.clearBreakpoint(stmt.breakpoint.number);
			
			if (old != null)
			{
				println("Overwriting [" + old.number + "] " + old.location);
			}
			
			Breakpoint bp = interpreter.setBreakpoint(stmt, condition);
			println("Created " + bp);
			println(interpreter.getSourceLine(bp.location));
		}
	}

	/**
	 * Set a breakpoint at the given function or operation name with
	 * a condition.
	 *
	 * @param name The function or operation name.
	 * @param condition Any condition for the breakpoint, or null.
	 * @throws Exception Problems parsing condition.
	 */
	private void setBreakpoint(String name, String condition)
		throws Exception
	{
		LexTokenReader ltr = new LexTokenReader(name, Dialect.VDM_SL);
		LexToken token = ltr.nextToken();
		ltr.close();

		Value v = null;

		if (token.is(Token.IDENTIFIER))
		{
			LexIdentifierToken id = (LexIdentifierToken)token;
			TCNameToken lnt = new TCNameToken(id.location, interpreter.getDefaultName(), id.name);
			v = interpreter.findGlobal(lnt);
		}
		else if (token.is(Token.NAME))
		{
			v = interpreter.findGlobal(new TCNameToken((LexNameToken)token));
		}

		if (v instanceof FunctionValue)
		{
			FunctionValue fv = (FunctionValue)v;
			INExpression exp = fv.body;
			
			while (exp instanceof INBinaryExpression)
			{
				// None of the binary expressions check their BP, to avoid excessive stepping
				// when going through (say) a chain of "and" clauses. So if we've picked a
				// binary expression here, we move the BP to the left hand.
				INBinaryExpression bexp = (INBinaryExpression)exp;
				exp = bexp.left;
			}
			
			Breakpoint old = interpreter.clearBreakpoint(exp.breakpoint.number);
			
			if (old != null)
			{
				println("Overwriting [" + old.number + "] " + old.location);
			}
			
			Breakpoint bp = interpreter.setBreakpoint(exp, condition);
			println("Created " + bp);
			println(interpreter.getSourceLine(bp.location));
		}
		else if (v instanceof OperationValue)
		{
			OperationValue ov = (OperationValue)v;
			INStatement stmt = ov.body;
			Breakpoint old = interpreter.clearBreakpoint(stmt.breakpoint.number);
			
			if (old != null)
			{
				println("Overwriting [" + old.number + "] " + old.location);
			}
			
			Breakpoint bp = interpreter.setBreakpoint(stmt, condition);
			println("Created " + bp);
			println(interpreter.getSourceLine(bp.location));
		}
		else if (v == null)
		{
			println(name + " is not visible or not found");
		}
		else
		{
			println(name + " is not a function or operation");
		}
	}

	/**
	 * Set a tracepoint at the given file and line. Tracepoints without
	 * a condition just print "Reached [n]", where [n] is the breakpoint
	 * number.
	 *
	 * @param file The file name
	 * @param line The line number
	 * @param trace Any expression to evaluate at the tracepoint, or null
	 * @throws Exception Problems parsing condition.
	 */
	private void setTracepoint(File file, int line, String trace)
		throws Exception
	{
		if (file == null)
		{
			file = interpreter.getDefaultFile();
		}

		if (file == null || file.getPath().equals("?"))
		{
			Set<File> files = interpreter.getSourceFiles();
			
			if (files.size() > 1)
			{
				println("Assuming file " + file.getPath());
			}
			else if (files.isEmpty())
			{
				println("No files defined");
				return;
			}

			file = files.iterator().next();
		}

		INStatement stmt = interpreter.findStatement(file, line);

		if (stmt == null)
		{
			INExpression exp = interpreter.findExpression(file, line);

			if (exp == null)
			{
				println("No breakable expressions or statements at " + file + ":" + line);
			}
			else
			{
				Breakpoint old = interpreter.clearBreakpoint(exp.breakpoint.number);
				
				if (old != null)
				{
					println("Overwriting [" + old.number + "] " + old.location);
				}
				
				Breakpoint bp = interpreter.setTracepoint(exp, trace);
				println("Created " + bp);
				println(interpreter.getSourceLine(bp.location));
			}
		}
		else
		{
			Breakpoint old = interpreter.clearBreakpoint(stmt.breakpoint.number);
			
			if (old != null)
			{
				println("Overwriting [" + old.number + "] " + old.location);
			}

			Breakpoint bp = interpreter.setTracepoint(stmt, trace);
			println("Created " + bp);
			println(interpreter.getSourceLine(bp.location));
		}
	}

	/**
	 * Set a tracepoint at the given function or operation name. Tracepoints
	 * without a condition just print "Reached [n]", where [n] is the
	 * breakpoint number.
	 *
	 * @param name The function or operation name.
	 * @param trace Any trace for the tracepoint
	 * @throws Exception Problems parsing condition.
	 */
	private void setTracepoint(String name, String trace) throws Exception
	{
		LexTokenReader ltr = new LexTokenReader(name, Dialect.VDM_SL);
		LexToken token = ltr.nextToken();
		ltr.close();

		Value v = null;

		if (token.is(Token.IDENTIFIER))
		{
			LexIdentifierToken id = (LexIdentifierToken)token;
			TCNameToken lnt = new TCNameToken(id.location, interpreter.getDefaultName(), id.name);
			v = interpreter.findGlobal(lnt);
		}
		else if (token.is(Token.NAME))
		{
			v = interpreter.findGlobal(new TCNameToken((LexNameToken)token));
		}

		if (v instanceof FunctionValue)
		{
			FunctionValue fv = (FunctionValue)v;
			INExpression exp = fv.body;
			interpreter.clearBreakpoint(exp.breakpoint.number);
			Breakpoint bp = interpreter.setTracepoint(exp, trace);
			println("Created " + bp);
			println(interpreter.getSourceLine(bp.location));
		}
		else if (v instanceof OperationValue)
		{
			OperationValue ov = (OperationValue)v;
			INStatement stmt = ov.body;
			interpreter.clearBreakpoint(stmt.breakpoint.number);
			Breakpoint bp = interpreter.setTracepoint(stmt, trace);
			println("Created " + bp);
			println(interpreter.getSourceLine(bp.location));
		}
		else
		{
			println(name + " is not a function or operation");
		}
	}

	protected void print(String m)
	{
		Console.out.print(m);
	}

	protected void println(String m)
	{
		Console.out.println(m);
	}

	protected String plural(int n, String s, String pl)
	{
		return n + " " + (n != 1 ? s + pl : s);
	}

	private void writeCoverage(File dir)
		throws IOException
    {
    	for (File f: interpreter.getSourceFiles())
    	{
    		SourceFile source = interpreter.getSourceFile(f);

    		File cov = new File(dir.getPath() + File.separator + f.getName() + ".cov");
    		PrintWriter pw = new PrintWriter(cov);
    		source.writeCoverage(pw);
    		pw.close();
    		println("Written coverage for " + f);
    	}
    }

	private void mergeCoverage(File dir)
		throws IOException
    {
    	for (File f: interpreter.getSourceFiles())
    	{
    		File cov = new File(dir.getPath() + File.separator + f.getName() + ".cov");
    		LexLocation.mergeHits(f, cov);
    		println("Merged coverage for " + f);
    	}
    }
}
