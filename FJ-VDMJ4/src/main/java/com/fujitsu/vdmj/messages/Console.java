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

package com.fujitsu.vdmj.messages;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class Console
{
	/** The charset for the console. */
	public static String charset;

	/** A print writer for stdout that uses a given encoding. */
	public static ConsoleWriter out;

	/** A print writer for stderr that uses a given encoding. */
	public static ConsoleWriter err;

	/** A buffered reader for stdin that uses a given encoding. */
	public static BufferedReader in;

	static
	{
		init(Charset.defaultCharset().name());
	}

	public static void init(String cs)
	{
		try
		{
			init(cs,
				new ConsolePrintWriter(new PrintWriter(new OutputStreamWriter(System.out, cs))),
				new ConsolePrintWriter(new PrintWriter(new OutputStreamWriter(System.err, cs))));
		}
		catch (UnsupportedEncodingException e)
		{
			System.err.println("Console encoding exception: " + e);
		}
	}
	
	public static void init(String cs, ConsoleWriter stdout, ConsoleWriter stderr)
	{
		try
		{
			charset = cs;
			out = stdout;
			err = stderr;
			in = new BufferedReader(new InputStreamReader(System.in, charset));
		}
		catch (UnsupportedEncodingException e)
		{
			System.err.println("Console encoding exception: " + e);
		}
	}
}
