package com.sap.hadoop.windowing.query

import java.util.ArrayList;

import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.antlr.runtime.tree.TreeAdaptor;

import com.sap.hadoop.windowing.Constants;
import com.sap.hadoop.windowing.WindowingException;
import com.sap.hadoop.windowing.parser.WindowingParser;

class QuerySpecBuilder
{
	CommonTreeAdaptor adaptor;
	QuerySpec qSpec = new QuerySpec()
	boolean processingInput = true
	
	void visit(CommonTree node) throws WindowingException
	{
		boolean isNil = adaptor.isNil(node);
		boolean visitSubTree = _preVisit(node);
		if ( visitSubTree )
		{
			for(int i = 0; i < adaptor.getChildCount(node); i++)
			{
				CommonTree child = adaptor.getChild(node, i);
				Object visitResult = visit(child);
			}
			_postVisit(node);
		}
	}
	
	boolean _preVisit(CommonTree node)
	{
		switch(node.getType())
		{
			case WindowingParser.OUTPUTSPEC:
				processingInput =false
				break;
			case WindowingParser.TBLFUNCTION:
				qSpec.tblFuncSpec = tablefunction(node);
				return false;
		}
		return true;
	}
	
	void _postVisit(CommonTree node) throws WindowingException
	{
		switch(node.getType())
		{
			case WindowingParser.QUERY:
				break;
			case WindowingParser.PARAM:
				if (processingInput) 
					visitInputParam(node, qSpec.tableIn);
				else
					visitOutputParam(node, qSpec.tableOut);
				break;
			case WindowingParser.TABLEINPUT:
				visitTableInput(node, qSpec.tableIn);
				break;
			case WindowingParser.FUNCTION:
				qSpec.funcSpecs << function(node)
				break;
			case WindowingParser.SELECTCOLUMN:
				qSpec.selectColumns << selectColumn(node)
				break;
			case WindowingParser.WHERE:
				qSpec.whereExpr = node.children[0].text
				break;
			case WindowingParser.PARTITION:
				partition(node, qSpec.tableIn.partitionColumns)
				break;
			case WindowingParser.ORDER:
				order(node, qSpec.tableIn.orderColumns)
				break;
			case WindowingParser.OUTPUTSPEC:
				ouputSpec(node, qSpec.tableOut);
				break;
		}
	}
	
	void partition(CommonTree node, ArrayList<String> partitionCols)
	{
		for(int i = 0; i < adaptor.getChildCount(node); i++)
		{
			CommonTree child = adaptor.getChild(node, i);
			partitionCols << child.text
		}
	}
	
	void order(CommonTree node, ArrayList<OrderColumn> orderColumns)
	{
		for(int i = 0; i < adaptor.getChildCount(node); i++)
		{
			CommonTree child = adaptor.getChild(node, i);
			orderColumns << orderColumn(child)
		}
	}
	
	OrderColumn orderColumn(CommonTree node)
	{
		def name = node.children[0].text
		def ord = Order.ASC
		if (node.childCount > 1)
		{
			def otyp = node.children[1].getType()
			ord = otyp == WindowingParser.ASC ? Order.ASC : Order.DESC
		}
		return new OrderColumn(name : name, order: ord)
	}
	
	void visitTableInput(CommonTree node, TableInput tableIn) throws WindowingException
	{
		CommonTree child = node.children[0]
		if ( child.getType() == WindowingParser.ID )
		{
			tableIn.tableName = child.text
		}
		else if ( child.getType() == WindowingParser.GROOVYEXPRESSION )
		{
			tableIn.hiveQuery = child.text
		}
	}
	
