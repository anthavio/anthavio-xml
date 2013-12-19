package net.anthavio.xml.test;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * 
 * @author vanek
 * 
 * Pro testovani ToString
 */
public class EventStoringAppender extends AppenderBase<ILoggingEvent> {

	int limit = 1000;

	private static final List<ILoggingEvent> events = new ArrayList<ILoggingEvent>();

	public static List<ILoggingEvent> getEvents() {
		return events;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	@Override
	public void start() {
		super.start();
	}

	@Override
	public void append(ILoggingEvent event) {
		events.add(event);
	}

}
