package snooway.dao;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * ��Ӧ�����ݿ�����ӳ�
 * @author Bys
 *
 */
public class DBConnectionPool_app2 {
	 private static   DBConnectionPool_app2 instance;  
	    private  ComboPooledDataSource dataSource;  
	  
	    private DBConnectionPool_app2(String drv, String url, String user, String pass) throws SQLException, PropertyVetoException {  
	        dataSource = new ComboPooledDataSource();  
	  
	        dataSource.setUser(user);  
	        dataSource.setPassword(pass);  
	        dataSource.setJdbcUrl(url);  
	        dataSource.setDriverClass(drv);  
	        //�Զ��������  ��С���� 1 ����������� 10 ��
	        dataSource.setInitialPoolSize(1); 
	        dataSource.setMinPoolSize(1);  
	        dataSource.setMaxPoolSize(50);  
	        dataSource.setMaxStatements(0);  
	        dataSource.setMaxIdleTime(60);
	        dataSource.setIdleConnectionTestPeriod(60);
//	        dataSource.setDebugUnreturnedConnectionStackTraces(true);
	    } 
	  
	    public static final DBConnectionPool_app2 getInstance(String drv, String url, String user, String pass) {  
	        if (instance == null) {  
	            try {  
	                instance = new DBConnectionPool_app2(drv, url, user, pass);  
	            } catch (Exception e) {  
	                e.printStackTrace();
	            }  
	        }
	        return instance;  
	    }  

	    public synchronized final Connection getConnection() {  
	        Connection conn = null;
	        try {
	            conn = dataSource.getConnection();  
	        } catch (SQLException e) {  
	            e.printStackTrace();  
	        }
	        return conn;
	    }
}
