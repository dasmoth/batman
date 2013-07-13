package batman;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import batman.matrix.Matrix1D;
import batman.matrix.SimpleMatrix1D;

public class RoiProfileReader {
	private XMLStreamReader xr;
	private boolean isReady;
	
	public RoiProfileReader(XMLStreamReader xr) throws XMLStreamException {
		super();
		this.xr = xr;
		skipToNext();
	}
	
	public boolean hasNext() {
		return isReady;
	}
	
	public RoiProfile next() 
		throws XMLStreamException 
	{
		RoiProfile rp = new RoiProfile();
		String name = xr.getAttributeValue(null, "name");
		rp.name = name;
		if (xr.getAttributeValue(null, "minPos") !=  null) {
			rp.min = Integer.parseInt(xr.getAttributeValue(null, "minPos"));
			rp.max = Integer.parseInt(xr.getAttributeValue(null, "maxPos"));
		}
		
		List<CGRecord> cgrs = new ArrayList<CGRecord>();
		skipToNextElement("design");
		int evt;
		while (true) {
			evt = xr.nextTag();
			if (evt == XMLStreamConstants.START_ELEMENT && xr.getLocalName().equals("cpg")) {
				String seq = xr.getAttributeValue(null, "seq");
				int pos = Integer.parseInt(xr.getAttributeValue(null, "pos"));
				double w = Double.parseDouble(xr.getAttributeValue(null, "weight"));
				cgrs.add(new CGRecord(seq, pos, w));
			} else if (evt == XMLStreamConstants.END_ELEMENT && xr.getLocalName().equals("design")) {
				break;
			}
		} 
		rp.cgs = cgrs.toArray(new CGRecord[0]);
		
		rp.samples = new ArrayList<Matrix1D>();
		skipToNextElement("samples");
		while (true) {
			evt = xr.nextTag();
			if (evt == XMLStreamConstants.START_ELEMENT && xr.getLocalName().equals("sample")) {
				String raw = readTextElement(xr);
				String[] svals = raw.split(" ");
				Matrix1D vals = new SimpleMatrix1D(svals.length);
				for (int p = 0; p < svals.length; ++p) {
					vals.set(p, Double.parseDouble(svals[p]));
				}
				rp.samples.add(vals);
			} else if (evt == XMLStreamConstants.END_ELEMENT && xr.getLocalName().equals("samples")) {
				break;
			}
		} 
		
		skipToNext();
		return rp;
	}
	
	private boolean skipToNextElement(String name) throws XMLStreamException {
        while (true) {
            int evt = xr.next();
            if (evt == XMLStreamConstants.START_ELEMENT) {
                if (xr.getLocalName().equals(name)) {
                    return true;
                }
            } else if (evt == XMLStreamConstants.END_DOCUMENT) {
                return false;
            }
        }
	}
	
	private void skipToNext() throws XMLStreamException {
        isReady = skipToNextElement("roi");
	}
	
	private static String readTextElement(XMLStreamReader xr) 
		throws XMLStreamException
	{
		StringBuilder sb = new StringBuilder();
		while (true) {
			final int evt = xr.next();
			if (evt == XMLStreamConstants.CHARACTERS || evt == XMLStreamConstants.CDATA || evt == XMLStreamConstants.SPACE) {
				sb.append(xr.getText());
			} else if (evt == XMLStreamConstants.START_ELEMENT) {
				throw new XMLStreamException("Child element found in unexpected location");
			} else if (evt == XMLStreamConstants.END_ELEMENT) {
				return sb.toString();
			}
		} 
	}
}
