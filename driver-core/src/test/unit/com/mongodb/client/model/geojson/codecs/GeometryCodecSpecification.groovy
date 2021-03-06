/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.client.model.geojson.codecs

import com.mongodb.client.model.geojson.GeoJsonObjectType
import com.mongodb.client.model.geojson.Geometry
import com.mongodb.client.model.geojson.GeometryCollection
import com.mongodb.client.model.geojson.LineString
import com.mongodb.client.model.geojson.MultiLineString
import com.mongodb.client.model.geojson.MultiPoint
import com.mongodb.client.model.geojson.MultiPolygon
import com.mongodb.client.model.geojson.Point
import com.mongodb.client.model.geojson.Polygon
import com.mongodb.client.model.geojson.PolygonCoordinates
import com.mongodb.client.model.geojson.Position
import org.bson.BsonDocument
import org.bson.BsonDocumentReader
import org.bson.BsonDocumentWriter
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecConfigurationException
import spock.lang.Specification

import static org.bson.BsonDocument.parse
import static org.bson.codecs.configuration.CodecRegistries.fromProviders

class GeometryCodecSpecification extends Specification {
    def registry = fromProviders([new GeoJsonCodecProvider()])
    def codec = registry.get(Geometry)
    def writer = new BsonDocumentWriter(new BsonDocument())
    def context = EncoderContext.builder().build()

    def 'should round trip known geometries'() {
        when:
        codec.encode(writer, geometry, context)

        then:
        writer.document == parse(geoJson)

        when:
        def decodedGeometry = codec.decode(new BsonDocumentReader(writer.document), DecoderContext.builder().build())

        then:
        geometry == decodedGeometry

        where:
        geometry << [
                new LineString([new Position(101d, 0d), new Position(102d, 1d)]),
                new MultiLineString([[new Position([1.0d, 1.0d]), new Position([2.0d, 2.0d]), new Position([3.0d, 4.0d])],
                                     [new Position([2.0d, 3.0d]), new Position([3.0d, 2.0d]), new Position([4.0d, 4.0d])]]),
                new Point(new Position(100d, 0d)),
                new MultiPoint([new Position([40.0d, 18.0d]), new Position([40.0d, 19.0d]), new Position([41.0d, 19.0d])]),
                new Polygon([new Position([40.0d, 18.0d]), new Position([40.0d, 19.0d]), new Position([41.0d, 19.0d]),
                             new Position([40.0d, 18.0d])]),
                new MultiPolygon([new PolygonCoordinates([new Position(102.0, 2.0), new Position(103.0, 2.0),
                                                          new Position(103.0, 3.0), new Position(102.0, 3.0),
                                                          new Position(102.0, 2.0)]),
                                  new PolygonCoordinates([new Position(100.0, 0.0), new Position(101.0, 0.0),
                                                          new Position(101.0, 1.0), new Position(100.0, 1.0),
                                                          new Position(100.0, 0.0)],
                                          [new Position(100.2, 0.2), new Position(100.8, 0.2),
                                           new Position(100.8, 0.8), new Position(100.2, 0.8),
                                           new Position(100.2, 0.2)])]),
                new GeometryCollection([new Point(new Position(100d, 0d)),
                                        new LineString([new Position(101d, 0d), new Position(102d, 1d)])])
        ]

        geoJson << [
                '{type: "LineString", coordinates: [ [101.0, 0.0], [102.0, 1.0] ]}',
                '{type: "MultiLineString", coordinates: [[[1.0, 1.0], [2.0, 2.0], [3.0, 4.0]], [[2.0, 3.0], [3.0, 2.0], [4.0, 4.0]]]}',
                '{type: "Point", coordinates: [100.0, 0.0]}',
                '{type: "MultiPoint", coordinates: [[40.0, 18.0], [40.0, 19.0], [41.0, 19.0]]}',
                '{type: "Polygon", coordinates: [[[40.0, 18.0], [40.0, 19.0], [41.0, 19.0], [40.0, 18.0]]]}',
                '''{type: "MultiPolygon", coordinates: [[[[102.0, 2.0], [103.0, 2.0], [103.0, 3.0], [102.0, 3.0], [102.0, 2.0]]],
                                                     [[[100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0]],
                                                      [[100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2]]]]}''',
                '''{ type: "GeometryCollection", geometries: [{ type: "Point", coordinates: [100.0, 0.0]},
                                                           { type: "LineString", coordinates: [ [101.0, 0.0], [102.0, 1.0] ]}]}'''
        ]
    }

    def 'should throw when decoding invalid documents'() {
        when:
        codec.decode(new BsonDocumentReader(parse(invalidJson)), DecoderContext.builder().build())

        then:
        thrown(CodecConfigurationException)

        where:
        invalidJson << [
                '{type: "GeoShard", coordinates: [100.0, 0.0]}',
                '{coordinates: [100.0, 0.0]}',
                '{type: "Point", coordinates: [40.0, 18.0], crs : {type: "link", properties: {href: "http://example.com/crs/42"}}}',
        ]
    }

    def 'should not support unknown geometries'() {
        given:
        def geometry = new Geometry() {
            @Override
            GeoJsonObjectType getType() {
                GeoJsonObjectType.POINT
            }
        }


        when:
        codec.encode(writer, geometry, context)

        then:
        thrown(CodecConfigurationException)
    }
}
