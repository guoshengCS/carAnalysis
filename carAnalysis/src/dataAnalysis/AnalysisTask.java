package dataAnalysis;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import javabean.AnalysisUnits;
import javabean.EventList;
import javabean.db.Event;

public class AnalysisTask {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		Socket taskSocket=new Socket("localhost", 8899);
//		ObjectInputStream in=new ObjectInputStream(taskSocket.getInputStream());
//		Event conceptPublic=(Event)in.readObject();
//		System.out.println(conceptPublic.getConceptByName("Î¥·¨Î¥¼Í").generateExp());
		PrintWriter writer=new PrintWriter(new OutputStreamWriter(taskSocket.getOutputStream(), "utf8"));
		Scanner scanner=new Scanner(System.in);
		String str=null;
		while(!(str=scanner.next()).equals("bye")){
			System.out.println(str);
			writer.println(str);
			writer.flush();
		}
		writer.println(str);
		writer.flush();
		taskSocket.close();
//		ObjectInputStream ois=new ObjectInputStream(taskSocket.getInputStream());
//		AnalysisUnits analysisUnits=(AnalysisUnits)ois.readObject();
//		System.out.println("haha"+analysisUnits);
	}

}
