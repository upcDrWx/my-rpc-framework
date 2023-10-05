package github.wx.remoting.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class Message implements Serializable {

    public Message(Object content) {
        this.content = content;
    }

    private Object content;
}
