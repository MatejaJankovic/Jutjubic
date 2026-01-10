package rs.ftn.isa.jutjubicbackend.dto;

public class CreateCommentRequest {
    private String text;

    public CreateCommentRequest() {}

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
