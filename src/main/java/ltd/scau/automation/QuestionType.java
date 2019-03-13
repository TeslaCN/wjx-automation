package ltd.scau.automation;

/**
 * @author Wu Weijie
 */
public enum QuestionType {

    SINGLE_SELECT(3),
    MULTI_SELECT(4),
    SCORE_SELECT(5),
    RATE(6),

    ;
    private Integer type;

    QuestionType(Integer type) {
        this.type = type;
    }

    public static QuestionType byNum(Integer type) {
        return values()[type - 3];
    }
}
