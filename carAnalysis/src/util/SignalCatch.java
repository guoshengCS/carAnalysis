package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import dataAnalysis.WebPageAnalyzer;

import javabean.AnalysisUnits;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * ��׽�˳��ź��������˳�ǰ���ڴ��з�����¼���л���洢��Ӳ��
 * @author Bys
 *
 */
public class SignalCatch implements SignalHandler {
	
	public AnalysisUnits analysisUnits;

	public SignalCatch(){
		
			
		File save = new File("analysis.save");
		if(save.exists()){
			FileInputStream fis;
			try {
				fis = new FileInputStream(save);
				ObjectInputStream ois = new ObjectInputStream(fis);
				analysisUnits = (AnalysisUnits)ois.readObject();
				ois.close();
				fis.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			analysisUnits = new AnalysisUnits();
		}
	}
	
	
	
	private void signalCallback(Signal sn) {  
		       System.err.println((new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date())+":"+sn.getName() + " is recevied!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		       AnalysisProperties.storeRecord(analysisUnits);
		       
		       if (AnalysisProperties.getInstance().isOld()) {
					WebPageAnalyzer.da.updateSubjectCount();
				}
		       
		       System.exit(-1);
		    }  

	@Override
	public void handle(Signal signalName) {
		// TODO Auto-generated method stub
		signalCallback(signalName);  
	}

}
