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

package com.github.ydespreaux.spring.data.elasticsearch.core.converter.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.ydespreaux.spring.data.elasticsearch.core.geo.GeoShapeOrientation;

import java.io.IOException;

/**
 * GeoShapeOrientationSerializer
 *
 * @author Yoann Despréaux
 * @since 1.0.2
 */
public class GeoShapeOrientationSerializer extends JsonSerializer<GeoShapeOrientation> {

    @Override
    public void serialize(GeoShapeOrientation orientation, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (orientation != null) {
            jsonGenerator.writeString(orientation.orientationName());
        } else {
            jsonGenerator.writeString(GeoShapeOrientation.COUNTER_CLOCKWISE.orientationName());
        }
    }
}
