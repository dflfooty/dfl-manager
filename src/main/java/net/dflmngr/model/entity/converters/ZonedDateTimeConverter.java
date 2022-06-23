package net.dflmngr.model.entity.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import net.dflmngr.utils.DflmngrUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

@Converter(autoApply = true)
public class ZonedDateTimeConverter implements AttributeConverter<ZonedDateTime, Date> {
	@Override
	public Date convertToDatabaseColumn(ZonedDateTime date) {
		if (date != null) {
			Instant instant = Instant.from(date);
			return Date.from(instant);
		} else {
			return null;
		}
	}

	@Override
	public ZonedDateTime convertToEntityAttribute(Date value) {
		if (value != null) {
			Instant instant = value.toInstant();
			return ZonedDateTime.ofInstant(instant, ZoneId.of(DflmngrUtils.DEFAULT_TIMEZONE));
		} else {
			return null;
		}
	}
}
