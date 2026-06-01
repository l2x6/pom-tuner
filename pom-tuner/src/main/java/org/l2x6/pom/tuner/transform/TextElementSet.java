package org.l2x6.pom.tuner.transform;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.l2x6.pom.tuner.PomTransformer.ProfileElement;
import org.l2x6.pom.tuner.PomTransformer.TextElement;
import org.l2x6.pom.tuner.PomTransformer.TransformationContext;
import org.l2x6.pom.tuner.PomTransformer.Transformation;

/**
 * A set of {@code pom.xml} text elements selected for modification.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 * @since           5.0.0
 */
public class TextElementSet extends ElementSet<TextElement, TextElementSet> {
    TextElementSet(Function<ProfileElement, Stream<TextElement>> getNodes, Predicate<TextElement> nodeSelector) {
        super(getNodes, nodeSelector);
    }

    TextElementSet(Predicate<String> profileSelector, Function<ProfileElement, Stream<TextElement>> getNodes,
            Predicate<TextElement> nodeSelector) {
        super(profileSelector, getNodes, nodeSelector);
    }

    /**
     * Returns a {@link Transformation} that modifies the text content of each selected element using the given
     * {@code modifyTextContent} function.
     *
     * @param  modifyTextContent a {@link Function} receiving the current text content and returning the new text content
     * @return                   a new {@link Transformation}
     * @since                    5.0.0
     */
    public Transformation modifyTextContent(Function<String, String> modifyTextContent) {
        return forEach(textElement -> textElement.setTextContent(modifyTextContent.apply(textElement.getTextContent())));
    }

    /**
     * Returns a {@link Transformation} that replaces each selected element with a comment node.
     *
     * @param  getCommentText a {@link Function} receiving the selected {@link TextElement} and returning the comment text
     * @return                a new {@link Transformation}
     * @since                 5.0.0
     */
    public Transformation commentOut(Function<TextElement, String> getCommentText) {
        return forEach(
                textElement -> TransformationContext.commentTextNode(textElement.getNode(), getCommentText.apply(textElement)));
    }

    @Override
    protected TextElementSet create(Predicate<String> profileSelector, Function<ProfileElement, Stream<TextElement>> getNodes,
            Predicate<TextElement> nodeSelector) {
        return new TextElementSet(profileSelector, getNodes, nodeSelector);
    }

}
