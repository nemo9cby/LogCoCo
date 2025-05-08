package ca.yorku.test;

import ca.yorku.nemo.main.MainParser;

public class TestMethodBindingResolving {
	public static void main(String[] args) {
		String filePath = "/Users/nemo/hbase-1.2.6/hbase-client/src/main/java/org/apache/hadoop/hbase/client/AsyncProcess.java";
		MainParser.getResolvedCUFromFilePath(filePath);
		
	}
}
