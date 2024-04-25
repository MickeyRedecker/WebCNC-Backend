package redecker.mickey.webcnc.setup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import redecker.mickey.webcnc.netstate.INetworkStateCache;
import redecker.mickey.webcnc.switchcomms.manager.ISwitchCommsManager;
import redecker.mickey.webcnc.switchcredstore.ISwitchCredStore;

/**
 * The main class of the WebCNC
 * 
 * It stores application-wide parameters and objects
 * 
 * @author Mickey Redecker
 *
 */
@SpringBootApplication
@ComponentScan(basePackages = { "redecker.mickey.webcnc.setup", "redecker.mickey.webcnc.restcontroller" })
public class WebcncApplication {

	public static INetworkStateCache netstatecache;
	public static ISwitchCredStore switchcredstore;
	public static ISwitchCommsManager switchComms;
	public static String password;
	public static Integer switchConnectionRetries;
	public static Integer switchConnectionTimeout;

	public static void main(String[] args) {
		Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
		SpringApplication.run(WebcncApplication.class, args);
	}

}
