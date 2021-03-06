/*
 * Copyright (C) 2018 Yoann Despréaux
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING . If not, write to the
 * Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * Please send bugreports with examples or suggestions to yoann.despreaux@believeit.fr
 */
package com.github.ydespreaux.spring.data.elasticsearch.core.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * DateTimeConverters
 *
 * @author Yoann Despréaux
 * @since 1.0.0
 */
public final class DateTimeConverters {

    public enum LocalDateTimeConverter implements Converter<LocalDateTime, String> {
        INSTANCE;

        @Nullable
        @Override
        public String convert(@Nullable LocalDateTime source) {
            if (source == null) {
                return null;
            }
            return source.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

    }

    public enum LocalDateConverter implements Converter<LocalDate, String> {
        INSTANCE;

        @Nullable
        @Override
        public String convert(@Nullable LocalDate source) {
            if (source == null) {
                return null;
            }
            return source.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

    }

    public enum JavaDateConverter implements Converter<Date, String> {
        INSTANCE;

        @Nullable
        @Override
        public String convert(@Nullable Date source) {
            if (source == null) {
                return null;
            }
            return source.toInstant()
                    .atOffset(ZoneOffset.UTC)
                    .toLocalDateTime()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

    }
}
