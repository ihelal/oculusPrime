package developer.swingtool;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import oculusPrime.State;
import oculusPrime.State.values;
 
public class StateMonitor extends JFrame {
	
	private static final long serialVersionUID = 1L;
	BufferedReader reader = null;
	PrintWriter printer = null;
	Socket socket = null;
	int rx = 0;
	String ip;
	int port;
    JTable table;
 
    public StateMonitor(String ip, int port) {

		this.ip = ip;
		this.port = port;
		
        String[] columnNames = {"state", "value", "count"};
        Object[][] data = new Object[State.values.values().length][3];

		values[] cmds = State.values.values();
		for(int i = 0; i < cmds.length; i++){
			data[i][0] = cmds[i].name();
			data[i][2] = 0;
		}
		
        table = new JTable(data, columnNames);
        table.setPreferredScrollableViewportSize(new Dimension(500, 800));
        table.getColumnModel().getColumn(0).setPreferredWidth(150); 
        table.getColumnModel().getColumn(0).setMinWidth(100); 
        table.getColumnModel().getColumn(0).setMaxWidth(250); 
        table.getColumnModel().getColumn(2).setMaxWidth(60); 
        // table.setEnabled(false);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
		setDefaultLookAndFeelDecorated(true);
		setLayout(new GridLayout(1,0));
        JScrollPane scrollPane = new JScrollPane(table);
        getContentPane().add(scrollPane);
        pack();
        setVisible(true);
		new Timer().scheduleAtFixedRate(new Task(), 0, 10000);
    }
 
	private class Task extends TimerTask {
		public void run(){
			if(printer == null || socket.isClosed()){		
				openSocket();
				try { Thread.sleep(5000); } catch (InterruptedException e) {}
				if(socket != null) if(socket.isConnected()) readSocket();					
			} else {
				try {
					printer.checkError();
					printer.flush();
					printer.println("state"); 
				} catch (Exception e) {
					System.out.println("TimerTask(): "+e.getMessage());
					closeSocket();
				}
			}
		}
	}
	
	void openSocket(){	
		try {	
			setTitle("trying to connect");
			socket = new Socket(ip, port);
			printer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			System.out.println("openSocket(): connected to: " + socket.getInetAddress().toString());
			setTitle(socket.getInetAddress().toString());
		} catch (Exception e) {
			setTitle("disconnected");
			System.out.println("openSocket(): " + e.getMessage());
			closeSocket();
		}
	}

	void closeSocket(){
		if(printer != null){
			printer.close();
			printer = null;
		}
		if(reader != null){
			try {
				reader.close();
				reader = null;
			} catch (IOException e) {
				System.out.println("closeSocket(): " + e.getLocalizedMessage());
			}
		}
		try { 
			if(socket != null) socket.close(); 
		} catch (IOException ex) {
			System.out.println("closeSocket(): " + ex.getLocalizedMessage());
		}
	}

	void readSocket(){	
		new Thread(new Runnable() { public void run() {
			String input = null;
			while(printer != null) {
				try {
					input = reader.readLine();
					if(input == null) {
						System.out.println("readSocket(): closing..");
						try { Thread.sleep(5000); } catch (InterruptedException e) {}
						closeSocket();
						break;
					}
					
					// ignore dummy messages 
					input = input.trim();
					if(input.length() > 0) {
						
						setTitle(socket.getInetAddress().toString() + " rx: " + rx++);
			
						input = input.replace("<telnet>", "");
						input = input.replace("=", "");
						input = input.replace("  ", " ");
						input = input.trim();
						
						String[] tokens = input.split(" ");	
						// System.out.println("[" + input + "] tokens:" + tokens.length);
						
						if(input.contains("deleted")){
							for( int i = 0 ; i < table.getRowCount() ; i++ )
								if(table.getValueAt(i, 0).equals(tokens[tokens.length-1]))
									table.setValueAt(table.getValueAt(i, 1) + " (deleted)", i, 1);		
						}
					
						if(input.contains("<state>")){
							for( int i = 0 ; i < table.getRowCount() ; i++ ){
								if(table.getValueAt(i, 0).equals(tokens[1])){
									String value = input.substring(input.indexOf(tokens[2]), input.length());
									table.setValueAt(value, i, 1);
									table.setValueAt((int)table.getValueAt(i, 2)+1, i, 2);
								}
							}
						}
						
						if(tokens.length == 2){
							for( int i = 0 ; i < table.getRowCount() ; i++ ){
								if(table.getValueAt(i, 0).equals(tokens[0])){
									table.setValueAt(tokens[1], i, 1);
									table.setValueAt((int)table.getValueAt(i, 2)+1, i, 2);
								}
							}
						}
					}
				} catch (Exception e) {
					System.out.println("readSocket(): "+e.getMessage());
					closeSocket();
				}
			}
		}}).start();
	}

    public static void main(String[] args) {
    	String ip = args[0];
		int port = Integer.parseInt(args[1]);
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	new StateMonitor(ip, port);
            }
        });
    }
}