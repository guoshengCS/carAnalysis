package util;
import java.util.Date;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


/**
 * �����ʼ���δʹ�ã�
 *
 */
public class MailSend {

    private  String personalName = "micheal";
    private  Session session;
    private Transport transport;
    public void init(String host,String username,String password) throws Exception
    {
    	Properties props = new Properties(); // ��ȡϵͳ����
        Authenticator auth =  new SimpleAuthenticator(username, password); // �����ʼ��������û���֤
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.auth", "true");
         
        session = Session.getDefaultInstance(props,auth);
        // ����session,���ʼ�����������ͨѶ��
 
        transport = session.getTransport("smtp");   
        System.out.println(host+" "+username+" "+password);
        transport.connect(host, username, password);
        System.out.println("mail init success");
    }
    /**
     * �������з��������û�����֤
     */
     public class SimpleAuthenticator extends Authenticator {
        
        private String username;        
        private String password;        
        public SimpleAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }
    
        public PasswordAuthentication getPasswordAuthentication() {    
            return new PasswordAuthentication(this.username, this.password);   
        }
     }
    public void sendMail(String mail_subject,String mail_body) throws Exception
    {
    	CommonParam common=new CommonParam();
		common.init("mail.properties");
		init(common.getHost(),common.getUsername(),common.getPassword());
    	 MimeMessage message = new MimeMessage(session);
    	 try {
			message.setSubject(mail_subject);  // �����ʼ�����
         message.setText(mail_body); // �����ʼ�����
         message.setSentDate(new Date()); // �����ʼ���������
         Address address;
         String mailto[]=common.getMail_to();
         InternetAddress[] toAddress = new InternetAddress[common.getMail_to().length];
		 address = new InternetAddress(common.getMail_from(), personalName);
         message.setFrom(address); // �����ʼ������ߵĵ�ַ
         for(int i=0;i<toAddress.length;i++)               //���ý��շ���ַ
         {
           toAddress[i] = new InternetAddress(mailto[i]);
//           System.out.println(mail_to[i]);

         }       
         message.addRecipients(Message.RecipientType.TO, toAddress);
         Transport.send(message); // �����ʼ�
         System.out.println("send ok!");
    	 } catch (Exception ex)
         {
             ex.printStackTrace();
             throw new Exception(ex.getMessage());
         }
    }   
        
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		MailSend sendmail = new MailSend();
	        try
	        {
	        	sendmail.sendMail("�����ļ�","���");
	        } catch (Exception ex)
	        {
	        }
	}

}

