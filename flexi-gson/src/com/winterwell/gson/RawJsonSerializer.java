package com.winterwell.gson;

import java.lang.reflect.Type;

final class RawJsonSerializer implements JsonSerializer<RawJson> {
	@Override
	public JsonElement serialize(RawJson src, Type srcType,
			JsonSerializationContext context) {
		return src;
	}
}
