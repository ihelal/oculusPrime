package oculusPrime;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import oculusPrime.commport.PowerLogger;

public class DashboardServlet extends HttpServlet {
	
	static final long serialVersionUID = 1L;	
	static final String HTTP_REFRESH_DELAY_SECONDS = "2";
	
	NetworkMonitor monitor = NetworkMonitor.getReference();
	Settings settings = Settings.getReference();
	BanList ban = BanList.getRefrence();
	State state = State.getReference();
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
	
		if ( ! settings.getBoolean(ManualSettings.developer.name())){
			out.println("this service is for developers only, check settings..");
			out.close();	
			return;
		}
		
		if(ban.isBanned(request.getRemoteAddr())){
			out.println("this is a banned address: " + ban);
			out.close();	
			return;
		}
	
		String view = null;	
		String delay = null;	
		String member = null;	
		try {
			view = request.getParameter("view");
			delay = request.getParameter("delay");
			member = request.getParameter("member");
		} catch (Exception e) {
			Util.debug("doGet(): " + e.getLocalizedMessage(), this);
		}
			
		if(delay == null) delay =  HTTP_REFRESH_DELAY_SECONDS;
		
		out.println("<html><head><meta http-equiv=\"refresh\" content=\""+ delay + "\"></head><body> \n");

		if(view != null){
			if(view.equalsIgnoreCase("ban")){
				out.println(ban + "<br />\n");
				out.println(ban.tail(30) + "\n");
				out.println("\n</body></html> \n");
				out.close();
			}
			
			if(view.equalsIgnoreCase("state")){
				
				if(member != null) {
					Util.debug("member = " + member);
					out.println(state.get(member.trim()));
				}
				else out.println(state.toHTML() + "\n");
				
				out.println("\n</body></html> \n");
				out.close();
			}
			
			if(view.equalsIgnoreCase("sysout")){
				out.println(new File(Settings.stdout).getAbsolutePath() + "<br />\n");
				out.println(Util.tail(30) + "\n");
				out.println("\n</body></html> \n");
				out.close();
			}
			
			if(view.equalsIgnoreCase("power")){	
				out.println(new File(PowerLogger.powerlog).getAbsolutePath() + "<br />\n");
				out.println(PowerLogger.tail(30) + "\n");
				out.println("\n</body></html> \n");
				out.close();
			}
			
			if(view.equalsIgnoreCase("ros")){
				out.println(state.rosDashboard() + "\n");
				out.println("\n</body></html> \n");
				out.close();
			}
			
			if(view.equalsIgnoreCase("ap")){
				String[] ap = monitor.getAccessPoints(); 
				for(int i = 0 ; i < ap.length ; i++)
					out.println(ap[i] + "<br />\n");
				out.println("\n</body></html> \n");
				out.close();
			}
			
			if(view.equalsIgnoreCase("log")){
				out.println("\nsystem output: <hr>\n");
				out.println(Util.tail(25) + "\n");
				out.println("\n<br />power log: <hr>\n");
				out.println("\n" + PowerLogger.tail(5) + "\n");
				out.println("\n<br />banned addresses: " +  ban + " telnet users [" 
						+ state.get(State.values.telnetusers)+ "] " + "<hr>\n");
				out.println("\n" + ban.tail(10) + "\n");
				out.println("\n</body></html> \n");
				out.close();
			}
		}
		
		// default
		out.println(state.toDashboard() + "\n");
		out.println("\n</body></html> \n");
		out.close();	
	}
	
	/*System.out.println(System.getProperty("sun.arch.data.model") );
	public String toHTML(){	
		Properties props = state.getProperties();
		StringBuffer str = new StringBuffer("\n<table>");
		Set<String> keys = props.keySet();
		for(Iterator<String> i = keys.iterator(); i.hasNext(); ){
			String key = "\n<tr><td>" + i.next() + "<td>";
			str.append(key + props.get(key) + "</tr>");
		}
		str.append("</table>\n");
		return str.toString();
	}
	*/


}
