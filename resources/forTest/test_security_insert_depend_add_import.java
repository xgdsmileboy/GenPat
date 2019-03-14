package code;

public class SAXParseService {
	public static Monitor readXml(InputStream inStream) throws Exception {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser saxParser = spf.newSAXParser();
		MonitorHandler handler = new MonitorHandler();
		saxParser.parse(inStream, handler);
		inStream.close();
		return handler.getMonitor();
	}
}