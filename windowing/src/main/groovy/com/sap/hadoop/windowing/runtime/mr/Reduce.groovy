package com.sap.hadoop.windowing.runtime.mr

import java.util.ArrayList;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorUtils;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorUtils.ObjectInspectorCopyOption;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;

import com.sap.hadoop.windowing.WindowingShell;
import com.sap.hadoop.windowing.query.Column;
import com.sap.hadoop.windowing.query.OutputColumn;
import com.sap.hadoop.windowing.query.Query;
import com.sap.hadoop.windowing.query.QueryInput;
import com.sap.hadoop.windowing.query.QueryOutput;
import com.sap.hadoop.windowing.runtime.OutputObj;

public class Reduce extends MapReduceBase implements Reducer<Writable, Writable, Writable, Writable>
{
	WindowingShell wshell
	Query qry
	ArrayList<StructField> partitionColumnFields = []
	
	public void configure(JobConf job) 
	{
		String qryStr = job.get(Job.WINDOWING_QUERY_STRING);
		wshell = new WindowingShell(job, new MRTranslator(), new MRExecutor())
		qry = wshell.translate(qryStr)
		
		ArrayList<StructField> partitionColumnFields = []
		for(Column c in qry.input.partitionColumns)
		{
			partitionColumnFields << c.field
		}

		println qry.qSpec.toString()
	}

	@Override
	public void reduce(Writable key, Iterator<Writable> values,
			OutputCollector<Writable, Writable> output, Reporter reporter)
			throws IOException
	{
		/*while(values.hasNext())
		{
			output.collect(NullWritable.get(), values.next());
		}*/
		
		QueryInput qryIn = qry.input
		def windowFns = qry.wnFns
		def windowFnAliases = qry.wnAliases
			
		OutputObj orow = new OutputObj();
		orow.resultMap = [:]
		com.sap.hadoop.windowing.runtime.Partition p = new com.sap.hadoop.windowing.runtime.Partition(
			qryIn.inputOI, qryIn.processingOI, partitionColumnFields)
		while(values.hasNext())
		{
			p << ObjectInspectorUtils.copyToStandardObject(qryIn.deserializer.deserialize(values.next()), 
				qryIn.inputOI, ObjectInspectorCopyOption.JAVA)
		}
		orow.resultMap.clear()
		for (i in 0..<windowFns.size())
		{
			orow.resultMap[windowFnAliases[i]] = windowFns[i].processPartition(p)
		}
		
		for(row in p)
		{
			orow.iObj = row
			writeOutputRow(orow, qry, output)
		}
	}
	
	void writeOutputRow(OutputObj orow, Query qry, OutputCollector<Writable, Writable> output)
	{
		QueryOutput qryOut = qry.output
		ArrayList o = []
		for(OutputColumn oc in qry.output.columns)
		{
			oc.groovyExpr.binding = orow
			o << oc.groovyExpr.run()
		}
		Writable outWritable = qryOut.serDe.serialize(o, qryOut.processingOI)
		output.collect(NullWritable.get(), outWritable);
	}
	
}