	void visitInputParam(CommonTree node, TableInput tableInput)
	{
		String name = node.children[0].text
		String value = node.children[1].text
		
		name = name.toLowerCase()
		switch(name)
		{
			case Constants.INPUT_PATH:
				tableInput.inputPath = value;
				break;
			case Constants.INPUT_KEY_CLASS:
				tableInput.keyClass = value;
				break;
			case Constants.INPUT_VALUE_CLASS:
				tableInput.valueClass = value;
				break;
			case Constants.INPUT_INPUTFORMAT_CLASS:
				tableInput.inputFormatClass = value;
				break;
			case Constants.INPUT_SERDE_CLASS:
				tableInput.serDeClass = value;
				break;
			case Constants.INPUT_RECORD_READER_CLASS:
				tableInput.windowingInputClass = value;
				break;
			default:
				tableInput.serDeProps.setProperty(name, value)
		}
	}
	
	void visitOutputParam(CommonTree node, TableOutput tableOutput)
	{
		String name = node.children[0].text
		String value = node.children[1].text
		
		tableOutput.serDeProps.setProperty(name, value)
	}
	
	void ouputSpec(CommonTree node, TableOutput tableOut)
	{
		int cCnt = node.children.size()
		tableOut.outputPath = node.children[0].text
		if ( cCnt > 1 )
		{
			CommonTree child1 = node.children[1]
			if ( child1.getType() == WindowingParser.SERDE)
			{
				tableOut.serDeClass = child1.children[0].text
				CommonTree rWrtrOrFmt = child1.children[1]
				if ( rWrtrOrFmt.getType() == WindowingParser.RECORDWRITER)
				{
					tableOut.recordwriterClass = rWrtrOrFmt.children[0].text
				}
				else
				{
					tableOut.outputFormat = rWrtrOrFmt.children[0].text
				}
			}
			
			if ( child1.getType() == WindowingParser.LOADSPEC || cCnt > 2)
			{
				CommonTree loadSpec = child1.getType() == WindowingParser.LOADSPEC ? child1 : node.children[2]
				tableOut.tableName = loadSpec.children[0].text
				if ( loadSpec.children.size() > 1 )
				{
					CommonTree gChild1 = loadSpec.children[1]
					if ( gChild1.getType() == WindowingParser.OVERWRITE)
					{
						tableOut.overwrite = true
					}
					else
						tableOut.partitionClause = gChild1.text
				}
				if ( loadSpec.children.size() > 2 )
				{
					CommonTree gChild2 = loadSpec.children[2]
					if ( gChild2.getType() == WindowingParser.OVERWRITE)
					{
						tableOut.overwrite = true
					}
				}
			}
		}
	}
	
	FuncSpec function(CommonTree node)
	{
		FuncSpec fSpec = new FuncSpec()
		fSpec.name = node.children[0].text.toLowerCase()
		fSpec.alias = node.children[1].text
		int idx = 2
		
		while ( node.childCount > idx && (node.children[idx].getType() in [WindowingParser.GROOVYEXPRESSION,WindowingParser.STRING, WindowingParser.ID, WindowingParser.NUMBER] ) )
		{
			fSpec.params = fSpec.params == null ? [] : fSpec.params
			fSpec.params << functionArg(node.children[idx])
			idx++
		}
		
		if ( node.childCount > idx && node.children[idx].getType() == WindowingParser.TYPENAME)
		{
			fSpec.typeName = node.children[idx].children[0].text
			idx++
		}
		
		if ( node.childCount > idx )
		{
			fSpec.window = createwindow(node.children[idx])
		}
		return fSpec
	}
	
	FuncArg functionArg(CommonTree node)
	{
		switch(node.getType())
		{
			case WindowingParser.STRING:
				return new FuncArg(str : node.text)
			case WindowingParser.GROOVYEXPRESSION:
				return new FuncArg(expr : node.text)
			case WindowingParser.ID:
				return new FuncArg(id : node.text)
			case WindowingParser.NUMBER:
				return new FuncArg(iVal : Integer.parseInt(node.text))
		}
	}
	
