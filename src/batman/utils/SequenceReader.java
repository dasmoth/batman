package batman.utils;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

public class SequenceReader extends Reader {

	private final Iterator<? extends Reader> streams;
	private Reader currentStream;
	
	private boolean hasNextStream()
		throws IOException
	{
		try {
			return streams.hasNext();
		} catch (Exception ex) {
			IOException e2 = new IOException("Couldn't open next stream");
			e2.initCause(ex);
			throw e2;
		}
	}	
	
	private Reader nextStream()
		throws IOException
	{
		try {
			return streams.next();
		} catch (Exception ex) {
			IOException e2 = new IOException("Couldn't open next stream");
			e2.initCause(ex);
			throw e2;
		}
	}
	
	public SequenceReader(Iterable<? extends Reader> streamSource) 
		throws IOException
	{
		super();
		streams = streamSource.iterator();
		currentStream = nextStream();
	}
	
	@Override
	public void close() throws IOException {
		currentStream.close();
		
		/* Should we do this?
		
		while (streams.hasNext()) {
			streams.next().close();
		}
		
		*/
		currentStream = null;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		if (currentStream == null) {
			return -1;
		}
		int read = currentStream.read(cbuf, off, len);
		if (read < 0) {
			currentStream.close();
			if (hasNextStream()) {
				currentStream = nextStream();
				return read(cbuf, off, len);
			} else {
				currentStream = null;
			}
		}
		return read;
	}

}
