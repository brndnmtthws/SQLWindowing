package com.sap.hadoop.windowing.query2.specification;

import java.util.ArrayList;

import org.antlr.runtime.tree.CommonTree;
import org.apache.hadoop.hive.ql.parse.ASTNode;

public class WindowFunctionSpec
{
	String name;
	boolean isStar;
	boolean isDistinct;
	ArrayList<ASTNode> args;
	WindowSpec windowSpec;
	String alias;
	
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public boolean isStar()
	{
		return isStar;
	}

	public void setStar(boolean isStar)
	{
		this.isStar = isStar;
	}

	public boolean isDistinct()
	{
		return isDistinct;
	}

	public void setDistinct(boolean isDistinct)
	{
		this.isDistinct = isDistinct;
	}

	public ArrayList<ASTNode> getArgs()
	{
		args = args == null ? new ArrayList<ASTNode>() : args;
		return args;
	}

	public void setArgs(ArrayList<ASTNode> args)
	{
		this.args = args;
	}
	
	public void addArg(CommonTree arg)
	{
		args = args == null ? new ArrayList<ASTNode>() : args;
		args.add((ASTNode)arg);
	}

	public WindowSpec getWindowSpec()
	{
		return windowSpec;
	}

	public void setWindowSpec(WindowSpec windowSpec)
	{
		this.windowSpec = windowSpec;
	}
	
	public String getAlias()
	{
		return alias;
	}

	public void setAlias(String alias)
	{
		this.alias = alias;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((args == null) ? 0 : args.hashCode());
		result = prime * result + (isDistinct ? 1231 : 1237);
		result = prime * result + (isStar ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((windowSpec == null) ? 0 : windowSpec.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WindowFunctionSpec other = (WindowFunctionSpec) obj;
		if (args == null)
		{
			if (other.args != null)
				return false;
		}
		else if (!args.equals(other.args))
			return false;
		if (isDistinct != other.isDistinct)
			return false;
		if (isStar != other.isStar)
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		if (windowSpec == null)
		{
			if (other.windowSpec != null)
				return false;
		}
		else if (!windowSpec.equals(other.windowSpec))
			return false;
		return true;
	}

	public String toString()
	{
		StringBuilder buf = new StringBuilder();
		buf.append(name).append("(");
		if (isStar )
		{
			buf.append("*");
		}
		else
		{
			if ( isDistinct )
			{
				buf.append("distinct ");
			}
			if ( args != null )
			{
				boolean first = true;
				for(CommonTree arg : args)
				{
					if ( first) first = false; else buf.append(", ");
					buf.append(arg.toStringTree());
				}
			}
		}
		
		buf.append(")");
		
		if ( windowSpec != null )
		{
			buf.append(" ").append(windowSpec.toString());
		}
		
		if ( alias != null )
		{
			buf.append(" as ").append(alias);
		}

		return buf.toString();
	}
	
}
