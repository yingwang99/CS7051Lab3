import java.awt.Choice;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import javax.rmi.CORBA.Util;

import DTO.Utility;

public class ChatClient {
	private Scanner scanner = new Scanner(System.in);
	private String nickname = "";
	private BufferedReader reader = null;
	private PrintWriter writer = null;
	
	public ChatClient(){
		Socket s = null;
		String join_id = "";
		try {
			join_id = InetAddress.getLocalHost().getHostAddress().replace(".", "");
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int choice = 0;
		try {
			//s = new Socket("10.62.0.8", 54321); 
			s = new Socket("localhost", 54321);
			reader = getReader(s);
	    	writer = getWriter(s);	
			
	    	MessageThread thread = new MessageThread(reader);
			thread.start();
			
			while(choice != 4){
				System.out.println("Please choose...\n" + menu());
				String input = scanner.nextLine();
				
				
				if(input.equals("1")){
					System.out.println("Please input a chatroom you want to join!");
					String chatroom = scanner.nextLine();
					System.out.println("Input a nickname");
					nickname = scanner.nextLine();
					
					writer.println(Utility.JOIN_CHATROOM + ":" + chatroom + Utility.SEGEMENT + Utility.CLIENT_IP + ":" + 0 + Utility.SEGEMENT + Utility.PORT + ":" + 0 + Utility.SEGEMENT + Utility.CLIENT_NAME + ":" + nickname );
					//writer.flush();
					
				}else if(input.equals("3")){
					System.out.println("Please input a chatroom ref you want to exit");
					String chatRmRef = scanner.nextLine();
					
					writer.println(Utility.LEAVE_CHATROOM + ":" + chatRmRef + Utility.SEGEMENT + Utility.JOIN_ID + ":" + join_id + Utility.SEGEMENT + Utility.CLIENT_NAME + ":" + nickname);
				 
				
				}else if(input.equals("2")){
					System.out.println("Please input the chatroom ref");
					String roomRef = scanner.nextLine();
					System.out.println("Please input the message you are going to send");
					String message = scanner.nextLine();
					message = message.trim();
					
					writer.println(Utility.CHAT + ":" + roomRef + Utility.SEGEMENT + Utility.JOIN_ID + ":" + join_id + Utility.SEGEMENT + Utility.CLIENT_NAME + ":" + nickname + Utility.SEGEMENT + Utility.MESSAGE + ":" + message + "\n\n");
				}else if(input.equals("4")){
					writer.println(Utility.DISCONNECT + ":0" + Utility.SEGEMENT + Utility.PORT + ":0" + Utility.SEGEMENT + Utility.CLIENT_NAME + ":" + nickname);
					writer.flush();
					thread.stop();
					
					break;
				}else{
					writer.println(input + "\n");
				}
				writer.flush();
				
			}
			
			reader.close();
			writer.close();
			s.close();
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
            try {
               
            	if(s != null){
            		s.close();
            	}
            	
                if(reader != null){
                	reader.close();
                }
                
                if(writer != null){
                	writer.close();
                }
               
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	}
	
	public String menu(){
		String menu = "1. Join a chatroom!\n" +
					"2. Send a message!\n" +
					"3. Exit a chatroom!\n" +
					"4. Exit!";
		return menu;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new ChatClient();
	}
	
	private PrintWriter getWriter(Socket socket) throws IOException{
        OutputStream socketOut=socket.getOutputStream();
        return new PrintWriter(socketOut,true);
    }
    private BufferedReader getReader(Socket socket) throws IOException{
        InputStream socketIn=socket.getInputStream();
        return new BufferedReader(new InputStreamReader(socketIn));
    }
    
   
    class MessageThread extends Thread {  
        private BufferedReader reader;  
       
        public MessageThread(BufferedReader reader) {  
            this.reader = reader;  
        }  
  
      
  
        public void run() {  
            String message = "";  
            while (true) {  
                try {  
                    while((message = reader.readLine()) != null){
                    	System.out.println(message);
                    }
                } catch (IOException e) {  
                    e.printStackTrace();  
                } catch (Exception e) {  
                    e.printStackTrace();  
                }  
            }  
        }
        
        
        
    }  


}
