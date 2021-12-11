/*******************************************************************************
 *
 *	Copyright (c) 2016 Aarhus University.
 *
 *	Author: Kenneth Lausdahl
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

package util;

import java.util.List;

import com.fujitsu.vdmj.values.Value;

public class CsvResult
{
	private List<Value> values;
	private String errorMsg;
	
	public CsvResult(List<Value> values)
	{
		this(values, null);
	}
	
	public CsvResult(List<Value> values, String errorMsg)
	{
		this.values = values;
		this.errorMsg = errorMsg;
	}
	
	public List<Value> getValues()
	{
		return values;
	}
	public boolean dataOk()
	{
		return errorMsg == null;
	}
	
	public String getErrorMsg()
	{
		return errorMsg;
	}
}
