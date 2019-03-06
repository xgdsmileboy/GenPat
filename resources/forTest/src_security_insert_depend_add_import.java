package code;

public class Test {
	
	public void parse(final InputSource is) {
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			final SAXParser parser = factory.newSAXParser();
			parser.parse(is, this);
		} catch (final Exception e) {
			throw new GsaConfigException("Failed to parse XML file.", e);
		}
	}

}
