/*
 *  Copyright © 2017-2018 Sharder Foundation.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  version 2 as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, you can visit it at:
 *  https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 *
 *  This software uses third party libraries and open-source programs,
 *  distributed under licenses described in 3RD-PARTY-LICENSES.
 *
 */

package org.conch.util;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.conch.Conch;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JSON {

    private JSON() {} //never

    public final static JSONStreamAware emptyJSON = prepare(new JSONObject());

    public static JSONStreamAware prepare(final JSONObject json) {
        return new JSONStreamAware() {
            private final char[] jsonChars = JSON.toJSONString(json).toCharArray();
            @Override
            public void writeJSONString(Writer out) throws IOException {
                out.write(jsonChars);
            }
        };
    }

    public static JSONStreamAware prepareRequest(final JSONObject json) {
        json.put("protocol", 1);
        return prepare(json);
    }

    public static String toString(JSONStreamAware jsonStreamAware) {
        StringWriter stringWriter = new StringWriter();
        try {
            JSON.writeJSONString(jsonStreamAware, stringWriter);
        } catch (IOException ignore) {}
        return stringWriter.toString();
    }

    /** String escape pattern */
    private static final Pattern pattern = Pattern.compile(
            "[\"\\\\\\u0008\\f\\n\\r\\t/\\u0000-\\u001f\\u007f-\\u009f\\u2000-\\u20ff\\ud800-\\udbff]");

    /**
     * Create a formatted JSON string
     *
     * @param   json                            JSON list or map
     * @return                                  Formatted string
     */
    public static String toJSONString(JSONAware json) {
        if (json == null)
            return "null";
        if (json instanceof Map) {
            StringBuilder sb = new StringBuilder(1024);
            encodeObject((Map)json, sb);
            return sb.toString();
        }
        if (json instanceof List) {
            StringBuilder sb = new StringBuilder(1024);
            encodeArray((List)json, sb);
            return sb.toString();
        }
        return json.toJSONString();
    }

    /**
     * Write a formatted JSON string
     *
     * @param   json                            JSON list or map
     * @param   writer                          Writer
     * @throws  IOException                     I/O error occurred
     */
    public static void writeJSONString(JSONStreamAware json, Writer writer) throws IOException {
        if (json == null) {
            writer.write("null");
            return;
        }
        if (json instanceof Map) {
            StringBuilder sb = new StringBuilder(1024);
            encodeObject((Map)json, sb);
            writer.write(sb.toString());
            return;
        }
        if (json instanceof List) {
            StringBuilder sb = new StringBuilder(1024);
            encodeArray((List)json, sb);
            writer.write(sb.toString());
            return;
        }
        json.writeJSONString(writer);
    }

    /**
     * Create a formatted string from a list
     *
     * @param   list                            List
     * @param   sb                              String builder
     */
    private static void encodeArray(List<?> list, StringBuilder sb) {
        if (list == null) {
            sb.append("null");
            return;
        }
        boolean firstElement = true;
        sb.append('[');
        for (Object obj : list) {
            if (firstElement)
                firstElement = false;
            else
                sb.append(',');
            encodeValue(obj, sb);
        }
        sb.append(']');
    }

    /**
     * Create a formatted string from a map
     *
     * @param   map                             Map
     * @param   sb                              String builder
     */
    public static void encodeObject(Map<?, ?> map, StringBuilder sb) {
        if (map == null) {
            sb.append("null");
            return;
        }
        Set<Map.Entry<Object, Object>> entries = (Set)map.entrySet();
        Iterator<Map.Entry<Object, Object>> it = entries.iterator();
        boolean firstElement = true;
        sb.append('{');
        while (it.hasNext()) {
            Map.Entry<Object, Object> entry = it.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key == null)
                continue;
            if (firstElement)
                firstElement = false;
            else
                sb.append(',');
            sb.append('\"').append(key.toString()).append("\":");
            encodeValue(value, sb);
        }
        sb.append('}');
    }

    /**
     * Encode a JSON value
     *
     * @param   value                           JSON value
     * @param   sb                              String builder
     */
    public static void encodeValue(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Double) {
            if (((Double)value).isInfinite() || ((Double)value).isNaN())
                sb.append("null");
            else
                sb.append(value.toString());
        } else if (value instanceof Float) {
            if (((Float)value).isInfinite() || ((Float)value).isNaN())
                sb.append("null");
            else
                sb.append(value.toString());
        } else if (value instanceof Number) {
            sb.append(value.toString());
        } else if (value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof Map) {
            encodeObject((Map<Object, Object>)value, sb);
        } else if (value instanceof List) {
            encodeArray((List<Object>)value, sb);
        } else {
            sb.append('\"');
            escapeString(value.toString(), sb);
            sb.append('\"');
        }
    }

    /**
     * Escape control characters in a string and append them to the string buffer
     *
     * @param   string                      String to be written
     * @param   sb                          String builder
     */
    private static void escapeString(String string, StringBuilder sb) {
        if (string.length() == 0)
            return;
        //
        // Find the next special character in the string
        //
        int start = 0;
        Matcher matcher = pattern.matcher(string);
        while (matcher.find(start)) {
            int pos = matcher.start();
            if (pos > start)
                sb.append(string.substring(start, pos));
            start = pos + 1;
            //
            // Escape control characters
            //
            char c = string.charAt(pos);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                default:
                    if((c>='\u0000' && c<='\u001F') || (c>='\u007F' && c<='\u009F') || (c>='\u2000' && c<='\u20FF')){
                        sb.append("\\u").append(String.format("%04X", (int)c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        //
        // Append the remainder of the string
        //
        if (start == 0)
            sb.append(string);
        else if (start < string.length())
            sb.append(string.substring(start));
    }

    /**
     * Read Json file,return jsonStr
     * @param fileName
     * @return
     */
    public static String readJsonFile(String fileName) {
        String jsonStr = "";
        try {
            File jsonFile = new File(fileName);
            FileReader fileReader = new FileReader(jsonFile);

            Reader reader = new InputStreamReader(new FileInputStream(jsonFile),"utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (IOException e) {
            e.printStackTrace();
            Logger.logInfoMessage("Cannot read file " + fileName + " error " + e.getMessage());

            throw new IllegalArgumentException(String.format("Error loading file %s", fileName));
        }
    }

    /**
     * JsonObject write to Json file
     * @param jsonObject
     * @param fileName
     */
    public static void JsonWrite(JSONObject jsonObject, String fileName){

        try {
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(fileName),"UTF-8");
            // json format output
            com.alibaba.fastjson.JSONObject object = com.alibaba.fastjson.JSONObject.parseObject(jsonObject.toJSONString());
            String pretty = com.alibaba.fastjson.JSON.toJSONString(object, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
                    SerializerFeature.WriteDateUseDateFormat);
            osw.write(pretty);
            // clear the buffer, forcing the output of data
            osw.flush();
            // close the output stream
            osw.close();
        } catch (IOException e) {
            e.printStackTrace();
            Logger.logInfoMessage("Cannot write to file " + fileName + " error " + e.getMessage());

            throw new IllegalArgumentException(String.format("Error write to file %s", fileName));
        }
    }

    /**
     * Json file convert to CSV file
     * @param sourceJsonPathName Contains only one array
     * @param targetCSVPathName
     */
    public static void JsonToCSV(String sourceJsonPathName, String targetCSVPathName) {
        try {
            JsonNode jsonTree = new ObjectMapper().readTree(new File(sourceJsonPathName)).elements().next();
            CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();

            JsonNode firstObject = jsonTree.elements().next();
            firstObject.fieldNames().forEachRemaining(fieldName -> {csvSchemaBuilder.addColumn(fieldName);});
            CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
            CsvMapper csvMapper = new CsvMapper();
            csvMapper.writerFor(JsonNode.class)
                    .with(csvSchema)
                    .writeValue(new File(targetCSVPathName), jsonTree);
        } catch (IOException e) {
            e.printStackTrace();
            Logger.logInfoMessage("Cannot convert file " + sourceJsonPathName + " to " + targetCSVPathName + ", error: " + e.getMessage());

            throw new IllegalArgumentException(String.format("Cannot convert file %s to %s", sourceJsonPathName, targetCSVPathName));
        }
    }
}
