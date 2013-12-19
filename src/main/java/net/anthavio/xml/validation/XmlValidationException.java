package net.anthavio.xml.validation;

import java.util.List;

import net.anthavio.xml.XmlParseException;


/**
 * 
 * @author martin.vanek
 *
 */
public class XmlValidationException extends XmlParseException {

	private static final long serialVersionUID = 9166282786729649645L;

	private final List<ValidationEventImpl> events;

	public XmlValidationException(List<ValidationEventImpl> events) {
		super(buildMessage(events));

		this.events = events;
	}

	public List<ValidationEventImpl> getEvents() {
		return events;
	}

	private static String buildMessage(List<ValidationEventImpl> events) {
		if (events == null || events.size() == 0) {
			throw new IllegalArgumentException("Validation Events list must not be null or empty");
		}
		if (events.size() == 1) {
			return events.get(0).toString();
		} else {
			StringBuilder sb = new StringBuilder(events.size() + " Errors");
			for (ValidationEventImpl event : events) {
				sb.append('\n').append(event.toString());
			}
			return sb.toString();
		}
	}
	/*
		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
		}
	*/
}
