package org.common.util.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

public class StringTrimmerModule extends SimpleModule {

	private static final long serialVersionUID = 1L;

	public StringTrimmerModule() {

		addDeserializer(String.class, new StdScalarDeserializer<String>(String.class) {
			private static final long serialVersionUID = 1L;

			@Override
			public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
				return jp.getValueAsString().trim();
			}
		});
	}
}