package com.tol1.irssinotifier.server.datamodels;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.persistence.Id;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindexed;
import com.tol1.irssinotifier.server.IrssiNotifier;
import com.tol1.irssinotifier.server.enums.TileType;
import com.tol1.irssinotifier.server.exceptions.XmlGeneratorException;

public class Message {
	@Id public Long id;
	@Indexed public Long timestamp;
	@Unindexed public String nick;
	@Unindexed public String channel;
	@Unindexed public String message;
	@Parent Key<IrssiNotifierUser> owner;
	
	private final String WP7_TEMPLATE = "<wp:Notification xmlns:wp=\"WPNotification\">" +
			"<wp:Tile>" +
			"<wp:Count/>" +
			"</wp:Tile>" +
			"</wp:Notification>";
	
	private final String WP8_FLIP_TEMPLATE = "<wp:Notification xmlns:wp=\"WPNotification\" Version=\"2.0\">" +
			"<wp:Tile Template=\"FlipTile\">" +
			"<wp:WideBackContent/>" +
			"<wp:Count/>" +
			"<wp:BackContent/>" +
			"</wp:Tile>" +
			"</wp:Notification>";
	
	private final String WP8_ICONIC_TEMPLATE = "<wp:Notification xmlns:wp=\"WPNotification\" Version=\"2.0\">" +
			"<wp:Tile Template=\"IconicTile\">" +
			"<wp:WideContent1/>" +
			"<wp:WideContent2/>" +
			"<wp:WideContent3/>" +
			"<wp:Count/>" +
			"</wp:Tile>" +
			"</wp:Notification>";
	
	public Message(){
		this.timestamp = System.currentTimeMillis();
		this.id = this.timestamp;
	}
	
	public Message(String nick, String channel, String message, IrssiNotifierUser user){
		this.timestamp = System.currentTimeMillis();
		this.id = this.timestamp;
		this.nick = nick;
		this.channel = channel;
		this.message = message.replaceAll("[\u0000-\u001f]", "");
		this.owner = new Key<IrssiNotifierUser>(user.getClass(), user.UserID);
	}
	
	private String getFromString() {
		return channel.equals("PRIVATE") ? nick : nick + " @ " + channel;
	}
	
	private String getTimeAtTimezone(String timeZoneOffset) {
		DateFormat format = new SimpleDateFormat("HH:mm:ss");
		return format.format(this.getDateInTimeZone("GMT"+timeZoneOffset));
	}
	
	public String GenerateToastNotification() throws XmlGeneratorException{
		
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			doc.setXmlStandalone(true);
			Element root = doc.createElement("wp:Notification");
			root.setAttribute("xmlns:wp", "WPNotification");
			doc.appendChild(root);
			Element toast = doc.createElement("wp:Toast");
			root.appendChild(toast);
			Element text1 = doc.createElement("wp:Text1");
			toast.appendChild(text1);
			text1.appendChild(doc.createTextNode(getFromString()));
			Element text2 = doc.createElement("wp:Text2");
			toast.appendChild(text2);
			text2.appendChild(doc.createTextNode(this.message));
			Element param = doc.createElement("wp:Param");
			toast.appendChild(param);
			param.appendChild(doc.createTextNode(IrssiNotifier.HILITEPAGEURL+"?NavigatedFrom=Toast"));
			
			StringWriter output = new StringWriter();

		    Transformer transformer = TransformerFactory.newInstance().newTransformer();
		    transformer.transform(new DOMSource(doc), new StreamResult(output));
		    
		    return output.toString();
			
		} catch (ParserConfigurationException e) {
			throw new XmlGeneratorException(e);
		} catch (TransformerConfigurationException e) {
			throw new XmlGeneratorException(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new XmlGeneratorException(e);
		} catch (TransformerException e) {
			throw new XmlGeneratorException(e);
		}
	}
	
	public String GenerateTileNotification(int countValue, String tileUrl, TileType template, String timeZoneOffset) throws XmlGeneratorException{
		if(template == null) {
			template = TileType.WP7;
		}
		if(timeZoneOffset == null) {
			timeZoneOffset = "";
		}
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc;
			switch(template) {
			case WP7:
				doc = docBuilder.parse(new ByteArrayInputStream(WP7_TEMPLATE.getBytes("UTF-8")));
				break;
			case WP8_FLIP:
				doc = docBuilder.parse(new ByteArrayInputStream(WP8_FLIP_TEMPLATE.getBytes("UTF-8")));
				doc.getElementsByTagName("wp:WideBackContent").item(0).appendChild(doc.createTextNode(getFromString()+"\n"+getTimeAtTimezone(timeZoneOffset)+"\n"+message));
				doc.getElementsByTagName("wp:BackContent").item(0).appendChild(doc.createTextNode(getFromString()+"\n"+getTimeAtTimezone(timeZoneOffset)+"\n"+message));
				break;
			case WP8_ICONIC:
				doc = docBuilder.parse(new ByteArrayInputStream(WP8_ICONIC_TEMPLATE.getBytes("UTF-8")));
				doc.getElementsByTagName("wp:WideContent1").item(0).appendChild(doc.createTextNode(getFromString()));
				doc.getElementsByTagName("wp:WideContent2").item(0).appendChild(doc.createTextNode(getTimeAtTimezone(timeZoneOffset)));
				doc.getElementsByTagName("wp:WideContent3").item(0).appendChild(doc.createTextNode(message));
				break;
			default:
				doc = docBuilder.parse(new ByteArrayInputStream(WP7_TEMPLATE.getBytes("UTF-8")));
				break;
			}
			doc.setXmlStandalone(true);
			((Element)doc.getElementsByTagName("wp:Tile").item(0)).setAttribute("Id", tileUrl);
			doc.getElementsByTagName("wp:Count").item(0).appendChild(doc.createTextNode(countValue+""));
			
			StringWriter output = new StringWriter();

		    Transformer transformer = TransformerFactory.newInstance().newTransformer();
		    transformer.transform(new DOMSource(doc), new StreamResult(output));
		    
		    return output.toString();
			
		} catch (ParserConfigurationException e) {
			throw new XmlGeneratorException(e);
		} catch (TransformerConfigurationException e) {
			throw new XmlGeneratorException(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new XmlGeneratorException(e);
		} catch (TransformerException e) {
			throw new XmlGeneratorException(e);
		} catch (SAXException e) {
			throw new XmlGeneratorException(e);
		} catch (IOException e) {
			throw new XmlGeneratorException(e);
		}
	}
	
	public Date getDateInTimeZone(String timeZoneId) {
		Calendar temp = new GregorianCalendar(TimeZone.getTimeZone(timeZoneId));
		temp.setTimeInMillis(this.timestamp);
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, temp.get(Calendar.YEAR));
		cal.set(Calendar.MONTH, temp.get(Calendar.MONTH));
		cal.set(Calendar.DAY_OF_MONTH, temp.get(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY));
		cal.set(Calendar.MINUTE, temp.get(Calendar.MINUTE));
		cal.set(Calendar.SECOND, temp.get(Calendar.SECOND));
		cal.set(Calendar.MILLISECOND, temp.get(Calendar.MILLISECOND));
	
		return cal.getTime();
	}
}
