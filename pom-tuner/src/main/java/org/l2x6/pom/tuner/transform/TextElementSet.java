package org.l2x6.pom.tuner.transform;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.l2x6.pom.tuner.PomTransformer.ProfileElement;
import org.l2x6.pom.tuner.PomTransformer.TextElement;
import org.l2x6.pom.tuner.PomTransformer.TransformationContext;
import org.l2x6.pom.tuner.PomTransformer.Transformation;

public class TextElementSet extends ElementSet<TextElement, TextElementSet> {
    public TextElementSet(Function<ProfileElement, Stream<TextElement>> getNodes, Predicate<TextElement> nodeSelector) {
        super(getNodes, nodeSelector);
    }

    public TextElementSet(Predicate<String> profileSelector, Function<ProfileElement, Stream<TextElement>> getNodes,
            Predicate<TextElement> nodeSelector) {
        super(profileSelector, getNodes, nodeSelector);
    }

    public Transformation modifyTextContent(Function<String, String> modifyTextContent) {
        return modify(textElement -> textElement.setTextContent(modifyTextContent.apply(textElement.getTextContent())));
    }

    public Transformation commentOut(Function<TextElement, String> getCommentText) {
        return modify(
                textElement -> TransformationContext.commentTextNode(textElement.getNode(), getCommentText.apply(textElement)));
    }

    @Override
    protected TextElementSet create(Predicate<String> profileSelector, Function<ProfileElement, Stream<TextElement>> getNodes,
            Predicate<TextElement> nodeSelector) {
        return new TextElementSet(profileSelector, getNodes, nodeSelector);
    }

}