	TableFuncSpec tablefunction(CommonTree node) throws WindowingException
	{
		TableFuncSpec fSpec = new TableFuncSpec()
		fSpec.name = node.children[0].text.toLowerCase()
		
		boolean inputIsTable = false
		CommonTree inputSpecNode = node.children[1]
		switch(inputSpecNode.getType())
		{
			case WindowingParser.TABLEINPUT:
				visit(inputSpecNode)
				inputIsTable = true
				break;
			case WindowingParser.TBLFUNCTION:
				fSpec.inputFuncSpec = tablefunction(inputSpecNode)
				break;
		}
		
		int idx = 2
		
		while ( node.childCount > idx && (node.children[idx].getType() in [WindowingParser.GROOVYEXPRESSION,WindowingParser.STRING, WindowingParser.ID, WindowingParser.NUMBER] ) )
		{
			fSpec.params = fSpec.params == null ? [] : fSpec.params
			fSpec.params << functionArg(node.children[idx])
			idx++
		}
		
		CommonTree partitionCols
		CommonTree orderCols
		CommonTree window
		
		for (; idx < node.childCount; idx++)
		{
			CommonTree nextChild = node.children[idx]
			if (nextChild.getType() == WindowingParser.PARTITION) 
				partitionCols = nextChild
			else if (nextChild.getType() == WindowingParser.ORDER) 
				orderCols = nextChild
			else
				window =  nextChild
		}
		
		if ( partitionCols != null && inputIsTable)
		{
			throw new WindowingException(sprintf("Function '%s' cannot have a partition clause, its input is a 'tableinput'", fSpec.name))
		}
		
		if ( partitionCols )
		{
			partition(partitionCols, fSpec.partitionColumns)
		}
		else
		{
			fSpec.partitionColumns = null
		}
		
		if ( orderCols )
		{
			order(orderCols, fSpec.orderColumns)
		}
		else
		{
			fSpec.orderColumns = null
		}
		
		if ( window )
		{
			fSpec.window = createwindow(window)
		}
		return fSpec
	}
	
	Window createwindow(CommonTree node)
	{
		switch(node.getType())
		{
			case WindowingParser.WINDOWRANGE:
				return new Window(start : rowsboundary(node.children[0]), end : rowsboundary(node.children[1]))
			case WindowingParser.WINDOWVALUES:
				return new Window(start : valuesboundary(node.children[0]), end : valuesboundary(node.children[1]))
		}
	}
	
	Boundary rowsboundary(CommonTree node)
	{
		switch(node.getType())
		{
			case WindowingParser.CURRENT:
				return new CurrentRow();
			case WindowingParser.PRECEDING:
			case WindowingParser.FOLLOWING:
				def dir = node.getType() == WindowingParser.PRECEDING ? Direction.PRECEDING : Direction.FOLLOWING
				def c = node.children[0]
				def amt = c.getType() == WindowingParser.UNBOUNDED ? Boundary.UNBOUNDED_AMOUNT : Integer.parseInt(c.getText())
			  return new RangeBoundary(direction: dir, amt: amt)
		}
	}
	
	Boundary valuesboundary(CommonTree node)
	{
		switch(node.getType())
		{
			case WindowingParser.CURRENT:
			case WindowingParser.PRECEDING:
			case WindowingParser.FOLLOWING:
				return rowsboundary(node)
			case WindowingParser.LESS:
			case WindowingParser.MORE:
				def dir =  node.getType() == WindowingParser.LESS ? Direction.PRECEDING : Direction.FOLLOWING
				def expr = node.children[0]
				def amt = Integer.parseInt(node.children[1].getText())
				return new ValueBoundary(direction: dir, exprString: expr, amt: amt)
		}
	}
	
	SelectColumn selectColumn(CommonTree node)
	{
		SelectColumn sc = new SelectColumn()
		sc.alias = node.children[0].text
		if ( node.children.size() > 1)
		{
			sc.expr = node.children[1].text
		}
		if ( node.children.size() > 2)
		{
			sc.typeName = node.children[2].children[0].text
		}
		return sc
	}
}
