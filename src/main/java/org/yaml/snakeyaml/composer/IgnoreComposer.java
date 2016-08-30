package org.yaml.snakeyaml.composer;

import java.util.*;

import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.events.*;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.resolver.Resolver;

/**
 * Created by lucboutier on 26/05/2016.
 */
public class IgnoreComposer extends Composer {
    private final Parser parser;
    private final Resolver resolver;
    private final Map<String, Node> anchors;
    private final Set<Node> recursiveNodes;

    public IgnoreComposer(Parser parser, Resolver resolver) {
        super(parser, resolver);
        this.parser = parser;
        this.resolver = resolver;
        this.anchors = new HashMap();
        this.recursiveNodes = new HashSet();
    }

    public boolean checkNode() {
        if (this.parser.checkEvent(Event.ID.StreamStart)) {
            this.parser.getEvent();
        }

        return !this.parser.checkEvent(Event.ID.StreamEnd);
    }

    public Node getNode() {
        return !this.parser.checkEvent(Event.ID.StreamEnd) ? this.composeDocument() : null;
    }

    public Node getSingleNode() {
        this.parser.getEvent();
        Node document = null;
        if (!this.parser.checkEvent(Event.ID.StreamEnd)) {
            document = this.composeDocument();
        }

        if (!this.parser.checkEvent(Event.ID.StreamEnd)) {
            Event event = this.parser.getEvent();
            throw new ComposerException("expected a single document in the stream", document.getStartMark(), "but found another document",
                    event.getStartMark());
        } else {
            this.parser.getEvent();
            return document;
        }
    }

    private Node composeDocument() {
        this.parser.getEvent();
        Node node = this.composeNode((Node) null);
        this.parser.getEvent();
        this.anchors.clear();
        this.recursiveNodes.clear();
        return node;
    }

    private Node composeNode(Node parent) {
        this.recursiveNodes.add(parent);
        String anchor;
        Node node;
        if (this.parser.checkEvent(Event.ID.Alias)) {
            AliasEvent event1 = (AliasEvent) this.parser.getEvent();
            anchor = event1.getAnchor();
            if (!this.anchors.containsKey(anchor)) {
                // throw new ComposerException((String) null, (Mark) null, "found undefined alias " + anchor, event1.getStartMark());
                return new ScalarNode(Tag.STR, true, anchor, event1.getStartMark(), event1.getEndMark(), Character.MAX_VALUE);
            } else {
                node = (Node) this.anchors.get(anchor);
                if (this.recursiveNodes.remove(node)) {
                    node.setTwoStepsConstruction(true);
                }

                return node;
            }
        } else {
            NodeEvent event = (NodeEvent) this.parser.peekEvent();
            anchor = null;
            anchor = event.getAnchor();
            if (anchor != null && this.anchors.containsKey(anchor)) {
                throw new ComposerException("found duplicate anchor " + anchor + "; first occurence", ((Node) this.anchors.get(anchor)).getStartMark(),
                        "second occurence", event.getStartMark());
            } else {
                node = null;
                if (this.parser.checkEvent(Event.ID.Scalar)) {
                    node = this.composeScalarNode(anchor);
                } else if (this.parser.checkEvent(Event.ID.SequenceStart)) {
                    node = this.composeSequenceNode(anchor);
                } else {
                    node = this.composeMappingNode(anchor);
                }

                this.recursiveNodes.remove(parent);
                return node;
            }
        }
    }

    private Node composeScalarNode(String anchor) {
        ScalarEvent ev = (ScalarEvent) this.parser.getEvent();
        String tag = ev.getTag();
        boolean resolved = false;
        Tag nodeTag;
        if (tag != null && !tag.equals("!")) {
            nodeTag = new Tag(tag);
        } else {
            nodeTag = this.resolver.resolve(NodeId.scalar, ev.getValue(), ev.getImplicit().canOmitTagInPlainScalar());
            resolved = true;
        }

        ScalarNode node = new ScalarNode(nodeTag, resolved, ev.getValue(), ev.getStartMark(), ev.getEndMark(), ev.getStyle());
        if (anchor != null) {
            this.anchors.put(anchor, node);
        }

        return node;
    }

    private Node composeSequenceNode(String anchor) {
        SequenceStartEvent startEvent = (SequenceStartEvent) this.parser.getEvent();
        String tag = startEvent.getTag();
        boolean resolved = false;
        Tag nodeTag;
        if (tag != null && !tag.equals("!")) {
            nodeTag = new Tag(tag);
        } else {
            nodeTag = this.resolver.resolve(NodeId.sequence, (String) null, startEvent.getImplicit());
            resolved = true;
        }

        ArrayList children = new ArrayList();
        SequenceNode node = new SequenceNode(nodeTag, resolved, children, startEvent.getStartMark(), (Mark) null, startEvent.getFlowStyle());
        if (anchor != null) {
            this.anchors.put(anchor, node);
        }

        for (int index = 0; !this.parser.checkEvent(Event.ID.SequenceEnd); ++index) {
            children.add(this.composeNode(node));
        }

        Event endEvent = this.parser.getEvent();
        node.setEndMark(endEvent.getEndMark());
        return node;
    }

    private Node composeMappingNode(String anchor) {
        MappingStartEvent startEvent = (MappingStartEvent) this.parser.getEvent();
        String tag = startEvent.getTag();
        boolean resolved = false;
        Tag nodeTag;
        if (tag != null && !tag.equals("!")) {
            nodeTag = new Tag(tag);
        } else {
            nodeTag = this.resolver.resolve(NodeId.mapping, (String) null, startEvent.getImplicit());
            resolved = true;
        }

        ArrayList children = new ArrayList();
        MappingNode node = new MappingNode(nodeTag, resolved, children, startEvent.getStartMark(), (Mark) null, startEvent.getFlowStyle());
        if (anchor != null) {
            this.anchors.put(anchor, node);
        }

        while (!this.parser.checkEvent(Event.ID.MappingEnd)) {
            Node endEvent = this.composeNode(node);
            if (endEvent.getTag().equals(Tag.MERGE)) {
                node.setMerged(true);
            } else if (endEvent.getTag().equals(Tag.VALUE)) {
                endEvent.setTag(Tag.STR);
            }

            Node itemValue = this.composeNode(node);
            children.add(new NodeTuple(endEvent, itemValue));
        }

        Event endEvent1 = this.parser.getEvent();
        node.setEndMark(endEvent1.getEndMark());
        return node;
    }
}
