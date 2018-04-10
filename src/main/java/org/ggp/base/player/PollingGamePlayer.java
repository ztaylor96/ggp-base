package org.ggp.base.player;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.ggp.base.player.event.PlayerDroppedPacketEvent;
import org.ggp.base.player.event.PlayerReceivedMessageEvent;
import org.ggp.base.player.event.PlayerSentMessageEvent;
import org.ggp.base.player.gamer.Gamer;
import org.ggp.base.player.gamer.statemachine.random.RandomGamer;
import org.ggp.base.player.request.factory.RequestFactory;
import org.ggp.base.player.request.grammar.Request;
import org.ggp.base.util.gdl.grammar.DataFormat;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.observer.Event;
import org.ggp.base.util.observer.Observer;
import org.ggp.base.util.symbol.factory.HRFSymbolFactory;
import org.ggp.base.util.symbol.grammar.SymbolList;

import com.google.common.collect.Lists;

/**
 *
 * A parallel class to GamePlayer which uses a reversed http protocol to communicate
 * with the game manager.
 *
 * Players poll for messages instead of having to establish a inward connection
 * with the game manager.
 *
 * The protocol is beneficial when fire walls, or lack of public IP, inhibit establishing a connection
 * between manager and player.
 * Players can also bounce back into games when connection hangs for a brief period of time.
 *
 * Assumes HRF message format, but can be configured otherwise.
 *
 * @author robertchuchro
 *
 */
public final class PollingGamePlayer extends AbstractGamePlayer
{
    private final String getUrl;
    private final String putUrl;
    private final Gamer gamer;
    private final List<Observer> observers;

    public PollingGamePlayer(String getUrl, String putUrl, Gamer gamer, DataFormat dataFormat) throws IOException
    {
		GdlPool.format = dataFormat;

        observers = new ArrayList<Observer>();

        this.getUrl = getUrl;
        this.putUrl = putUrl;
        this.gamer = gamer;
    }

    public PollingGamePlayer(String getUrl, String putUrl, Gamer gamer) throws IOException
    {
    	//Configures code base to operate in HRF mode by default
		this(getUrl, putUrl, gamer, DataFormat.HRF);
    }

	@Override
	public void addObserver(Observer observer)
	{
		observers.add(observer);
	}

	@Override
	public void notifyObservers(Event event)
	{
		for (Observer observer : observers)
		{
			observer.observe(event);
		}
	}

	public final Gamer getGamer() {
	    return gamer;
	}

	public void shutdown() {
		//meow
	}

	@Override
	public void run()
	{
		while (true) {
			try {
				String in = sendGet();

				if (in == null || in.length() == 0) {
					sleep(1000L);
					continue;
				}

				notifyObservers(new PlayerReceivedMessageEvent(in));
				GamerLogger.log("GamePlayer", "[Received at " + System.currentTimeMillis() + "] " + in, GamerLogger.LOG_LEVEL_DATA_DUMP);

				Request request = new RequestFactory().create(gamer, in);
				String out = request.process(System.currentTimeMillis());
				//System.out.println(out);

				sendPost(out, parseMessageId(in));
				notifyObservers(new PlayerSentMessageEvent(out));
				GamerLogger.log("GamePlayer", "[Sent at " + System.currentTimeMillis() + "] " + out, GamerLogger.LOG_LEVEL_DATA_DUMP);
			} catch (Exception e) {
				System.out.println("[Dropped data at " + System.currentTimeMillis() + "] Due to " + e);
				GamerLogger.log("GamePlayer", "[Dropped data at " + System.currentTimeMillis() + "] Due to " + e, GamerLogger.LOG_LEVEL_DATA_DUMP);
				notifyObservers(new PlayerDroppedPacketEvent());
			}
			try {
				sleep(1000L);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * extracts the [message id, message sender]
	 * out of the received message
	 */
	private List<String> parseMessageId(String in) {
		SymbolList sym;
		try {
			sym = (SymbolList) HRFSymbolFactory.create(in);
		} catch (Exception e) {
			return Lists.newArrayList("", "");
		}
		return Lists.newArrayList(sym.get(1).toString(), sym.get(2).toString());

	}

	// HTTP GET request
	private String sendGet() throws IOException {

		String params = "?";
		params += "recipient=" + gamer.getName();
		URL url = new URL(this.getUrl + params);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		int responseCode = con.getResponseCode();

		//System.out.println("\nSending 'GET' request to URL : " + url);
		//System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		String msg = response.toString();
		//System.out.println("response: " + msg);

		return response.toString();
	}

	// HTTP POST request
	private void sendPost(String message, List<String> msgId) throws Exception {

		String params = "?";
		params += "sender=" + gamer.getName();
		params += "&recipient=" + msgId.get(1);
		params += "&msgid=" + msgId.get(0);

		URL url = new URL(putUrl + params);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		//add request header
		con.setRequestMethod("POST");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		//message = "ready";
		wr.writeBytes(message);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		//System.out.println("\nSending 'POST' request to URL : " + putUrl);
		//System.out.println("Post parameters : " + params);
		//System.out.println("Message : " + message);
		//System.out.println("Response Code : " + responseCode);
	}


	// It might make sense to factor this out into a separate app sometime,
	// so that the GamePlayer class doesn't have to import RandomGamer.
	public static void main(String[] args)
	{
		if (args.length < 2) {
			System.err.println("Usage: GamePlayer <get url> <put url>");
			System.exit(1);
		}

		try {
			PollingGamePlayer player = new PollingGamePlayer(args[0], args[1], new RandomGamer());
			player.run();
		} catch (IOException e) {
			System.err.println("IO Exception: " + e);
			e.printStackTrace();
			System.exit(3);
		}
	}
}
