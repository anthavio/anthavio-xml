/**
 * 
 */
package net.anthavio.xml.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import net.anthavio.log.ToString;
import net.anthavio.log.ToStringBuilder;
import net.anthavio.xml.StringSource;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * @author vanek
 *
 */
public class SimpleJaxbTest {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void testJaxbParser() throws Exception {
		JAXBContext jaxbCtx = JAXBContext.newInstance(JaxbTestClass.class);

		JaxbTestClass obj1 = buildJaxbClass();

		log.debug("\n" + obj1);

		StringWriter out = new StringWriter();
		jaxbCtx.createMarshaller().marshal(obj1, out);
		log.debug("\n" + out.toString());

		JaxbTestClass obj2 = (JaxbTestClass) jaxbCtx.createUnmarshaller().unmarshal(new StringSource(out.toString()));

		assertThat(obj1.list).isEqualTo(obj2.list);
		assertThat(obj1.map).isEqualTo(obj2.map);
		assertThat(obj1.cisla).isEqualTo(obj2.cisla);
		assertThat(obj1.array).isEqualTo(obj2.array);
		assertThat(obj1.nelogovat).isEqualTo(obj2.nelogovat);
		assertThat(obj1.hashed).isEqualTo(obj2.hashed);
	}

	@Test
	public void testKmxToStingBuilder() {
		JaxbTestClass obj1 = buildJaxbClass();
		EventStoringAppender.getEvents().clear();
		log.debug("\n" + obj1);
		ILoggingEvent enterEvent = EventStoringAppender.getEvents().get(0);
		assertThat(enterEvent.getMessage()).contains("list=[Brekeke, Kvakvakva]");
		assertThat(enterEvent.getMessage()).contains("cisla=[1, 1.1, 2, 3]");
		assertThat(enterEvent.getMessage()).contains("map={Cosi=Kdesi}");
		assertThat(enterEvent.getMessage()).contains("array={Prase,Ovce,Godzilla}");
		assertThat(enterEvent.getMessage()).contains("rekurze=JaxbTestClass[1]");
		assertThat(enterEvent.getMessage()).doesNotContain("hashed=tajny text");
		assertThat(enterEvent.getMessage()).doesNotContain("nelogovat=");

	}

	/**
	 * Pozor, ty hodnoty slouzi i k testu KmxToStingBuilder!
	 */
	private JaxbTestClass buildJaxbClass() {
		JaxbTestClass obj1 = new JaxbTestClass();
		obj1.map = new HashMap<String, String>();
		obj1.map.put("Cosi", "Kdesi");
		obj1.list = new ArrayList<String>();
		obj1.list.add("Brekeke");
		obj1.list.add("Kvakvakva");

		obj1.cisla = new ArrayList<Number>();
		obj1.cisla.add(new Integer(1));
		obj1.cisla.add(new Double(1.1));
		obj1.cisla.add(new BigInteger("2"));
		obj1.cisla.add(new BigDecimal(3));

		obj1.array = new String[] { "Prase", "Ovce", "Godzilla" };

		obj1.hashed = "tajny text";
		obj1.nelogovat = "nevypisovany text";

		obj1.rekurze = new JaxbTestClass[] { new JaxbTestClass() };
		return obj1;
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	static class JaxbTestClass {

		@XmlElementWrapper(name = "seznam", required = true)
		@XmlElement(name = "polozka")
		@ToString(detail = true)
		private List<String> list = new ArrayList<String>();

		@XmlElements({ //
		@XmlElement(name = "integer", type = Integer.class), @XmlElement(name = "double", type = Double.class),
				@XmlElement(name = "bigInteger", type = BigInteger.class),
				@XmlElement(name = "bigDecimal", type = BigDecimal.class)
		//
		})
		@XmlElementWrapper(name = "podtridy")
		@ToString(detail = true)
		private List<Number> cisla;

		@ToString(detail = true)
		private Map<String, String> map = new HashMap<String, String>();

		@XmlElementWrapper(name = "polevole")
		@XmlElement(name = "radek")
		@ToString(detail = true)
		private String[] array;

		private JaxbTestClass[] rekurze;

		@ToString(hide = true)
		private String nelogovat;

		@ToString(hash = true)
		private String hashed;

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this);
			//return ReflectionToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
		}

	}
}
