package org.yaml.snakeyaml;

import org.yaml.snakeyaml.composer.IgnoreComposer;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.io.InputStream;

/**
 * Yaml that ignore some issues on parsing.
 */
public class IgnoreYaml extends Yaml {

    @Override
    public Object load(InputStream io) {
        return this.loadFromReader(new StreamReader(new UnicodeReader(io)), Object.class);
    }

    private Object loadFromReader(StreamReader sreader, Class<?> type) {
        IgnoreComposer composer = new IgnoreComposer(new ParserImpl(sreader), this.resolver);
        this.constructor.setComposer(composer);
        return this.constructor.getSingleData(type);
    }
}
