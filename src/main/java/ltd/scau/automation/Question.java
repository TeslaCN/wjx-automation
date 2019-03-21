package ltd.scau.automation;

import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Objects;

/**
 * @author Wu Weijie
 */
public class Question {

    private Integer topic;

    private Boolean required;

    private QuestionType type;

    private Integer minValue;

    private Integer maxValue;

    private List<WebElement> clickableElements;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        return Objects.equals(topic, question.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic);
    }

    @Override
    public String toString() {
        return "Question{" +
                "topic=" + topic +
                ", required=" + required +
                ", type=" + type +
                ", minValue=" + minValue +
                ", maxValue=" + maxValue +
                ", clickableElements=" + clickableElements +
                '}';
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public Integer getMinValue() {
        return minValue;
    }

    public void setMinValue(Integer minValue) {
        this.minValue = minValue;
    }

    public List<WebElement> getClickableElements() {
        return clickableElements;
    }

    public void setClickableElements(List<WebElement> clickableElements) {
        this.clickableElements = clickableElements;
    }

    public Integer getTopic() {
        return topic;
    }

    public void setTopic(Integer topic) {
        this.topic = topic;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Integer getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
    }

    public static QuestionBuilder aQuestion() {
        return new QuestionBuilder();
    }

    public static final class QuestionBuilder {
        private Integer topic;
        private Boolean required;
        private QuestionType type;
        private Integer minValue;
        private Integer maxValue;
        private List<WebElement> clickableElements;

        private QuestionBuilder() {
        }

        public static QuestionBuilder aQuestion() {
            return new QuestionBuilder();
        }

        public QuestionBuilder withTopic(Integer topic) {
            this.topic = topic;
            return this;
        }

        public QuestionBuilder withRequired(Boolean required) {
            this.required = required;
            return this;
        }

        public QuestionBuilder withType(QuestionType type) {
            this.type = type;
            return this;
        }

        public QuestionBuilder withMinValue(Integer minValue) {
            this.minValue = minValue;
            return this;
        }

        public QuestionBuilder withMaxValue(Integer maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        public QuestionBuilder withClickableElements(List<WebElement> clickableElements) {
            this.clickableElements = clickableElements;
            return this;
        }

        public Question build() {
            Question question = new Question();
            question.setTopic(topic);
            question.setRequired(required);
            question.setType(type);
            question.setMinValue(minValue);
            question.setMaxValue(maxValue);
            question.setClickableElements(clickableElements);
            return question;
        }
    }
}
